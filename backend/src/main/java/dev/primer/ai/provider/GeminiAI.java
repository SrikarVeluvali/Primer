package dev.primer.ai.provider;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dev.primer.ai.AIInterface;

/**
 * Gemini implementation of {@link AIInterface}, backed by Spring AI's {@link ChatClient}.
 * <p>
 * Registered under the bean name {@code "gemini"} so {@code dev.primer.ai.AIService} can select it
 * purely via the {@code primer.ai.provider=gemini} property — no other wiring is required.
 * All request handling lives in {@link SpringAiChatProvider}; this class only binds that behaviour
 * to the Gemini-backed {@link ChatClient}.
 */
@Service("gemini")
public class GeminiAI extends SpringAiChatProvider {

    public GeminiAI(@Qualifier("geminiChatClient") ChatClient chatClient) {
        super(chatClient, "Gemini");
    }
}
