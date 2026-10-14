package dev.folio.retrieval;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(indexes = @Index(columnList = "paperId"))
public class Chunk {
    @Id @GeneratedValue public Long id;
    public UUID paperId;
    public int page;
    public int position;
    @Lob public String text;
    @Lob public String embedding;
    protected Chunk() {}
    public Chunk(UUID paperId, int page, int position, String text, String embedding) {
        this.paperId = paperId; this.page = page; this.position = position; this.text = text; this.embedding = embedding;
    }
}
