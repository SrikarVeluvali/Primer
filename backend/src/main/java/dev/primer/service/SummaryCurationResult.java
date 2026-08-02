package dev.primer.service;

/**
 * Placeholder result until the strategies actually call {@code AIService}. {@code strategyUsed}
 * lets callers (and tests) confirm routing worked without depending on real generated content yet.
 */
public record SummaryCurationResult(String strategyUsed, String message) {
}
