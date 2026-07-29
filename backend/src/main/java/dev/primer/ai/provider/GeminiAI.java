package dev.primer.ai.provider;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import dev.primer.ai.AIInterface;
import dev.primer.ai.AIRequest;
import dev.primer.ai.AIResponse;
import dev.primer.exception.AiProviderException;

/**
 * Gemini implementation of {@link AIInterface}, backed by Spring AI's {@link ChatClient}.
 * <p>
 * Registered under the bean name {@code "gemini"} so {@code dev.primer.ai.AIService} can select it
 * purely via the {@code primer.ai.provider=gemini} property — no other wiring is required.
 */
@Service("gemini")
public class GeminiAI implements AIInterface {

    private static final Logger log = LoggerFactory.getLogger(GeminiAI.class);

    private static final String DEFAULT_IMAGE_PROMPT =
            "Describe this image in detail, noting anything relevant to a research paper reader.";

    private static final MimeType DEFAULT_IMAGE_MIME_TYPE = MimeTypeUtils.IMAGE_PNG;

    private final ChatClient chatClient;

    public GeminiAI(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
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
            throw new AiProviderException("Failed to call Gemini model", exception);
        }
        if (content == null || content.isBlank()) {
            throw new AiProviderException("Gemini returned an empty response");
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
