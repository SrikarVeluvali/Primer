package dev.primer.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePaperRequest(@NotBlank String arxivReference) {
}
