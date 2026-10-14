package dev.folio.personal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;

@Service
public class PersonalStore {
    private final PersonalDocumentRepository documents;
    private final ObjectMapper json;
    public PersonalStore(PersonalDocumentRepository documents,ObjectMapper json) { this.documents=documents; this.json=json; }
    public <T> T get(UUID user,String key,Class<T> type,T fallback) {
        return documents.findByUserIdAndDocumentKey(user,key).map(d -> {
            try { return json.readValue(d.content,type); } catch(Exception e) { throw new IllegalStateException("Your saved workspace could not be read.",e); }
        }).orElse(fallback);
    }
    @Transactional public <T> T put(UUID user,String key,T content) {
        try {
            var document=documents.findByUserIdAndDocumentKey(user,key).orElseGet(() -> new PersonalDocument(user,key,""));
            document.content=json.writeValueAsString(content); document.updatedAt=Instant.now(); documents.saveAndFlush(document); return content;
        } catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Your changes could not be saved.",e); }
    }
}
