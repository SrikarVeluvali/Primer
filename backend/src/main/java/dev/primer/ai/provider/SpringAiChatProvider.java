package dev.primer.ai.provider;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import dev.primer.ai.AIInterface;
import dev.primer.ai.AIRequest;
import dev.primer.ai.AIResponse;
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
