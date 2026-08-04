package dev.primer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "primer.questionnaire")
public class QuestionnaireProperties {

    /**
     * Fewest questions an acceptable questionnaire may contain.
     */
    private int minQuestions = 5;

    /**
     * Most questions an acceptable questionnaire may contain. Long questionnaires get abandoned.
     */
    private int maxQuestions = 10;

    /**
     * How many times generation may be attempted for one paper before it stays FAILED. Caps the API
     * spend on a paper the model consistently cannot handle.
     */
    private int maxAttempts = 3;

    /**
     * Characters of extracted paper text sent to each agent. Papers are truncated to this length so a
     * long PDF cannot blow past the model's context window or a provider's per-minute token budget.
     * <p>
     * Truncation keeps the front of the paper, which is where the title, abstract and introduction
     * live — the parts that state what a reader needs to know going in. That is what the agents are
     * looking for, so a smaller value costs less than it might appear to.
     */
    private int maxInputCharacters = 8_000;

    public int getMinQuestions() {
        return minQuestions;
    }

    public void setMinQuestions(int minQuestions) {
        this.minQuestions = minQuestions;
    }

    public int getMaxQuestions() {
        return maxQuestions;
    }

    public void setMaxQuestions(int maxQuestions) {
        this.maxQuestions = maxQuestions;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getMaxInputCharacters() {
        return maxInputCharacters;
    }

    public void setMaxInputCharacters(int maxInputCharacters) {
        this.maxInputCharacters = maxInputCharacters;
    }
}
