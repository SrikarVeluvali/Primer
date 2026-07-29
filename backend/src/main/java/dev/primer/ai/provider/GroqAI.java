package dev.primer.ai.provider;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dev.primer.ai.AIInterface;

/**
 * Groq implementation of {@link AIInterface}.
 * <p>
 * Groq serves an OpenAI-compatible API, so there is no Groq-specific Spring AI starter: the OpenAI
 * starter is pointed at {@code https://api.groq.com/openai} via {@code spring.ai.openai.base-url}
 * and everything else works through the standard {@code OpenAiChatModel}.
 * <p>
 * Image input requires a vision-capable Groq model — set {@code spring.ai.openai.chat.options.model}
 * to one (e.g. {@code qwen/qwen3.6-27b}). Text-only models on Groq reject multimodal messages
 * outright, which surfaces here as an {@link dev.primer.exception.AiProviderException}.
 * <p>
 * Registered under the bean name {@code "groq"}; activate it with {@code primer.ai.provider=groq}.
 */
@Service("groq")
public class GroqAI extends SpringAiChatProvider {

    public GroqAI(@Qualifier("groqChatClient") ChatClient chatClient) {
        super(chatClient, "Groq");
    }
}
