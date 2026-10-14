package dev.folio.personal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PersonalDocumentRepository extends JpaRepository<PersonalDocument,UUID> {
    Optional<PersonalDocument> findByUserIdAndDocumentKey(UUID userId,String documentKey);
}
