package dev.primer.service.questionnaire;

/**
 * How much the concept a question probes matters for understanding the paper. The later analyzer
 * ranks knowledge gaps by this, so a gap in a CORE concept outweighs several PERIPHERAL ones.
 */
public enum QuestionImportance {
    CORE,
    HELPFUL,
    PERIPHERAL
}
