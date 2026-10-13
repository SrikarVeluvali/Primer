package dev.folio.paper;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Paper {
    @Id public UUID id = UUID.randomUUID();
    @Column(unique = true, nullable = false) public String arxivId;
    @Column(length = 2000) public String title;
    @Column(length = 4000) public String authors = "";
    @Lob public String abstractText = "";
    public String published = "";
    public String status = "QUEUED";
    public String stage = "Waiting to index";
    @Column(length = 2000) public String error;
    public String embeddingModel;
    public int pageCount;
    public int chunkCount;
    @Column(nullable = false, columnDefinition = "integer default 0") public int indexVersion;
    public Instant createdAt = Instant.now();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "paper_tags", joinColumns = @JoinColumn(name = "paper_id"))
    @Column(name = "tag", length = 100)
    @OrderColumn(name = "tag_order")
    public List<String> tags = new ArrayList<>();
    protected Paper() {}
    public Paper(String arxivId, String embeddingModel) {
        this.arxivId = arxivId; this.title = "arXiv " + arxivId; this.embeddingModel = embeddingModel;
    }
}
