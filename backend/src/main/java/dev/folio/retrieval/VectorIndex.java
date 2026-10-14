package dev.folio.retrieval;

import java.util.*;

/** Replace this storage port with pgvector or another index without changing chat orchestration. */
public interface VectorIndex {
    record Match(int page, String text, double similarity) {}
    List<Match> search(UUID paperId, double[] query, int limit);
    List<Match> overview(UUID paperId, int limit);
}
