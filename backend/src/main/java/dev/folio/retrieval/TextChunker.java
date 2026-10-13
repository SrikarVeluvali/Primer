package dev.folio.retrieval;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TextChunker {
    public List<String> split(String input) {
        String text = input.replaceAll("[\\t ]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + 1600, text.length());
            if (end < text.length()) {
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > start + 800) end = boundary;
            }
            if (end - start > 30) chunks.add(text.substring(start, end).trim());
            if (end == text.length()) break;
            start = Math.max(start + 1, end - 200);
        }
        return chunks;
    }
    public static double cosine(double[] a, double[] b) {
        if (a.length != b.length) throw new IllegalStateException("Embedding dimensions changed. Reindex this paper.");
        double dot = 0, aa = 0, bb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i]; }
        return aa == 0 || bb == 0 ? 0 : dot / Math.sqrt(aa * bb);
    }
}
