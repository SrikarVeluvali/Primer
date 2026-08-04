package dev.primer.ai;

/**
 * Contract implemented by every AI provider (Gemini, and future providers such as OpenAI or
 * Anthropic). Implementations are registered as Spring beans named after the provider key used in
 * {@code primer.ai.provider} (e.g. {@code @Service("gemini")}), which is how {@link AIService}
 * resolves the active provider without any conditional logic.
 * Each method must translate provider-specific failures into {@link dev.primer.exception.AiProviderException}.
 */
public interface AIInterface {

    AIResponse textOnlyInput(AIRequest request);

    AIResponse imageOnlyInput(AIRequest request);

    AIResponse textAndImageInput(AIRequest request);

    /**
     * Sends the request and binds the reply to {@code type} instead of returning free-form text.
     * Implementations are expected to describe {@code type}'s shape to the model so the reply can be
     * deserialized without post-processing, which is what makes generated artifacts such as the
     * questionnaire safe to store and serve verbatim.
     */
    <T> T structuredOutput(AIRequest request, Class<T> type);
}
