package dev.folio.annotation;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;

@Entity @Table(indexes=@Index(columnList="userId,paperId"))
public class Highlight {
    @Id public UUID id=UUID.randomUUID();
    @Column(nullable=false) public UUID userId;
    @Column(nullable=false) public UUID paperId;
    public int page;
    @Lob public String text;
    @Lob public String rectangles;
    public String color;
    public Instant createdAt=Instant.now();
    protected Highlight() {}
    public Highlight(UUID userId,UUID paperId,int page,String text,String rectangles,String color) {
        this.userId=userId;this.paperId=paperId;this.page=page;this.text=text;this.rectangles=rectangles;this.color=color;
    }
}
