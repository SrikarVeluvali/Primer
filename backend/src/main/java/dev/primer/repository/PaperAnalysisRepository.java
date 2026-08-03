package dev.primer.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import dev.primer.entity.PaperAnalysis;

public interface PaperAnalysisRepository extends JpaRepository<PaperAnalysis, UUID> {

    Optional<PaperAnalysis> findByPaperId(UUID paperId);

    /**
     * Creates the row for a paper if it does not already have one. {@code ON CONFLICT DO NOTHING}
     * makes this safe to call from concurrent requests, which is what lets the caller follow it with
     * an unconditional {@link #claimForGeneration} rather than a read-then-write race.
     */
    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO paper_analyses (id, paper_id, status, attempts, created_at, updated_at)
            VALUES (:id, :paperId, 'PENDING', 0, now(), now())
            ON CONFLICT (paper_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("paperId") UUID paperId);

    /**
     * Atomically takes ownership of generation for one paper, returning 1 if this caller won and 0 if
     * generation is already running, already finished, or has failed too many times. A single
     * conditional UPDATE is what stops repeated PDF accesses from launching duplicate LLM runs.
     */
    @Transactional
    @Modifying
    @Query(value = """
            UPDATE paper_analyses
            SET status = 'GENERATING', attempts = attempts + 1, updated_at = now()
            WHERE paper_id = :paperId
              AND status IN ('PENDING', 'FAILED')
              AND attempts < :maxAttempts
            """, nativeQuery = true)
    int claimForGeneration(@Param("paperId") UUID paperId, @Param("maxAttempts") int maxAttempts);
}
