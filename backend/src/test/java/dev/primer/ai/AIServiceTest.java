package dev.primer.ai;

import dev.primer.ai.config.AIProperties;
import dev.primer.exception.AiProviderException;
import dev.primer.exception.InvalidAiRequestException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AIServiceTest {

    private final AIInterface provider = mock(AIInterface.class);

    private AIService serviceWithGeminiConfigured() {
        AIProperties properties = new AIProperties();
        properties.setProvider("gemini");
        return new AIService(Map.of("gemini", provider), properties);
    }

    @Test
    void dispatchesTextOnlyRequestToTextOnlyInput() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest("hello", null, null);
        AIResponse expected = new AIResponse("hi there");
        when(provider.textOnlyInput(request)).thenReturn(expected);

        AIResponse actual = service.process(request);

        assertThat(actual).isEqualTo(expected);
        verify(provider).textOnlyInput(request);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void dispatchesImageOnlyRequestToImageOnlyInput() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest(null, new byte[]{1, 2, 3}, "image/png");
        AIResponse expected = new AIResponse("a picture of a cat");
        when(provider.imageOnlyInput(request)).thenReturn(expected);

        AIResponse actual = service.process(request);

        assertThat(actual).isEqualTo(expected);
        verify(provider).imageOnlyInput(request);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void dispatchesTextAndImageRequestToTextAndImageInput() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest("what is this?", new byte[]{1, 2, 3}, "image/png");
        AIResponse expected = new AIResponse("it's a diagram");
        when(provider.textAndImageInput(request)).thenReturn(expected);

        AIResponse actual = service.process(request);

        assertThat(actual).isEqualTo(expected);
        verify(provider).textAndImageInput(request);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void rejectsRequestWithNeitherTextNorImage() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest(null, null, null);

        assertThatThrownBy(() -> service.process(request))
                .isInstanceOf(InvalidAiRequestException.class);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void rejectsRequestWithBlankTextAndNoImage() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest("   ", null, null);

        assertThatThrownBy(() -> service.process(request))
                .isInstanceOf(InvalidAiRequestException.class);
    }

    @Test
    void wrapsUnexpectedProviderFailuresAsAiProviderException() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest("hello", null, null);
        when(provider.textOnlyInput(request)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.process(request))
                .isInstanceOf(AiProviderException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void propagatesAiProviderExceptionWithoutRewrapping() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest("hello", null, null);
        AiProviderException original = new AiProviderException("upstream failure");
        when(provider.textOnlyInput(request)).thenThrow(original);

        assertThatThrownBy(() -> service.process(request)).isSameAs(original);
    }

    @Test
    void failsFastAtConstructionWhenConfiguredProviderIsNotRegistered() {
        AIProperties properties = new AIProperties();
        properties.setProvider("openai");

        assertThatThrownBy(() -> new AIService(Map.of("gemini", provider), properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("openai");
    }

    @Test
    void rejectsImageLargerThanConfiguredLimit() {
        AIProperties properties = new AIProperties();
        properties.setProvider("gemini");
        properties.setMaxImageSize(org.springframework.util.unit.DataSize.ofBytes(2));
        AIService service = new AIService(Map.of("gemini", provider), properties);
        AIRequest request = new AIRequest(null, new byte[]{1, 2, 3, 4}, "image/png");

        assertThatThrownBy(() -> service.process(request))
                .isInstanceOf(InvalidAiRequestException.class);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void rejectsImageWithDisallowedContentType() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest(null, new byte[]{1, 2, 3}, "image/gif");

        assertThatThrownBy(() -> service.process(request))
                .isInstanceOf(InvalidAiRequestException.class);
        verifyNoMoreInteractions(provider);
    }

    @Test
    void rejectsImageWithMissingContentType() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest(null, new byte[]{1, 2, 3}, null);

        assertThatThrownBy(() -> service.process(request))
                .isInstanceOf(InvalidAiRequestException.class);
    }

    @Test
    void acceptsImageWithinLimitsAndAllowedType() {
        AIService service = serviceWithGeminiConfigured();
        AIRequest request = new AIRequest(null, new byte[]{1, 2, 3}, "image/png");
        when(provider.imageOnlyInput(request)).thenReturn(new AIResponse("ok"));

        assertThat(service.process(request).text()).isEqualTo("ok");
    }
}
