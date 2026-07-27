package dev.primer.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import dev.primer.config.PdfExtractionProperties;
import dev.primer.exception.PdfExtractionException;

class PdfExtractionServiceTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};

    @TempDir
    private Path temporaryDirectory;

    private final PdfExtractionProperties properties = new PdfExtractionProperties();
    private final PdfExtractionService service = new PdfExtractionService(properties);

    @Test
    void returnsOrderedPageTextWithoutImagesWhenNoFigureIsReferenced() throws Exception {
        Path pdf = writePdf("Introduction to the method", "Related work and background", "Concluding remarks");

        PdfExtractionResult result = service.extract(pdf);

        assertEquals(List.of(1, 2, 3), result.pages().stream().map(ExtractedPage::pageNumber).toList());
        assertTrue(result.pages().get(0).text().contains("Introduction to the method"));
        assertTrue(result.pages().get(1).text().contains("Related work and background"));
        assertTrue(result.pages().get(2).text().contains("Concluding remarks"));
        assertTrue(result.pages().stream().allMatch(page -> page.images().isEmpty()));
    }

    @Test
    void rendersOnlyPagesThatReferenceFigures() throws Exception {
        Path pdf = writePdf("Introduction with no references",
                "As shown in Fig. 1 the loss decreases",
                "Unrelated discussion of prior work",
                "Figure 2 reports the ablation study");

        PdfExtractionResult result = service.extract(pdf);

        assertEquals(List.of(2, 4), renderedPageNumbers(result));
        assertEquals(1, result.pages().get(1).images().size());
        assertEquals(1, result.pages().get(3).images().size());
    }

    @Test
    void matchesAbbreviatedAndSpacedFigureReferences() throws Exception {
        Path pdf = writePdf("compare fig 3 against the baseline",
                "SEE FIGURE 4 FOR THE ARCHITECTURE",
                "the residual is plotted in Fig.5 below",
                "no reference of any kind on this page");

        assertEquals(List.of(1, 2, 3), renderedPageNumbers(service.extract(pdf)));
    }

    @Test
    void rendersValidPngCarryingItsSourcePageNumber() throws Exception {
        Path pdf = writePdf("No figures on this page", "Results appear in Figure 1");

        PdfExtractionResult result = service.extract(pdf);

        RenderedPageImage image = result.pages().get(1).images().get(0);
        assertEquals(2, image.pageNumber());
        assertEquals("image/png", image.contentType());
        assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(image.bytes(), PNG_SIGNATURE.length));
        assertNotNull(ImageIO.read(new ByteArrayInputStream(image.bytes())));
    }

    @Test
    void rejectsMissingFile() {
        Path missing = temporaryDirectory.resolve("absent.pdf");

        assertThrows(PdfExtractionException.class, () -> service.extract(missing));
    }

    @Test
    void rejectsDirectory() {
        assertThrows(PdfExtractionException.class, () -> service.extract(temporaryDirectory));
    }

    @Test
    void rejectsFileThatIsNotAPdf() throws Exception {
        Path notAPdf = temporaryDirectory.resolve("notes.pdf");
        Files.writeString(notAPdf, "This is plain text, not a PDF document.");

        assertThrows(PdfExtractionException.class, () -> service.extract(notAPdf));
    }

    @Test
    void rejectsEncryptedPdf() throws Exception {
        Path encrypted = temporaryDirectory.resolve("encrypted.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.LETTER));
            document.protect(new StandardProtectionPolicy("owner-secret", "user-secret", new AccessPermission()));
            document.save(encrypted.toFile());
        }

        assertThrows(PdfExtractionException.class, () -> service.extract(encrypted));
    }

    @Test
    void rejectsPdfAboveTheSourceSizeLimit() throws Exception {
        Path pdf = writePdf("A document that is small but not tiny");
        properties.setMaxSourceSize(DataSize.ofBytes(64));

        assertThrows(PdfExtractionException.class, () -> service.extract(pdf));
    }

    @Test
    void rejectsPdfAboveThePageLimit() throws Exception {
        Path pdf = writePdf("One", "Two", "Three");
        properties.setMaxPages(2);

        assertThrows(PdfExtractionException.class, () -> service.extract(pdf));
    }

    @Test
    void rejectsPdfAboveTheRenderedFigurePageLimit() throws Exception {
        Path pdf = writePdf("Fig. 1 appears on this page", "Figure 2 appears on this page");
        properties.setMaxRenderedPages(1);

        assertThrows(PdfExtractionException.class, () -> service.extract(pdf));
    }

    @Test
    void rejectsPdfAboveTheTotalImageByteLimit() throws Exception {
        Path pdf = writePdf("Figure 1 appears on this page");
        properties.setMaxTotalImageSize(DataSize.ofBytes(128));

        assertThrows(PdfExtractionException.class, () -> service.extract(pdf));
    }

    private List<Integer> renderedPageNumbers(PdfExtractionResult result) {
        return result.pages().stream()
                .filter(page -> !page.images().isEmpty())
                .map(ExtractedPage::pageNumber)
                .toList();
    }

    private Path writePdf(String... pageTexts) throws IOException {
        Path file = temporaryDirectory.resolve(UUID.randomUUID() + ".pdf");
        try (PDDocument document = new PDDocument()) {
            for (String pageText : pageTexts) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(72, 700);
                    content.showText(pageText);
                    content.endText();
                }
            }
            document.save(file.toFile());
        }
        return file;
    }
}
