package dev.folio.annotation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface HighlightRepository extends JpaRepository<Highlight,UUID> {
    List<Highlight> findByUserIdAndPaperIdOrderByCreatedAtAsc(UUID userId,UUID paperId);
    Optional<Highlight> findByIdAndUserIdAndPaperId(UUID id,UUID userId,UUID paperId);
    long countByUserIdAndPaperId(UUID userId,UUID paperId);
}
