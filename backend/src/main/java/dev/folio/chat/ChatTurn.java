package dev.folio.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(indexes = @Index(columnList = "userId,paperId"))
public class ChatTurn {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable = false) public UUID userId;
    @Column(nullable = false) public UUID paperId;
    @Lob public String question;
    @Lob public String answer;
    @Lob public String sources;
    public String model;
    @Column(nullable = false, columnDefinition = "integer default 0") public int indexVersion;
    public Instant createdAt = Instant.now();
    protected ChatTurn() {}
    public ChatTurn(UUID userId, UUID paperId, String question, String answer, String sources, String model) {
        this.userId = userId; this.paperId = paperId; this.question = question; this.answer = answer; this.sources = sources; this.model = model;
    }
}
