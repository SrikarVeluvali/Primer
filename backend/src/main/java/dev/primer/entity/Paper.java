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

@Entity
@Table(name = "papers")
public class Paper {

    @Id
    private UUID id;

    @Column(name = "arxiv_id", nullable = false, unique = true, length = 64)
    private String arxivId;

    @Column(name = "source_url", nullable = false, length = 512)
    private String sourceUrl;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_status", nullable = false, length = 32)
    private PaperFileStatus fileStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private ProcessingStatus processingStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Paper() {
    }

    public Paper(UUID id, String arxivId, String sourceUrl, String storagePath) {
        this.id = id;
        this.arxivId = arxivId;
        this.sourceUrl = sourceUrl;
        this.storagePath = storagePath;
        this.fileStatus = PaperFileStatus.STORED;
        this.processingStatus = ProcessingStatus.PENDING;
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
    public String getArxivId() { return arxivId; }
    public String getStoragePath() { return storagePath; }
    public PaperFileStatus getFileStatus() { return fileStatus; }
    public void markFileMissing() { fileStatus = PaperFileStatus.MISSING; }
    public void markStored(String storagePath) {
        this.storagePath = storagePath;
        this.fileStatus = PaperFileStatus.STORED;
    }
}
