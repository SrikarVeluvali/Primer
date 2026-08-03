package dev.primer.service.questionnaire;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import dev.primer.ai.AIService;
import dev.primer.ai.config.AIProperties;
import dev.primer.config.PdfExtractionProperties;
import dev.primer.config.QuestionnaireProperties;
import dev.primer.service.ExtractedPage;
import dev.primer.service.PdfExtractionService;

/**
 * Manual harness for running the two analysis agents against whichever provider
 * {@code primer.ai.provider} selects, and printing what they produce. This is the only way to judge
 * whether the questionnaire is actually any good — the automated tests mock the model and can only
 * prove the plumbing works.
 * <p>
 * Skipped unless {@code -Dprimer.ai.manualTest=true} is passed, for the same reason as
 * {@link dev.primer.ai.AIManualTest}: a -D flag has to be given on each invocation, so it cannot leak
 * into a normal {@code mvn test} run the way an environment variable would.
 * <p>
 * The context is scoped to {@code dev.primer.ai} plus the two agents, so neither Postgres nor the
 * paper repositories are involved — an API key is the only thing needed.
 *
 * <pre>
 * Against a short built-in sample:
 *   mvn test -Dtest=QuestionnaireManualTest#generatesAQuestionnaireFromSampleText -Dprimer.ai.manualTest=true
 *
 * Against a real PDF you have on disk:
 *   mvn test -Dtest=QuestionnaireManualTest#generatesAQuestionnaireFromAPdf -Dprimer.ai.manualTest=true -Dprimer.paper.pdf=./data/papers/&lt;id&gt;.pdf
 * </pre>
 */
@SpringBootTest(classes = QuestionnaireManualTest.TestConfig.class)
@EnabledIfSystemProperty(named = "primer.ai.manualTest", matches = "true")
class QuestionnaireManualTest {

    private static final String SAMPLE_PAPER = """
            Attention Is All You Need

            Abstract. The dominant sequence transduction models are based on complex recurrent or
            convolutional neural networks that include an encoder and a decoder. We propose a new simple
            network architecture, the Transformer, based solely on attention mechanisms, dispensing with
            recurrence and convolutions entirely. Experiments on two machine translation tasks show these
            models to be superior in quality while being more parallelizable and requiring significantly
            less time to train.

            1. Introduction. Recurrent neural networks, long short-term memory and gated recurrent neural
            networks in particular, have been firmly established as state of the art approaches in sequence
            modeling. This inherently sequential nature precludes parallelization within training examples.

            3. Model Architecture. The Transformer follows an encoder-decoder architecture using stacked
            self-attention and point-wise, fully connected layers. We call our particular attention
            "Scaled Dot-Product Attention". The input consists of queries and keys of dimension d_k, and
            values of dimension d_v. We compute the dot products of the query with all keys, divide each
            by the square root of d_k, and apply a softmax function to obtain the weights on the values.
            Multi-head attention allows the model to jointly attend to information from different
            representation subspaces. Since our model contains no recurrence and no convolution, we add
            positional encodings using sine and cosine functions of different frequencies.
            """;

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties({AIProperties.class, QuestionnaireProperties.class, PdfExtractionProperties.class})
    @ComponentScan(basePackageClasses = AIService.class)
    static class TestConfig {
    }

    @Autowired
    private AIService aiService;

    @Autowired
    private QuestionnaireProperties questionnaireProperties;

    @Autowired
    private PdfExtractionProperties pdfExtractionProperties;

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void generatesAQuestionnaireFromSampleText() throws Exception {
        run(SAMPLE_PAPER);
    }

    @Test
    void generatesAQuestionnaireFromAPdf() throws Exception {
        String pdfPathProperty = System.getProperty("primer.paper.pdf", "");
        org.junit.jupiter.api.Assumptions.assumeTrue(!pdfPathProperty.isBlank(),
                "set -Dprimer.paper.pdf=/path/to/paper.pdf to run this test");

        String text = new PdfExtractionService(pdfExtractionProperties)
                .extract(Path.of(pdfPathProperty)).pages().stream()
                .map(ExtractedPage::text)
                .collect(Collectors.joining("\n\n"));
        int limit = questionnaireProperties.getMaxInputCharacters();
        run(text.length() <= limit ? text : text.substring(0, limit));
    }

    private void run(String paperText) throws Exception {
        PaperContent content = new PaperContentAgent(aiService).analyze(paperText);
        Questionnaire questionnaire = new QuestioningAgent(aiService, questionnaireProperties).generate(paperText);

        System.out.println();
        System.out.println("=== PAPER CONTENT ===");
        System.out.println(objectMapper.writeValueAsString(content));
        System.out.println();
        System.out.println("=== QUESTIONNAIRE ===");
        System.out.println(objectMapper.writeValueAsString(questionnaire));

        // The same check the pipeline applies before it will store a questionnaire.
        new QuestionnaireValidator(questionnaireProperties).validate(questionnaire);
        assertThat(content.prerequisiteConcepts()).isNotEmpty();
    }
}
