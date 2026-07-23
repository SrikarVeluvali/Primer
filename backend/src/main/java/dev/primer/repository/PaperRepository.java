package dev.primer.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.primer.entity.Paper;

public interface PaperRepository extends JpaRepository<Paper, UUID> {
    Optional<Paper> findByArxivId(String arxivId);
}
