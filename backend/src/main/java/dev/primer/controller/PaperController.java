package dev.primer.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.primer.dto.request.CreatePaperRequest;
import dev.primer.service.PaperPdf;
import dev.primer.service.PaperService;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> createPaper(@Valid @RequestBody CreatePaperRequest request) throws IOException {
        return pdfResponse(paperService.ingest(request.arxivReference()), true);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> getPdf(@PathVariable UUID id) throws IOException {
        return pdfResponse(paperService.findPdf(id), false);
    }

    private ResponseEntity<InputStreamResource> pdfResponse(PaperPdf paperPdf, boolean created) throws IOException {
        String filename = paperPdf.id() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Paper-Id", paperPdf.id().toString());
        headers.setLocation(java.net.URI.create("/api/papers/" + paperPdf.id() + "/pdf"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.status(created ? 201 : 200)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(Files.size(paperPdf.path()))
                .body(new InputStreamResource(Files.newInputStream(paperPdf.path())));
    }
}
