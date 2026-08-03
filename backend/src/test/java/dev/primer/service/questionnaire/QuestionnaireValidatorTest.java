package dev.primer.service.questionnaire;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.primer.config.QuestionnaireProperties;
import dev.primer.exception.QuestionnaireGenerationException;

class QuestionnaireValidatorTest {

    private final QuestionnaireProperties properties = new QuestionnaireProperties();
    private final QuestionnaireValidator validator = new QuestionnaireValidator(properties);

    private static QuestionnaireQuestion choice(String id) {
        return new QuestionnaireQuestion(id, "attention", QuestionType.SINGLE_CHOICE, QuestionImportance.CORE,
                "How familiar are you with attention?",
                List.of(new QuestionOption("a", "Not at all"), new QuestionOption("b", "Very")));
    }

    private static Questionnaire of(List<QuestionnaireQuestion> questions) {
        return new Questionnaire("Attention Is All You Need", "A few quick questions.", questions);
    }

    private static Questionnaire withFiveQuestions() {
        List<QuestionnaireQuestion> questions = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            questions.add(choice("q" + index));
        }
        return of(questions);
    }

    @Test
    void acceptsAWellFormedQuestionnaire() {
        assertThatCode(() -> validator.validate(withFiveQuestions())).doesNotThrowAnyException();
    }

    @Test
    void acceptsFreeTextQuestionsWithoutOptions() {
        List<QuestionnaireQuestion> questions = new ArrayList<>(withFiveQuestions().questions());
        questions.add(new QuestionnaireQuestion("q6", "goals", QuestionType.FREE_TEXT, QuestionImportance.HELPFUL,
                "What do you want out of this paper?", List.of()));

        assertThatCode(() -> validator.validate(of(questions))).doesNotThrowAnyException();
    }

    @Test
    void rejectsAQuestionnaireWithNoQuestions() {
        assertThatThrownBy(() -> validator.validate(of(List.of())))
                .isInstanceOf(QuestionnaireGenerationException.class)
                .hasMessageContaining("no questions");
    }

    @Test
    void rejectsFewerQuestionsThanConfigured() {
        assertThatThrownBy(() -> validator.validate(of(List.of(choice("q1")))))
                .isInstanceOf(QuestionnaireGenerationException.class)
                .hasMessageContaining("between");
    }

    @Test
    void rejectsMoreQuestionsThanConfigured() {
        List<QuestionnaireQuestion> questions = new ArrayList<>();
        for (int index = 1; index <= properties.getMaxQuestions() + 1; index++) {
            questions.add(choice("q" + index));
        }

        assertThatThrownBy(() -> validator.validate(of(questions)))
                .isInstanceOf(QuestionnaireGenerationException.class)
                .hasMessageContaining("between");
    }

    @Test
    void rejectsDuplicateQuestionIds() {
        List<QuestionnaireQuestion> questions = new ArrayList<>(withFiveQuestions().questions());
        questions.set(4, choice("q1"));

        assertThatThrownBy(() -> validator.validate(of(questions)))
                .isInstanceOf(QuestionnaireGenerationException.class)
                .hasMessageContaining("more than once");
    }

    @Test
    void rejectsAChoiceQuestionWithASingleOption() {
        List<QuestionnaireQuestion> questions = new ArrayList<>(withFiveQuestions().questions());
        questions.set(0, new QuestionnaireQuestion("q1", "attention", QuestionType.SINGLE_CHOICE,
                QuestionImportance.CORE, "How familiar are you?", List.of(new QuestionOption("a", "Not at all"))));

        assertThatThrownBy(() -> validator.validate(of(questions)))
                .isInstanceOf(QuestionnaireGenerationException.class)
                .hasMessageContaining("fewer than");
    }

    @Test
    void rejectsFreeTextQuestionsThatCarryOptions() {
        List<QuestionnaireQuestion> questions = new ArrayList<>(withFiveQuestions().questions());
        questions.set(0, new QuestionnaireQuestion("q1", "goals", QuestionType.FREE_TEXT, QuestionImportance.HELPFUL,
                "What do you want out of this?", List.of(new QuestionOption("a", "Not at all"))));

        assertThatThrownBy(() -> validator.validate(of(questions)))
                .isInstanceOf(QuestionnaireGenerationException.class)
                .hasMessageContaining("must not have options");
    }

    @Test
    void rejectsDuplicateOptionIdsWithinAQuestion() {
        List<QuestionnaireQuestion> questions = new ArrayList<>(withFiveQuestions().questions());
        questions.set(0, new QuestionnaireQuestion("q1", "attention", QuestionType.SINGLE_CHOICE,
                QuestionImportance.CORE, "How familiar are you?",
                List.of(new QuestionOption("a", "Not at all"), new QuestionOption("a", "Very"))));

        assertThatThrownBy(() -> validator.validate(of(questions)))
                .isInstanceOf(QuestionnaireGenerationException.class)
                .hasMessageContaining("reuses option ID");
    }

    @Test
    void rejectsAQuestionWithoutAPrompt() {
        List<QuestionnaireQuestion> questions = new ArrayList<>(withFiveQuestions().questions());
        questions.set(0, new QuestionnaireQuestion("q1", "attention", QuestionType.SINGLE_CHOICE,
                QuestionImportance.CORE, "  ",
                List.of(new QuestionOption("a", "Not at all"), new QuestionOption("b", "Very"))));

        assertThatThrownBy(() -> validator.validate(of(questions)))
                .isInstanceOf(QuestionnaireGenerationException.class)
                .hasMessageContaining("no prompt");
    }
}
