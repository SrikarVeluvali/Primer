package dev.primer.ai.provider;

import dev.primer.ai.AIRequest;
import dev.primer.ai.AIResponse;
import dev.primer.exception.AiProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The request-shaping behaviour shared by every Spring AI provider is covered by
 * {@link GeminiAITest}; this class covers what is specific to Groq — that it drives the same
 * text/image paths through its own {@link ChatClient}, and that failures are attributed to Groq
 * rather than to whichever provider happens to be configured.
 */
@ExtendWith(MockitoExtension.class)
class GroqAITest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private GroqAI groqAI;

    @BeforeEach
    void setUp() {
        groqAI = new GroqAI(chatClient);
    }

    @Test
    void textOnlyInputReturnsModelResponse() {
        AIRequest request = new AIRequest("What is attention?", null, null);
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("It's a mechanism...");

        AIResponse response = groqAI.textOnlyInput(request);

        assertThat(response.text()).isEqualTo("It's a mechanism...");
    }

    @Test
    void textAndImageInputReturnsModelResponse() {
        AIRequest request = new AIRequest("What does this show?", new byte[]{1, 2, 3}, "image/png");
        when(chatClient.prompt().user(this.<ChatClient.PromptUserSpec>anyConsumer()).call().content())
                .thenReturn("It shows the training loss curve");

        AIResponse response = groqAI.textAndImageInput(request);

        assertThat(response.text()).isEqualTo("It shows the training loss curve");
    }

    @Test
    void imageOnlyInputReturnsModelResponse() {
        AIRequest request = new AIRequest(null, new byte[]{1, 2, 3}, "image/jpeg");
        when(chatClient.prompt().user(this.<ChatClient.PromptUserSpec>anyConsumer()).call().content())
                .thenReturn("A diagram of a neural network");

        AIResponse response = groqAI.imageOnlyInput(request);

        assertThat(response.text()).isEqualTo("A diagram of a neural network");
    }

    @Test
    void attributesFailureToGroqRatherThanAnotherProvider() {
        AIRequest request = new AIRequest("hello", null, null);
        when(chatClient.prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("model_not_found"));

        assertThatThrownBy(() -> groqAI.textOnlyInput(request))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("Groq")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void throwsAiProviderExceptionWhenModelReturnsBlankContent() {
        AIRequest request = new AIRequest("hello", null, null);
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("   ");

        assertThatThrownBy(() -> groqAI.textOnlyInput(request))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("empty response");
    }

    @SuppressWarnings("unchecked")
    private <T> Consumer<T> anyConsumer() {
        return any(Consumer.class);
    }
}
