package dev.primer.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import dev.primer.config.PdfExtractionProperties;

/**
 * Manual harness for eyeballing extraction against a real PDF. Skipped unless -Dprimer.pdf is set,
 * so it never runs in CI.
 *
 * mvn test -Dtest=PdfExtractionManualTest -Dprimer.pdf=/path/to/paper.pdf
 */
@EnabledIfSystemProperty(named = "primer.pdf", matches = ".+")
class PdfExtractionManualTest {

    @Test
    void extractsRealPdf() throws Exception {
        Path pdf = Path.of(System.getProperty("primer.pdf"));
        Path outputDirectory = Path.of(System.getProperty("primer.out", "target/pdf-extraction"));
        Files.createDirectories(outputDirectory);

        PdfExtractionService service = new PdfExtractionService(new PdfExtractionProperties());

        long start = System.currentTimeMillis();
        PdfExtractionResult result = service.extract(pdf);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println();
        System.out.println("Source  : " + pdf.toAbsolutePath());
        System.out.println("Pages   : " + result.pages().size());
        System.out.println("Elapsed : " + elapsed + " ms");
        System.out.println();

        long totalImageBytes = 0;
        long renderedPages = 0;
        for (ExtractedPage page : result.pages()) {
            String preview = page.text().replaceAll("\\s+", " ").trim();
            if (preview.length() > 64) {
                preview = preview.substring(0, 64) + "...";
            }
            System.out.printf("p%-3d %6d chars  %-7s %s%n",
                    page.pageNumber(), page.text().length(),
                    page.images().isEmpty() ? "" : "FIGURE", preview);

            for (RenderedPageImage image : page.images()) {
                byte[] bytes = image.bytes();
                totalImageBytes += bytes.length;
                renderedPages++;
                Files.write(outputDirectory.resolve("page-" + image.pageNumber() + ".png"), bytes);
            }
        }

        System.out.println();
        System.out.println("Rendered: " + renderedPages + " pages, " + (totalImageBytes / 1024) + " KiB total");
        System.out.println("PNGs in : " + outputDirectory.toAbsolutePath());
    }
}
