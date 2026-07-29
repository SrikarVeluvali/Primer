package dev.primer.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One {@link ChatClient} per AI provider, each built from that provider's own {@code ChatModel}.
 * <p>
 * With a single Spring AI starter on the classpath, providers could simply inject the
 * autoconfigured {@code ChatClient.Builder}. Adding the OpenAI starter (used here to reach Groq)
 * puts a second {@code ChatModel} in the context, at which point that builder can no longer resolve
 * a model unambiguously. Naming a client per provider keeps the choice explicit and lets both
 * providers coexist, so switching between them stays a pure {@code primer.ai.provider} change.
 * <p>
 * Both clients are always created; only the one belonging to the active provider is ever called.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient geminiChatClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean
    public ChatClient groqChatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
