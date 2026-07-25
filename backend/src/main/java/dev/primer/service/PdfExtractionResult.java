package dev.primer.service;

import java.util.List;

public record PdfExtractionResult(List<ExtractedPage> pages) {

    public PdfExtractionResult {
        pages = List.copyOf(pages);
    }
}
