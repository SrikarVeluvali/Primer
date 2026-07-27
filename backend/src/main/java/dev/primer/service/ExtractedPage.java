package dev.primer.service;

import java.util.List;
import java.util.Objects;

public record ExtractedPage(int pageNumber, String text, List<RenderedPageImage> images) {

    public ExtractedPage {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page numbers start at 1.");
        }
        Objects.requireNonNull(text, "text");
        images = List.copyOf(images);
    }
}
