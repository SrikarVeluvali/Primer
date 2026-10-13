package dev.folio.annotation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.folio.paper.PaperService;
import dev.folio.chat.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;

@Service
public class AnnotationService {
    public record Rect(double x,double y,double width,double height) {}
    public record Input(int page,String text,List<Rect> rectangles,String color) {}
    public record View(UUID id,int page,String text,List<Rect> rectangles,String color,Instant createdAt) {}
    private final HighlightRepository highlights;
    private final PaperService papers;
    private final ObjectMapper json;
    private final ChatService chats;
    public AnnotationService(HighlightRepository highlights,PaperService papers,ObjectMapper json,ChatService chats) { this.highlights=highlights;this.papers=papers;this.json=json;this.chats=chats; }
    private View view(Highlight h) {
        try { return new View(h.id,h.page,h.text,json.readValue(h.rectangles,new TypeReference<>(){}),h.color,h.createdAt); }
        catch(Exception e) { throw new IllegalStateException("Your highlight could not be read.",e); }
    }
    public List<View> list(UUID user,UUID paper) { papers.get(paper); return highlights.findByUserIdAndPaperIdOrderByCreatedAtAsc(user,paper).stream().map(this::view).toList(); }
    private Highlight owned(UUID user,UUID paper,UUID id) { return highlights.findByIdAndUserIdAndPaperId(id,user,paper).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Highlight not found.")); }
    @Transactional public View add(UUID user,UUID paperId,Input input) {
        var paper=papers.get(paperId);
        if(input.page()<1 || input.page()>paper.pageCount || input.text()==null || input.text().strip().length()<2 || input.text().length()>4000 || input.color()==null || !Set.of("amber","green","blue","pink").contains(input.color()) || input.rectangles()==null || input.rectangles().isEmpty() || input.rectangles().size()>120) throw new IllegalArgumentException("Select up to 4,000 characters from a valid PDF page.");
        for(var r:input.rectangles()) if(r==null || !Double.isFinite(r.x()+r.y()+r.width()+r.height()) || r.x()<0 || r.y()<0 || r.width()<=0 || r.height()<=0 || r.x()+r.width()>1.001 || r.y()+r.height()>1.001) throw new IllegalArgumentException("Invalid highlight coordinates.");
        if(highlights.countByUserIdAndPaperId(user,paperId)>=500)throw new IllegalArgumentException("Keep up to 500 highlights per paper.");
        try { return view(highlights.saveAndFlush(new Highlight(user,paperId,input.page(),input.text().strip(),json.writeValueAsString(input.rectangles()),input.color()))); }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Your highlight could not be saved.",e); }
    }
    public void delete(UUID user,UUID paper,UUID id) { highlights.delete(owned(user,paper,id)); }
    public ChatService.TurnView explain(UUID user,UUID paper,UUID id,String model) {
        var highlight=owned(user,paper,id);
        return chats.explain(user,paper,highlight.text,highlight.page,model);
    }
}
