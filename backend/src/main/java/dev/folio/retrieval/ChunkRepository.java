package dev.folio.retrieval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {
    List<Chunk> findByPaperIdOrderByPosition(UUID paperId);
    @Transactional void deleteByPaperId(UUID paperId);
}
