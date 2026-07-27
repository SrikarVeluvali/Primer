package dev.primer.service;

import java.util.Objects;

public record RenderedPageImage(int pageNumber, String contentType, byte[] bytes) {

    public RenderedPageImage {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page numbers start at 1.");
        }
        Objects.requireNonNull(contentType, "contentType");
        bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
