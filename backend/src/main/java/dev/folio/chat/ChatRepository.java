package dev.folio.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public interface ChatRepository extends JpaRepository<ChatTurn, UUID> {
    List<ChatTurn> findByUserIdAndPaperIdOrderByCreatedAtAsc(UUID userId, UUID paperId);
    @Transactional void deleteByUserIdAndPaperId(UUID userId, UUID paperId);
}
