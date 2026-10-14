package dev.folio.personal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "documentKey"}))
public class PersonalDocument {
    @Id public UUID id = UUID.randomUUID();
    @Column(nullable=false) public UUID userId;
    @Column(nullable=false, length=150) public String documentKey;
    @Lob public String content;
    public Instant updatedAt = Instant.now();
    @Version public Long version;
    protected PersonalDocument() {}
    public PersonalDocument(UUID userId, String key, String content) { this.userId=userId; this.documentKey=key; this.content=content; }
}
