package dev.primer.entity;

/**
 * How far the reader has progressed with a paper. Distinct from {@link ProcessingStatus}, which
 * tracks what the system has done to the file rather than what the reader has done with it.
 * <p>
 * The stage only ever moves forward, so the ordinal order here is the order of the journey.
 */
public enum ReadingStage {
    INGESTED,
    ACCESSED,
    QUESTIONNAIRE_READY,
    QUESTIONNAIRE_SKIPPED,
    QUESTIONNAIRE_ANSWERED
}
