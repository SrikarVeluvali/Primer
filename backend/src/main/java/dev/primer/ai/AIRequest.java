package dev.primer.ai;

/**
 * Encapsulates a single AI request. Either {@code text}, {@code image}, or both must be present;
 * an empty request is rejected upstream by {@link AIService}.
 * The image array is defensively copied on construction and access so instances stay effectively
 * immutable even though {@code byte[]} is mutable.
 */
public record AIRequest(String text, byte[] image, String imageContentType) {

    public AIRequest {
        image = image != null ? image.clone() : null;
    }

    @Override
    public byte[] image() {
        return image != null ? image.clone() : null;
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    public boolean hasImage() {
        return image != null && image.length > 0;
    }
}
