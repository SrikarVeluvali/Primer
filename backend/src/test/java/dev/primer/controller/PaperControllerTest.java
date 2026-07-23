package dev.primer.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.primer.service.PaperPdf;
import dev.primer.service.PaperService;

@WebMvcTest(PaperController.class)
class PaperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaperService paperService;

    @TempDir
    private Path temporaryDirectory;

    private PaperPdf paperPdf;

    @BeforeEach
    void setUp() throws Exception {
        UUID id = UUID.randomUUID();
        Path file = temporaryDirectory.resolve(id + ".pdf");
        Files.write(file, "%PDF-test".getBytes());
        paperPdf = new PaperPdf(id, file);
    }

    @Test
    void createsAndStreamsPdf() throws Exception {
        when(paperService.ingest("2607.19354")).thenReturn(paperPdf);

        mockMvc.perform(post("/api/papers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"arxivReference\":\"2607.19354\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Paper-Id", paperPdf.id().toString()))
                .andExpect(header().string("Location", "/api/papers/" + paperPdf.id() + "/pdf"));
    }

    @Test
    void streamsPreviouslyStoredPdf() throws Exception {
        when(paperService.findPdf(paperPdf.id())).thenReturn(paperPdf);

        mockMvc.perform(get("/api/papers/{id}/pdf", paperPdf.id()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Paper-Id", paperPdf.id().toString()));
    }
}
