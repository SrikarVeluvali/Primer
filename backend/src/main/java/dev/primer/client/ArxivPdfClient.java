package dev.primer.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.stereotype.Component;

import dev.primer.exception.PaperDownloadException;

@Component
public class ArxivPdfClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public void download(ArxivReference reference, Path destination) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(reference.pdfUrl()))
                .timeout(Duration.ofSeconds(90))
                .header("User-Agent", "Primer/0.0.1 (paper ingestion)")
                .GET()
                .build();
        try {
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(destination));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(destination);
                throw new PaperDownloadException("arXiv returned HTTP " + response.statusCode() + ".");
            }
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase().startsWith("application/pdf") || !hasPdfSignature(destination)) {
                Files.deleteIfExists(destination);
                throw new PaperDownloadException("arXiv did not return a PDF document.");
            }
        } catch (IOException exception) {
            throw new PaperDownloadException("Could not download the arXiv PDF.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaperDownloadException("The arXiv PDF download was interrupted.", exception);
        }
    }

    private boolean hasPdfSignature(Path file) throws IOException {
        try (var input = Files.newInputStream(file)) {
            byte[] signature = input.readNBytes(5);
            return signature.length == 5 && signature[0] == '%' && signature[1] == 'P'
                    && signature[2] == 'D' && signature[3] == 'F' && signature[4] == '-';
        }
    }
}
