package dev.primer.service.questionnaire;

import java.util.List;

/**
 * The "paper content" artifact from the design flow: a compact, structured reading of the paper that
 * downstream steps can use without re-reading the full text.
 */
public record PaperContent(
        String title,
        String summary,
        String domain,
        List<String> contributions,
        List<String> methods,
        List<String> prerequisiteConcepts) {
}
