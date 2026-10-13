package dev.folio.retrieval;

import dev.folio.ollama.OllamaClient;
import dev.folio.paper.Paper;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RetrievalService {
    public record Source(int number, int page, String text, double similarity) {}
    private final VectorIndex index;
    private final OllamaClient ollama;
    public RetrievalService(VectorIndex index, OllamaClient ollama) { this.index = index; this.ollama = ollama; }
    public List<Source> retrieve(Paper paper, String question) {
        List<VectorIndex.Match> matches;
        if (isOverview(question)) matches = index.overview(paper.id, 6);
        else {
            double[] query = ollama.embed(paper.embeddingModel, List.of(question)).getFirst();
            matches = index.search(paper.id, query, 6);
        }
        List<Source> selected = new ArrayList<>();
        for (var match : matches) selected.add(new Source(selected.size() + 1, match.page(), match.text(), match.similarity()));
        if (selected.isEmpty()) throw new IllegalStateException("No indexed passages found for this paper.");
        return selected;
    }
    public static boolean isOverview(String question) {
        return question.toLowerCase(Locale.ROOT).matches("(?s).*\\b(summarize|summarise|summary|overview|big picture|main contribution|key findings|main findings|main idea)\\b.*")
                || question.toLowerCase(Locale.ROOT).matches("(?s).*what.*paper.*about.*");
    }
}
