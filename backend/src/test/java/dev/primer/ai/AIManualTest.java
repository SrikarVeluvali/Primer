package dev.primer.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import dev.primer.ai.config.AIProperties;

/**
 * Manual harness for calling the real Gemini API end-to-end through {@link AIService}, the same
 * entry point upstream callers use. Skipped unless explicitly requested via
 * {@code -Dprimer.ai.manualTest=true} — deliberately NOT gated on the GEMINI_API_KEY environment
 * variable, since an env var can persist across an entire shell/CI session and would silently
 * make this hit the real API (and fail the build on quota/network issues) on every normal
 * {@code mvn test} run. A -D flag has to be passed explicitly on each invocation, so it can't leak.
 * <p>
 * Uses a narrow test-only configuration scoped to the {@code dev.primer.ai} package instead of
 * booting the full {@code PrimerApplication} — that keeps PaperController/PaperService/
 * PaperRepository (and the real Postgres connection they need) out of the context entirely, so
 * this test needs nothing but a Gemini API key.
 *
 * Text only:
 *   mvn test -Dtest=AIManualTest#callsRealGeminiWithTextOnly -Dprimer.ai.manualTest=true
 *
 * Text + image:
 *   mvn test -Dtest=AIManualTest#callsRealGeminiWithImage -Dprimer.ai.manualTest=true -Dprimer.ai.image=/path/to/figure.png
 *
 * GEMINI_API_KEY must still be set in the environment either way — it's only the trigger to
 * *run* the test that changed, not how the key itself is supplied.
 */
@SpringBootTest(classes = AIManualTest.TestConfig.class)
@EnabledIfSystemProperty(named = "primer.ai.manualTest", matches = "true")
class AIManualTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(AIProperties.class)
    @ComponentScan(basePackageClasses = AIService.class)
    static class TestConfig {
    }

    @Autowired
    private AIService aiService;

    @Test
    void callsRealGeminiWithTextOnly() {
        AIResponse response = aiService.process(
                new AIRequest("In one sentence, what is attention in transformer models?", null, null));

        System.out.println();
        System.out.println("Gemini response: " + response.text());
        assertThat(response.text()).isNotBlank();
    }

    @Test
    void callsRealGeminiWithImage() throws Exception {
        String imagePathProperty = System.getProperty("primer.ai.image", "");
        assumeTrue(!imagePathProperty.isBlank(), "set -Dprimer.ai.image=/path/to/image.png to run this test");

        Path imagePath = Path.of(imagePathProperty);
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String contentType = imagePathProperty.endsWith(".jpg") || imagePathProperty.endsWith(".jpeg")
                ? "image/jpeg" : "image/png";

        AIResponse response = aiService.process(
                new AIRequest("What does this image show?", imageBytes, contentType));

        System.out.println();
        System.out.println("Gemini response: " + response.text());
        assertThat(response.text()).isNotBlank();
    }
}