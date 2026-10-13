package dev.folio.paper;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PaperRepository extends JpaRepository<Paper, UUID> {
    Optional<Paper> findByArxivId(String arxivId);
    List<Paper> findAllByOrderByCreatedAtDesc();
}
