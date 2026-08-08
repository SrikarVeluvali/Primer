package dev.primer.service.questionnaire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.primer.ai.AIRequest;
import dev.primer.ai.AIService;
import dev.primer.exception.InvalidQuestionnaireAnswerException;

class AnswerAmbiguityDetectorTest {

    private final AIService aiService = mock(AIService.class);
    private final AnswerAmbiguityDetector detector = new AnswerAmbiguityDetector(aiService);

    private static QuestionnaireQuestion singleChoice(String id) {
        return new QuestionnaireQuestion(id, "attention", QuestionType.SINGLE_CHOICE, QuestionImportance.CORE,
                "How familiar are you with attention?",
                List.of(new QuestionOption("a", "No exposure"), new QuestionOption("b", "Very familiar")));
    }

    private static QuestionnaireQuestion freeText(String id) {
        return new QuestionnaireQuestion(id, "goals", QuestionType.FREE_TEXT, QuestionImportance.HELPFUL,
                "What do you want out of this paper?", List.of());
    }

    private static Questionnaire questionnaireOf(QuestionnaireQuestion... questions) {
        return new Questionnaire("Attention Is All You Need", "A few quick questions.", List.of(questions));
    }

    @Test
    void rejectsAnAnswerToAnUnknownQuestion() {
        Questionnaire questionnaire = questionnaireOf(singleChoice("q1"));
        QuestionnaireAnswers answers = new QuestionnaireAnswers(
                List.of(new QuestionnaireAnswer("q99", List.of("a"), null)));

        assertThatThrownBy(() -> detector.detect(questionnaire, answers))
                .isInstanceOf(InvalidQuestionnaireAnswerException.class)
                .hasMessageContaining("unknown question");
        verify(aiService, never()).processStructured(any(AIRequest.class), any(Class.class));
    }

    @Test
    void rejectsAnAnswerSelectingAnUnknownOption() {
        Questionnaire questionnaire = questionnaireOf(singleChoice("q1"));
        QuestionnaireAnswers answers = new QuestionnaireAnswers(
                List.of(new QuestionnaireAnswer("q1", List.of("z"), null)));

        assertThatThrownBy(() -> detector.detect(questionnaire, answers))
                .isInstanceOf(InvalidQuestionnaireAnswerException.class)
                .hasMessageContaining("unknown option");
        verify(aiService, never()).processStructured(any(AIRequest.class), any(Class.class));
    }

    @Test
    void rejectsASingleChoiceAnswerWithMultipleSelections() {
        Questionnaire questionnaire = questionnaireOf(singleChoice("q1"));
        QuestionnaireAnswers answers = new QuestionnaireAnswers(
                List.of(new QuestionnaireAnswer("q1", List.of("a", "b"), null)));

        assertThatThrownBy(() -> detector.detect(questionnaire, answers))
                .isInstanceOf(InvalidQuestionnaireAnswerException.class)
                .hasMessageContaining("single-choice");
        verify(aiService, never()).processStructured(any(AIRequest.class), any(Class.class));
    }

    @Test
    void acceptsAWellFormedFreeTextAnswerWithoutOptionChecks() {
        Questionnaire questionnaire = questionnaireOf(freeText("q1"));
        QuestionnaireAnswers answers = new QuestionnaireAnswers(
                List.of(new QuestionnaireAnswer("q1", List.of(), "I want to understand the method.")));
        when(aiService.processStructured(any(AIRequest.class), eq(AmbiguityReport.class)))
                .thenReturn(new AmbiguityReport(List.of()));

        assertThat(detector.detect(questionnaire, answers).hasAmbiguity()).isFalse();
    }

    @Test
    void returnsWhateverTheModelFlagsAsAmbiguous() {
        Questionnaire questionnaire = questionnaireOf(singleChoice("q1"));
        QuestionnaireAnswers answers = new QuestionnaireAnswers(
                List.of(new QuestionnaireAnswer("q1", List.of("a"), null)));
        AmbiguityReport modelReport = new AmbiguityReport(
                List.of(new AmbiguousAnswer("q1", "Contradicts a later answer about the same concept.")));
        when(aiService.processStructured(any(AIRequest.class), eq(AmbiguityReport.class)))
                .thenReturn(modelReport);

        AmbiguityReport result = detector.detect(questionnaire, answers);

        assertThat(result.hasAmbiguity()).isTrue();
        assertThat(result.ambiguousAnswers()).containsExactly(
                new AmbiguousAnswer("q1", "Contradicts a later answer about the same concept."));
    }

    @Test
    void toleratesAQuestionWithNoAnswerAtAll() {
        Questionnaire questionnaire = questionnaireOf(singleChoice("q1"), freeText("q2"));
        QuestionnaireAnswers answers = new QuestionnaireAnswers(
                List.of(new QuestionnaireAnswer("q1", List.of("a"), null)));
        when(aiService.processStructured(any(AIRequest.class), eq(AmbiguityReport.class)))
                .thenReturn(new AmbiguityReport(List.of()));

        assertThat(detector.detect(questionnaire, answers)).isNotNull();
    }
}
