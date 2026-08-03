package dev.primer.service;

import java.util.UUID;

/**
 * Published when a reader opens a paper. Consumed asynchronously so the work it kicks off never sits
 * inside the request that served the PDF.
 */
public record PaperAccessedEvent(UUID paperId) {
}
