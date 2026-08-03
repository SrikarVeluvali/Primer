package dev.primer.ai.provider;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import dev.primer.ai.AIInterface;
import dev.primer.ai.AIRequest;
import dev.primer.ai.AIResponse;
import dev.primer.ai.StructuredOutputSanitizer;
import dev.primer.exception.AiProviderException;

/**
 * Shared implementation of {@link AIInterface} for any provider reachable through Spring AI's
 * {@link ChatClient}. Subclasses supply a provider-specific {@link ChatClient} (built from that
 * provider's own {@code ChatModel}) and a display name used in error messages, so a failure can
 * still be attributed to the provider that produced it.
 * <p>
 * Note that each subclass must be given an explicitly qualified {@link ChatClient}: once more than
 * one Spring AI model starter is on the classpath there are multiple {@code ChatModel} beans, and
 * the autoconfigured {@code ChatClient.Builder} can no longer resolve one unambiguously. See
 * {@link dev.primer.ai.config.ChatClientConfig}.
 */
abstract class SpringAiChatProvider implements AIInterface {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatProvider.class);

    private static final String DEFAULT_IMAGE_PROMPT =
            "Describe this image in detail, noting anything relevant to a research paper reader.";

    private static final MimeType DEFAULT_IMAGE_MIME_TYPE = MimeTypeUtils.IMAGE_PNG;

    private static final int SNIPPET_LENGTH = 400;

    private final ChatClient chatClient;
    private final String providerName;

    protected SpringAiChatProvider(ChatClient chatClient, String providerName) {
        this.chatClient = chatClient;
        this.providerName = providerName;
    }

    @Override
    public AIResponse textOnlyInput(AIRequest request) {
        return execute(() -> chatClient.prompt()
                .user(request.text())
                .call()
                .content());
    }

    @Override
    public AIResponse imageOnlyInput(AIRequest request) {
        Media media = toMedia(request);
        return execute(() -> chatClient.prompt()
                .user(userSpec -> userSpec.text(DEFAULT_IMAGE_PROMPT).media(media))
                .call()
                .content());
    }

    @Override
    public AIResponse textAndImageInput(AIRequest request) {
        Media media = toMedia(request);
        return execute(() -> chatClient.prompt()
                .user(userSpec -> userSpec.text(request.text()).media(media))
                .call()
                .content());
    }

    /**
     * Asks for the reply in the shape of {@code type} and converts it.
     * <p>
     * This drives {@link BeanOutputConverter} by hand rather than calling {@code .entity(type)},
     * which does the same two steps but parses the reply exactly as it arrives. That fails against
     * any model whose reply carries something alongside the JSON — a reasoning model's
     * {@code <think>} block, a Markdown fence — so the reply passes through
     * {@link StructuredOutputSanitizer} in between. The schema instructions the converter generates
     * are unchanged either way.
     */
    @Override
    public <T> T structuredOutput(AIRequest request, Class<T> type) {
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(type);
        String instructions = promptText(request) + "\n\n" + converter.getFormat();

        String content;
        try {
            content = request.hasImage()
                    ? chatClient.prompt()
                            .user(userSpec -> userSpec.text(instructions).media(toMedia(request)))
                            .call()
                            .content()
                    : chatClient.prompt()
                            .user(instructions)
                            .call()
                            .content();
        } catch (Exception exception) {
            throw new AiProviderException("Failed to call %s model".formatted(providerName), exception);
        }

        String json = StructuredOutputSanitizer.sanitize(content);
        if (json == null || json.isBlank()) {
            throw new AiProviderException(
                    "%s returned no JSON. Raw reply was %s".formatted(providerName, snippet(content)));
        }

        T entity;
        try {
            entity = converter.convert(json);
        } catch (Exception exception) {
            throw new AiProviderException(
                    "%s returned a response that could not be read as %s. Raw reply was %s"
                            .formatted(providerName, type.getSimpleName(), snippet(content)), exception);
        }
        if (entity == null) {
            throw new AiProviderException(
                    "%s returned a response that could not be read as %s. Raw reply was %s"
                            .formatted(providerName, type.getSimpleName(), snippet(content)));
        }
        return entity;
    }

    /**
     * A short, single-line excerpt of a reply for use in an error message. Without it a structured
     * output failure says only that parsing failed, which is the one thing the caller already knows —
     * what they need is what the model actually said.
     */
    private static String snippet(String content) {
        if (content == null) {
            return "<null>";
        }
        if (content.isBlank()) {
            return "<blank, %d characters>".formatted(content.length());
        }
        String collapsed = content.strip().replaceAll("\\s+", " ");
        return collapsed.length() <= SNIPPET_LENGTH
                ? "'%s'".formatted(collapsed)
                : "'%s' (truncated from %d characters)"
                        .formatted(collapsed.substring(0, SNIPPET_LENGTH), collapsed.length());
    }

    private String promptText(AIRequest request) {
        return request.hasText() ? request.text() : DEFAULT_IMAGE_PROMPT;
    }

    private AIResponse execute(Supplier<String> call) {
        String content;
        try {
            content = call.get();
        } catch (Exception exception) {
            throw new AiProviderException("Failed to call %s model".formatted(providerName), exception);
        }
        if (content == null || content.isBlank()) {
            throw new AiProviderException("%s returned an empty response".formatted(providerName));
        }
        return new AIResponse(content);
    }

    private Media toMedia(AIRequest request) {
        MimeType mimeType = DEFAULT_IMAGE_MIME_TYPE;
        if (request.imageContentType() != null && !request.imageContentType().isBlank()) {
            try {
                mimeType = MimeTypeUtils.parseMimeType(request.imageContentType());
            } catch (Exception exception) {
                log.warn("Unrecognized image content type '{}', defaulting to {}",
                        request.imageContentType(), DEFAULT_IMAGE_MIME_TYPE, exception);
            }
        }
        return new Media(mimeType, new ByteArrayResource(request.image()));
    }
}
