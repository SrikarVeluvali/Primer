package dev.folio.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class LocalVectorIndex implements VectorIndex {
    private final ChunkRepository chunks;
    private final ObjectMapper json;
    public LocalVectorIndex(ChunkRepository chunks, ObjectMapper json) { this.chunks = chunks; this.json = json; }
    @Override public List<Match> search(UUID paperId, double[] query, int limit) {
        List<Match> scored = new ArrayList<>();
        for (var chunk : chunks.findByPaperIdOrderByPosition(paperId)) {
            if (!PassageCleaner.readable(chunk.text) || PassageCleaner.bibliography(chunk.text)) continue;
            try { scored.add(new Match(chunk.page, chunk.text, TextChunker.cosine(query, json.readValue(chunk.embedding, double[].class)))); }
            catch (java.io.IOException e) { throw new IllegalStateException("The paper index could not be read.", e); }
        }
        scored.sort(Comparator.comparingDouble(Match::similarity).reversed());
        List<Match> selected = new ArrayList<>(); Map<Integer, Integer> perPage = new HashMap<>();
        for (var match : scored) {
            if (perPage.getOrDefault(match.page(), 0) >= 2) continue;
            selected.add(match); perPage.merge(match.page(), 1, Integer::sum);
            if (selected.size() == limit) break;
        }
        return selected;
    }
    @Override public List<Match> overview(UUID paperId, int limit) {
        record Candidate(Chunk chunk, int priority) {}
        List<Candidate> candidates = new ArrayList<>();
        for (var chunk : chunks.findByPaperIdOrderByPosition(paperId)) {
            if (!PassageCleaner.readable(chunk.text) || PassageCleaner.bibliography(chunk.text)) continue;
            String text = chunk.text.toLowerCase(Locale.ROOT);
            int priority = chunk.page == 1 ? 100 : chunk.page == 2 ? 55 : 0;
            if (text.contains("abstract")) priority += 50;
            if (text.matches("(?s).*\\bconclusions?\\b.*")) priority += 85;
            if (text.contains("we propose") || text.contains("we present")) priority += 30;
            if (text.contains("results") || text.contains("experiments")) priority += 35;
            if (text.contains("outperforms") || text.contains("achieves") || text.contains("limitations")) priority += 20;
            // A figure-only appendix cannot become the primary evidence for a paper summary.
            if (text.startsWith("figure ") && text.length() < 600) continue;
            candidates.add(new Candidate(chunk, priority));
        }
        candidates.sort(Comparator.comparingInt(Candidate::priority).reversed().thenComparingInt(c -> c.chunk.position));
        List<Match> result = new ArrayList<>(); Map<Integer, Integer> perPage = new HashMap<>();
        for (var candidate : candidates) {
            var chunk = candidate.chunk;
            if (perPage.getOrDefault(chunk.page, 0) >= 2) continue;
            result.add(new Match(chunk.page, chunk.text, 0)); perPage.merge(chunk.page, 1, Integer::sum);
            if (result.size() == limit) break;
        }
        return result;
    }
}
