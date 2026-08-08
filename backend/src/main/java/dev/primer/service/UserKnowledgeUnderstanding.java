package dev.primer.service;

/**
 * Plain-language distillation of what a user already knows/doesn't know, derived from their
 * prerequisite questionnaire answers. Produced by {@link KnowledgeCrafterAgent}; intended to be
 * threaded into the prompt for the curated-summary generation step downstream.
 */
public record UserKnowledgeUnderstanding(String summary) {
}
