package dev.folio.learn;

import dev.folio.http.HttpTransport;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static dev.folio.learn.LearningTypes.*;

/** Public topic search only; personal answers, names, notes and goals never enter search queries. */
@Service
public class ResourceDiscovery {
    public record Result(List<Resource> resources,String status) {}
    private record Cached(List<Resource> resources,Instant expires) {}
    private final Map<String,Cached> cache=new ConcurrentHashMap<>();
    private final String searchApiKey;
    private final ObjectMapper json;
    private volatile Instant publicSearchBlockedUntil=Instant.EPOCH;
    public ResourceDiscovery(ObjectMapper json,@Value("${primer.search-api-key:}") String searchApiKey) {this.json=json;this.searchApiKey=searchApiKey;}
    private static final Set<String> STOP=Set.of("the","and","for","with","learning","introduction","basics","of","to","in","tutorial");
    public Result find(String topic) {
        return find(topic,"");
    }
    public Result find(String topic,String publicPaperTitle) {
        String safeTopic=publicTopic(topic).replaceAll("[^\\p{L}\\p{N} .+-]"," ").strip();
        if(safeTopic.length()>120) safeTopic=safeTopic.substring(0,120);
        List<Resource> resources=new ArrayList<>();
        String subject=safeTopic;
        var queries=List.of("youtube","blog","reddit");
        // Three bounded independent requests, shared by topic across learners.
        var tasks=queries.stream().map(type -> CompletableFuture.supplyAsync(() -> search(subject,type))).toList();
        for(var task:tasks) try { resources.addAll(task.get(25,TimeUnit.SECONDS)); } catch(Exception ignored) { task.cancel(true); }
        Set<String> seen=new HashSet<>(); resources.removeIf(r -> !seen.add(r.url()));
        resources.addAll(curated(subject+" "+publicPaperTitle).stream().filter(r -> seen.add(r.url())).toList());
        boolean live=resources.stream().anyMatch(r -> r.origin().equals("Web search"));
        return new Result(resources,live?"Live topic search, with curated references when available.":"Web search is unavailable or returned no relevant matches. Curated references and clearly labeled search links are shown instead.");
    }
    private List<Resource> search(String topic,String type) {
        String query=topic+" tutorial "+(type.equals("youtube")?"site:youtube.com/watch":type.equals("reddit")?"site:reddit.com/r/":"guide explanation -site:youtube.com -site:reddit.com");
        var cached=cache.get(query); if(cached!=null && cached.expires().isAfter(Instant.now())) return cached.resources();
        if(!searchApiKey.isBlank()) return brave(topic,type,query);
        if(publicSearchBlockedUntil.isAfter(Instant.now())) return List.of();
        try {
            var response=HttpTransport.request(URI.create("https://html.duckduckgo.com/html/?q="+URLEncoder.encode(query,StandardCharsets.UTF_8)),null,Duration.ofSeconds(15),2_000_000);
            if(response.status()!=200) {publicSearchBlockedUntil=Instant.now().plusSeconds(600);return List.of();}
            var document=Jsoup.parse(response.text()); List<Resource> out=new ArrayList<>();
            for(var item:document.select(".result")) {
                var link=item.selectFirst("a.result__a"); if(link==null) continue;
                String url=decode(link.attr("href")); if(!allowed(url,type) || !relevant(topic,link.text()+" "+item.select(".result__snippet").text())) continue;
                String description=item.select(".result__snippet").text();
                out.add(new Resource(UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)).toString(),link.text(),url,type,description.length()>350?description.substring(0,350):description,topic,"Web search"));
                if(out.size()==3) break;
            }
            if(cache.size()>300) cache.clear(); cache.put(query,new Cached(out,Instant.now().plusSeconds(out.isEmpty()?120:1800))); return out;
        } catch(Exception e) { return List.of(); }
    }
    private List<Resource> brave(String topic,String type,String query) {
        try {
            var response=HttpTransport.request(URI.create("https://api.search.brave.com/res/v1/web/search?q="+URLEncoder.encode(query,StandardCharsets.UTF_8)+"&count=8"),null,Duration.ofSeconds(15),2_000_000,Map.of("X-Subscription-Token",searchApiKey,"Accept","application/json"));
            if(response.status()!=200)return List.of();
            List<Resource> results=new ArrayList<>();
            for(var item:json.readTree(response.text()).path("web").path("results")) {
                String url=item.path("url").asText(),title=item.path("title").asText();String description=Jsoup.parse(item.path("description").asText()).text();
                if(!allowed(url,type)||!relevant(topic,title+" "+description))continue;
                results.add(new Resource(UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8)).toString(),title,url,type,description,topic,"Web search"));if(results.size()==3)break;
            }
            cache.put(query,new Cached(results,Instant.now().plusSeconds(results.isEmpty()?120:1800)));return results;
        }catch(Exception e){return List.of();}
    }
    static String decode(String value) {
        if(value.startsWith("//")) value="https:"+value;
        URI uri=URI.create(value);
        if(uri.getHost()!=null && uri.getHost().endsWith("duckduckgo.com") && uri.getRawQuery()!=null) {
            for(String part:uri.getRawQuery().split("&")) if(part.startsWith("uddg=")) return URLDecoder.decode(part.substring(5),StandardCharsets.UTF_8);
        }
        return value;
    }
    public static boolean allowed(String value,String type) {
        try {
            var uri=URI.create(value); String host=uri.getHost();
            if(!"https".equals(uri.getScheme()) || host==null || uri.getUserInfo()!=null || uri.getPort()!=-1 || host.equals("localhost") || !host.contains(".") || host.matches("[0-9.]+")) return false;
            boolean youtube=host.equals("youtube.com") || host.endsWith(".youtube.com") || host.equals("youtu.be");
            boolean reddit=host.equals("reddit.com") || host.endsWith(".reddit.com");
            return type.equals("youtube")?youtube && (uri.getPath().equals("/watch") || host.equals("youtu.be")):type.equals("reddit")?reddit && uri.getPath().contains("/comments/"):!youtube && !reddit;
        } catch(Exception e) { return false; }
    }
    public static boolean relevant(String topic,String text) {
        var terms=Arrays.stream(topic.toLowerCase(Locale.ROOT).split("\\W+")).filter(t -> t.length()>2 && !STOP.contains(t)).toList();
        String candidate=text.toLowerCase(Locale.ROOT);
        return !terms.isEmpty() && terms.stream().filter(candidate::contains).count() >= Math.min(2,terms.size());
    }
    private static String publicTopic(String topic) {
        String lower=topic.toLowerCase(Locale.ROOT);
        if(lower.contains("recurrent neural") || lower.matches(".*\\brnns?\\b.*"))return "recurrent neural networks";
        if(lower.contains("attention"))return "attention mechanisms";
        if(lower.contains("transformer"))return "transformer architecture";
        if(lower.contains("linear algebra") || lower.contains("matrix") || lower.contains("matrices"))return "linear algebra matrices";
        if(lower.contains("neural network"))return "neural networks";
        if(lower.contains("complexity"))return "computational complexity";
        if(lower.contains("probability") || lower.contains("softmax"))return "probability softmax";
        return topic.replaceFirst("^[^:]{1,50}:\\s*","");
    }
    private List<Resource> curated(String topic) {
        List<Resource> out=new ArrayList<>(); String lower=topic.toLowerCase(Locale.ROOT);
        if(lower.contains("attention") || lower.contains("transformer")) {
            out.add(new Resource("illustrated-transformer","The Illustrated Transformer","https://jalammar.github.io/illustrated-transformer/","blog","A visual walkthrough of embeddings, attention, and encoder-decoder architecture.","Attention and Transformers","Curated reference"));
            out.add(new Resource("attention-3b1b","Attention in transformers, step by step","https://www.3blue1brown.com/lessons/attention/","blog","An illustrated lesson by Grant Sanderson, with its companion video.","Attention mechanisms","Curated reference"));
            out.add(new Resource("attention-video","Attention in transformers, step by step — 3Blue1Brown","https://www.youtube.com/watch?v=eMlx5fFNoYc","youtube","Visualize queries, keys, values, and the attention mechanism in the original Transformer.","Attention mechanisms","Curated reference"));
            out.add(new Resource("transformer-video","Transformers, the technology behind LLMs — 3Blue1Brown","https://www.youtube.com/watch?v=wjZofJX0v4M","youtube","Build visual intuition for tokens, embeddings, and the Transformer architecture.","Transformers","Curated reference"));
            out.add(new Resource("transformer-discussion","Resources for understanding the original Transformer paper","https://www.reddit.com/r/MachineLearning/comments/pkedi4/d_resources_for_understanding_the_original/","reddit","A community discussion of explanations and study resources for the paper.","Transformers","Curated reference"));
        }
        if(lower.contains("neural") || lower.contains("deep learning")) {
            out.add(new Resource("neural-3b1b","But what is a neural network?","https://www.youtube.com/watch?v=aircAruvnKk","youtube","3Blue1Brown introduces network layers, weights, and activations visually.","Neural network foundations","Curated reference"));
            out.add(new Resource("neural-resources-reddit","Beginner resources for deep learning and neural networks","https://www.reddit.com/r/learnmachinelearning/comments/1vjr97q/absolute_beginner_here_what_are_the_easiest_most/","reddit","A learner discussion about visual introductions and foundational study resources.","Neural network foundations","Curated reference"));
        }
        if(lower.contains("recurrent") || lower.matches(".*\\brnns?\\b.*")) out.add(new Resource("d2l-rnn","Recurrent Neural Networks — Dive into Deep Learning","https://classic.d2l.ai/chapter_recurrent-neural-networks/index.html","blog","An open textbook chapter explaining sequence models and recurrent neural networks.","Recurrent neural networks","Curated reference"));
        if(lower.contains("matrix") || lower.contains("matrices") || lower.contains("linear algebra")) {
            out.add(new Resource("linear-transformations","Linear transformations and matrices","https://www.3blue1brown.com/lessons/linear-transformations/","blog","An illustrated explanation of matrices as transformations of space.","Linear algebra","Curated reference"));
            out.add(new Resource("matrix-composition","Matrix multiplication as composition","https://www.3blue1brown.com/lessons/matrix-multiplication/","blog","Build visual intuition for multiplying matrices before tackling attention equations.","Matrix multiplication","Curated reference"));
            out.add(new Resource("linear-video","Linear transformations and matrices — Essence of linear algebra","https://www.youtube.com/watch?v=kYB8IZa5AuE","youtube","3Blue1Brown visualizes linear transformations and the meaning of matrix entries.","Linear algebra","Curated reference"));
        }
        return out;
    }
}
