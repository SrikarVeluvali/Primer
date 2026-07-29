package dev.primer.ai;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.primer.ai.config.AIProperties;
import dev.primer.exception.AiProviderException;
import dev.primer.exception.InvalidAiRequestException;

/**
 * Orchestration layer for AI requests.
 * The active provider is resolved once, at startup, from the map of every {@link AIInterface}
 * bean in the context (Spring auto-populates {@code Map<String, AIInterface>} keyed by bean
 * name) using {@code primer.ai.provider}. This means:
 *     - Adding a new provider only requires a new {@code @Service("name")} class — no changes here.
 *     - Switching the active provider only requires changing the {@code primer.ai.provider} property value.
 *     - A misconfigured provider name fails application startup instead of failing on the first request.
 */
@Service
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    private final AIInterface provider;
    private final AIProperties properties;
    private final String providerName;

    public AIService(Map<String, AIInterface> providers, AIProperties properties) {
        this.properties = properties;
        this.providerName = properties.getProvider();
        this.provider = providers.get(providerName);
        if (this.provider == null) {
            throw new IllegalStateException(
                    "No AI provider registered for primer.ai.provider='%s'. Available providers: %s"
                            .formatted(providerName, providers.keySet()));
        }
        log.info("AIService configured with provider '{}'", providerName);
    }

    public AIResponse process(AIRequest request) {
        if (!request.hasText() && !request.hasImage()) {
            throw new InvalidAiRequestException("Request must contain text and/or an image");
        }
        validateImage(request);

        try {
            if (request.hasText() && request.hasImage()) {
                return provider.textAndImageInput(request);
            }
            if (request.hasText()) {
                return provider.textOnlyInput(request);
            }
            return provider.imageOnlyInput(request);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException(
                    "AI provider '%s' failed to process the request".formatted(providerName), exception);
        }
    }

    private void validateImage(AIRequest request) {
        if (!request.hasImage()) {
            return;
        }
        if (request.image().length > properties.getMaxImageSize().toBytes()) {
            throw new InvalidAiRequestException(
                    "Image exceeds the maximum allowed size of " + properties.getMaxImageSize());
        }
        String contentType = request.imageContentType();
        if (contentType == null || !properties.getAllowedImageTypes().contains(contentType)) {
            throw new InvalidAiRequestException(
                    "Unsupported image type '%s'. Allowed types: %s"
                            .formatted(contentType, properties.getAllowedImageTypes()));
        }
    }
}
