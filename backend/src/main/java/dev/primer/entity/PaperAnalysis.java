package dev.primer.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The output of the two analysis agents for one paper: the structured paper content and the
 * questionnaire built from it.
 * <p>
 * Both are stored as already-serialized JSON so a questionnaire is served to every reader exactly as
 * it was generated. That stability matters because answers will reference question and option IDs
 * from this document — re-generating it would silently orphan them.
 */
@Entity
@Table(name = "paper_analyses")
public class PaperAnalysis {

    private static final int MAX_ERROR_LENGTH = 1024;

    @Id
    private UUID id;

    @Column(name = "paper_id", nullable = false, unique = true)
    private UUID paperId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AnalysisStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_json")
    private String contentJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "questionnaire_json")
    private String questionnaireJson;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaperAnalysis() {
    }

    public PaperAnalysis(UUID id, UUID paperId) {
        this.id = id;
        this.paperId = paperId;
        this.status = AnalysisStatus.PENDING;
        this.attempts = 0;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPaperId() { return paperId; }
    public AnalysisStatus getStatus() { return status; }
    public String getContentJson() { return contentJson; }
    public String getQuestionnaireJson() { return questionnaireJson; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }

    public void markReady(String contentJson, String questionnaireJson) {
        this.contentJson = contentJson;
        this.questionnaireJson = questionnaireJson;
        this.lastError = null;
        this.status = AnalysisStatus.READY;
    }

    public void markFailed(String message) {
        this.lastError = truncate(message);
        this.status = AnalysisStatus.FAILED;
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
