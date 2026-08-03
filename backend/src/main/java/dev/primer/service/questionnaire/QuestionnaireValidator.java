package dev.primer.service.questionnaire;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import dev.primer.config.QuestionnaireProperties;
import dev.primer.exception.QuestionnaireGenerationException;

/**
 * Enforces the rules the response schema cannot express. A schema can guarantee a questionnaire has
 * a list of questions; only this can guarantee the list is usable — IDs unique enough to hang answers
 * off, choice questions that actually offer a choice.
 * <p>
 * A questionnaire that fails here is discarded exactly like a provider failure, because storing a
 * malformed one would break the answer-collection step later.
 */
@Component
public class QuestionnaireValidator {

    private static final int MIN_OPTIONS = 2;

    private final QuestionnaireProperties properties;

    public QuestionnaireValidator(QuestionnaireProperties properties) {
        this.properties = properties;
    }

    public void validate(Questionnaire questionnaire) {
        List<String> problems = new ArrayList<>();
        if (questionnaire == null || questionnaire.questions() == null || questionnaire.questions().isEmpty()) {
            throw new QuestionnaireGenerationException("The generated questionnaire contains no questions.");
        }

        List<QuestionnaireQuestion> questions = questionnaire.questions();
        if (questions.size() < properties.getMinQuestions() || questions.size() > properties.getMaxQuestions()) {
            problems.add("expected between %d and %d questions but got %d"
                    .formatted(properties.getMinQuestions(), properties.getMaxQuestions(), questions.size()));
        }

        Set<String> seenIds = new HashSet<>();
        for (QuestionnaireQuestion question : questions) {
            problems.addAll(problemsWith(question, seenIds));
        }

        if (!problems.isEmpty()) {
            throw new QuestionnaireGenerationException(
                    "The generated questionnaire is not usable: " + String.join("; ", problems) + ".");
        }
    }

    private List<String> problemsWith(QuestionnaireQuestion question, Set<String> seenIds) {
        List<String> problems = new ArrayList<>();
        if (question.id() == null || question.id().isBlank()) {
            problems.add("a question has no ID");
        } else if (!seenIds.add(question.id())) {
            problems.add("question ID '%s' is used more than once".formatted(question.id()));
        }
        if (question.prompt() == null || question.prompt().isBlank()) {
            problems.add("question '%s' has no prompt".formatted(question.id()));
        }
        if (question.type() == null) {
            problems.add("question '%s' has no type".formatted(question.id()));
            return problems;
        }

        List<QuestionOption> options = question.options() == null ? List.of() : question.options();
        if (question.type() == QuestionType.FREE_TEXT) {
            if (!options.isEmpty()) {
                problems.add("free-text question '%s' must not have options".formatted(question.id()));
            }
            return problems;
        }

        if (options.size() < MIN_OPTIONS) {
            problems.add("question '%s' offers %d options, fewer than the %d a choice needs"
                    .formatted(question.id(), options.size(), MIN_OPTIONS));
            return problems;
        }

        Set<String> seenOptionIds = new HashSet<>();
        for (QuestionOption option : options) {
            if (option.id() == null || option.id().isBlank()) {
                problems.add("an option of question '%s' has no ID".formatted(question.id()));
            } else if (!seenOptionIds.add(option.id())) {
                problems.add("question '%s' reuses option ID '%s'".formatted(question.id(), option.id()));
            }
            if (option.label() == null || option.label().isBlank()) {
                problems.add("an option of question '%s' has no label".formatted(question.id()));
            }
        }
        return problems;
    }
}
