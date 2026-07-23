package dev.primer.service;

import java.nio.file.Path;
import java.util.UUID;

public record PaperPdf(UUID id, Path path) {
}
