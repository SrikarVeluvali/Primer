package dev.primer.service.questionnaire;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dev.primer.ai.AIRequest;
import dev.primer.ai.AIService;
import dev.primer.exception.InvalidQuestionnaireAnswerException;

/**
 * Decides which of a reader's submitted answers are too ambiguous to confidently derive their
 * actual knowledge from — across every question type, not just free text. A multi-choice answer
 * that contradicts itself is exactly as unusable downstream as a hedging free-text sentence is.
 * <p>
 * This assumes an answer set already exists (the submission endpoint that produces one isn't
 * built yet); it only validates and judges what it's given. Two layers of checking happen here:
 * a structural pass that rejects answers referencing questions/options that don't exist — a hard
 * usage error, no judgment required — then an AI pass that judges genuine ambiguity, which the
 * schema alone can't determine.
 */
@Component
public class AnswerAmbiguityDetector {

    private static final String PROMPT_TEMPLATE = """
            A reader answered a prerequisite questionnaire before reading a research paper. Decide \
            which of their answers are too ambiguous to confidently tell what they actually know.

            Flag an answer as ambiguous when:
              - a free-text answer hedges or is vague ("kind of", "not really sure", "maybe") \
            instead of clearly stating familiarity or lack of it
              - a choice answer selects options that contradict each other (e.g. both "no exposure" \
            and "expert fluency" for the same concept)
              - a free-text answer contradicts the reader's own choice-question selections for the \
            same or a closely related concept
              - a question was left with no answer at all

            Do NOT flag an answer just because it indicates low familiarity — "I don't know this" or \
            selecting the lowest familiarity option is a clear, useful answer, not an ambiguous one. \
            Only flag answers where the reader's actual knowledge level genuinely cannot be \
            determined from what they gave you.

            For each ambiguous answer, give its questionId exactly as shown below and a one-sentence \
            reason. If nothing is ambiguous, return an empty list.

            QUESTIONNAIRE AND ANSWERS:
            %s
            """;

    private final AIService aiService;

    public AnswerAmbiguityDetector(AIService aiService) {
        this.aiService = aiService;
    }

    public AmbiguityReport detect(Questionnaire questionnaire, QuestionnaireAnswers answers) {
        requireStructurallyValid(questionnaire, answers);

        String prompt = PROMPT_TEMPLATE.formatted(describe(questionnaire, answers));
        return aiService.processStructured(new AIRequest(prompt, null, null), AmbiguityReport.class);
    }

    /**
     * Rejects answers that reference a question/option that doesn't exist, or a single-choice
     * question answered with anything other than exactly one option. Deliberately does NOT reject
     * an empty multi-choice selection or a missing free-text answer — those are incomplete, not
     * invalid, and left for the AI pass to flag as ambiguous rather than hard-rejected here.
     */
    private void requireStructurallyValid(Questionnaire questionnaire, QuestionnaireAnswers answers) {
        Map<String, QuestionnaireQuestion> questionsById = questionnaire.questions().stream()
                .collect(Collectors.toMap(QuestionnaireQuestion::id, question -> question));

        for (QuestionnaireAnswer answer : answers.answers()) {
            QuestionnaireQuestion question = questionsById.get(answer.questionId());
            if (question == null) {
                throw new InvalidQuestionnaireAnswerException(
                        "Answer references unknown question '" + answer.questionId() + "'.");
            }
            if (question.type() == QuestionType.FREE_TEXT) {
                continue;
            }

            Set<String> validOptionIds = question.options().stream()
                    .map(QuestionOption::id)
                    .collect(Collectors.toSet());
            List<String> selected = answer.selectedOptionIds() == null ? List.of() : answer.selectedOptionIds();
            for (String optionId : selected) {
                if (!validOptionIds.contains(optionId)) {
                    throw new InvalidQuestionnaireAnswerException(
                            "Answer to question '" + answer.questionId() + "' selects unknown option '"
                                    + optionId + "'.");
                }
            }
            if (question.type() == QuestionType.SINGLE_CHOICE && selected.size() != 1) {
                throw new InvalidQuestionnaireAnswerException(
                        "Question '" + answer.questionId() + "' is single-choice but has "
                                + selected.size() + " selected option(s).");
            }
        }
    }

    private String describe(Questionnaire questionnaire, QuestionnaireAnswers answers) {
        Map<String, QuestionnaireAnswer> answersByQuestionId = answers.answers().stream()
                .collect(Collectors.toMap(QuestionnaireAnswer::questionId, answer -> answer));

        StringBuilder description = new StringBuilder();
        for (QuestionnaireQuestion question : questionnaire.questions()) {
            description.append(question.id())
                    .append(" [").append(question.importance()).append(", concept: ")
                    .append(question.concept()).append("] \"")
                    .append(question.prompt()).append("\"\n");

            if (question.type() != QuestionType.FREE_TEXT) {
                String optionsText = question.options().stream()
                        .map(option -> option.id() + ") " + option.label())
                        .collect(Collectors.joining("  "));
                description.append("  Options: ").append(optionsText).append("\n");
            }

            QuestionnaireAnswer answer = answersByQuestionId.get(question.id());
            if (answer == null) {
                description.append("  Reader's answer: (no answer provided)\n\n");
                continue;
            }
            if (question.type() == QuestionType.FREE_TEXT) {
                description.append("  Reader's answer (free text): \"")
                        .append(answer.freeText() == null ? "" : answer.freeText()).append("\"\n\n");
            } else {
                description.append("  Reader's answer: selected ")
                        .append(answer.selectedOptionIds()).append("\n\n");
            }
        }
        return description.toString();
    }
}
