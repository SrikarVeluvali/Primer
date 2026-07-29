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

@ExtendWith(MockitoExtension.class)
class GeminiAITest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private GeminiAI geminiAI;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        geminiAI = new GeminiAI(chatClientBuilder);
    }

    @Test
    void textOnlyInputReturnsModelResponse() {
        AIRequest request = new AIRequest("What is attention?", null, null);
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("It's a mechanism...");

        AIResponse response = geminiAI.textOnlyInput(request);

        assertThat(response.text()).isEqualTo("It's a mechanism...");
    }

    @Test
    void imageOnlyInputReturnsModelResponse() {
        AIRequest request = new AIRequest(null, new byte[]{1, 2, 3}, "image/png");
        when(chatClient.prompt().user(this.<ChatClient.PromptUserSpec>anyConsumer()).call().content())
                .thenReturn("A diagram of a neural network");

        AIResponse response = geminiAI.imageOnlyInput(request);

        assertThat(response.text()).isEqualTo("A diagram of a neural network");
    }

    @Test
    void textAndImageInputReturnsModelResponse() {
        AIRequest request = new AIRequest("What does this show?", new byte[]{1, 2, 3}, "image/jpeg");
        when(chatClient.prompt().user(this.<ChatClient.PromptUserSpec>anyConsumer()).call().content())
                .thenReturn("It shows the training loss curve");

        AIResponse response = geminiAI.textAndImageInput(request);

        assertThat(response.text()).isEqualTo("It shows the training loss curve");
    }

    @Test
    void imageOnlyInputDefaultsUnrecognizedContentTypeInsteadOfFailing() {
        AIRequest request = new AIRequest(null, new byte[]{1, 2, 3}, "not-a-real-mime-type");
        when(chatClient.prompt().user(this.<ChatClient.PromptUserSpec>anyConsumer()).call().content())
                .thenReturn("described anyway");

        AIResponse response = geminiAI.imageOnlyInput(request);

        assertThat(response.text()).isEqualTo("described anyway");
    }

    @Test
    void throwsAiProviderExceptionWhenModelReturnsBlankContent() {
        AIRequest request = new AIRequest("hello", null, null);
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("   ");

        assertThatThrownBy(() -> geminiAI.textOnlyInput(request))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void wrapsUnderlyingChatClientFailureAsAiProviderException() {
        AIRequest request = new AIRequest("hello", null, null);
        when(chatClient.prompt().user(anyString()).call().content())
                .thenThrow(new RuntimeException("connection reset"));

        assertThatThrownBy(() -> geminiAI.textOnlyInput(request))
                .isInstanceOf(AiProviderException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @SuppressWarnings("unchecked")
    private <T> Consumer<T> anyConsumer() {
        return any(Consumer.class);
    }
}
