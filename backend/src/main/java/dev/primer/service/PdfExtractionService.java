package dev.primer.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import dev.primer.config.PdfExtractionProperties;
import dev.primer.exception.PdfExtractionException;

@Service
public class PdfExtractionService {

    private static final Pattern FIGURE_REFERENCE =
            Pattern.compile("\\b(?:fig(?:ure)?)\\.?\\s*\\d+\\b", Pattern.CASE_INSENSITIVE);
    private static final String PNG_CONTENT_TYPE = "image/png";

    private final PdfExtractionProperties properties;

    public PdfExtractionService(PdfExtractionProperties properties) {
        this.properties = properties;
    }

    public PdfExtractionResult extract(Path pdfPath) {
        validateSource(pdfPath);
        try (PDDocument document = load(pdfPath)) {
            if (document.isEncrypted()) {
                throw new PdfExtractionException("Encrypted PDFs are not supported.");
            }

            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new PdfExtractionException("The PDF at " + pdfPath + " contains no pages.");
            }
            if (pageCount > properties.getMaxPages()) {
                throw new PdfExtractionException("The PDF has " + pageCount + " pages, above the limit of "
                        + properties.getMaxPages() + ".");
            }

            List<String> pageTexts = extractText(document, pageCount);
            Map<Integer, RenderedPageImage> rendered = render(document, figurePageNumbers(pageTexts));

            List<ExtractedPage> pages = new ArrayList<>(pageCount);
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                RenderedPageImage image = rendered.get(pageNumber);
                pages.add(new ExtractedPage(pageNumber, pageTexts.get(pageNumber - 1),
                        image == null ? List.of() : List.of(image)));
            }
            return new PdfExtractionResult(pages);
        } catch (IOException exception) {
            throw new PdfExtractionException("Could not extract content from the PDF at " + pdfPath + ".", exception);
        }
    }

    private void validateSource(Path pdfPath) {
        if (pdfPath == null) {
            throw new PdfExtractionException("A PDF path is required.");
        }
        if (!Files.isRegularFile(pdfPath)) {
            throw new PdfExtractionException("No PDF file exists at " + pdfPath + ".");
        }
        if (!Files.isReadable(pdfPath)) {
            throw new PdfExtractionException("The PDF at " + pdfPath + " is not readable.");
        }

        long size;
        try {
            size = Files.size(pdfPath);
        } catch (IOException exception) {
            throw new PdfExtractionException("Could not read the size of the PDF at " + pdfPath + ".", exception);
        }
        long limit = properties.getMaxSourceSize().toBytes();
        if (size > limit) {
            throw new PdfExtractionException("The PDF is " + size + " bytes, above the limit of " + limit + ".");
        }
    }

    private PDDocument load(Path pdfPath) {
        try {
            return Loader.loadPDF(pdfPath.toFile());
        } catch (InvalidPasswordException exception) {
            throw new PdfExtractionException("Encrypted PDFs are not supported.", exception);
        } catch (IOException exception) {
            throw new PdfExtractionException("The file at " + pdfPath + " could not be parsed as a PDF.", exception);
        }
    }

    private List<String> extractText(PDDocument document, int pageCount) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        List<String> pageTexts = new ArrayList<>(pageCount);
        for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            pageTexts.add(stripper.getText(document));
        }
        return pageTexts;
    }

    private List<Integer> figurePageNumbers(List<String> pageTexts) {
        List<Integer> figurePages = new ArrayList<>();
        for (int index = 0; index < pageTexts.size(); index++) {
            if (FIGURE_REFERENCE.matcher(pageTexts.get(index)).find()) {
                figurePages.add(index + 1);
            }
        }
        if (figurePages.size() > properties.getMaxRenderedPages()) {
            throw new PdfExtractionException("The PDF has " + figurePages.size()
                    + " figure pages, above the limit of " + properties.getMaxRenderedPages() + ".");
        }
        return figurePages;
    }

    private Map<Integer, RenderedPageImage> render(PDDocument document, List<Integer> figurePages) throws IOException {
        if (figurePages.isEmpty()) {
            return Map.of();
        }

        long limit = properties.getMaxTotalImageSize().toBytes();
        PDFRenderer renderer = new PDFRenderer(document);
        Map<Integer, RenderedPageImage> rendered = new LinkedHashMap<>();
        long totalBytes = 0;
        for (int pageNumber : figurePages) {
            byte[] png = toPng(renderer, pageNumber);
            totalBytes += png.length;
            if (totalBytes > limit) {
                throw new PdfExtractionException("The rendered figure pages total " + totalBytes
                        + " bytes, above the limit of " + limit + ".");
            }
            rendered.put(pageNumber, new RenderedPageImage(pageNumber, PNG_CONTENT_TYPE, png));
        }
        return rendered;
    }

    private byte[] toPng(PDFRenderer renderer, int pageNumber) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, properties.getRenderDpi(), ImageType.RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) {
            throw new PdfExtractionException("No PNG writer is available to render page " + pageNumber + ".");
        }
        return output.toByteArray();
    }
}
