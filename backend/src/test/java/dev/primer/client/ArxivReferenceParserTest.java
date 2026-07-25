package dev.primer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import dev.primer.exception.InvalidArxivReferenceException;

class ArxivReferenceParserTest {

    private final ArxivReferenceParser parser = new ArxivReferenceParser();

    @Test
    void acceptsBareArxivId() {
        ArxivReference reference = parser.parse("2607.19354");

        assertEquals("2607.19354", reference.id());
        assertEquals("https://arxiv.org/pdf/2607.19354", reference.pdfUrl());
    }

    @Test
    void acceptsPdfUrlWithVersionAndExtension() {
        ArxivReference reference = parser.parse("https://arxiv.org/pdf/2607.19354v2.pdf");

        assertEquals("2607.19354v2", reference.id());
    }

    @Test
    void rejectsNonArxivUrl() {
        assertThrows(InvalidArxivReferenceException.class,
                () -> parser.parse("https://example.com/pdf/2607.19354"));
    }
}
