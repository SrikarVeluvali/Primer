package dev.folio.ollama;

import com.fasterxml.jackson.databind.*;
import dev.folio.http.HttpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.time.Duration;
import java.util.*;

@Component
public class OllamaClient {
    private final ObjectMapper json;
    private final String baseUrl;
    public final String chatModel;
    public final String embeddingModel;
    public OllamaClient(ObjectMapper json, @Value("${folio.ollama-url}") String baseUrl,
                        @Value("${folio.chat-model}") String chatModel, @Value("${folio.embedding-model}") String embeddingModel) {
        this.json = json; this.baseUrl = baseUrl.replaceAll("/+$", ""); this.chatModel = chatModel; this.embeddingModel = embeddingModel;
    }
    private JsonNode request(String path, Object body, Duration timeout) {
        try {
            var response = HttpTransport.request(URI.create(baseUrl + path), body == null ? null : json.writeValueAsString(body), timeout, 10_000_000);
            JsonNode result = json.readTree(response.text());
            if (response.status() >= 400) throw new IllegalStateException("Ollama: " + result.path("error").asText("request failed"));
            return result;
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Cannot reach Ollama. Start it with 'ollama serve' and check the configured address.", e); }
    }
    public List<String> models() {
        List<String> names = new ArrayList<>();
        request("/api/tags", null, Duration.ofSeconds(5)).path("models").forEach(n -> names.add(n.path("name").asText()));
        return names;
    }
    public List<String> chatModels() {
        List<String> available = new ArrayList<>();
        for (String name : models()) {
            if (name.contains(":cloud")) continue;
            JsonNode details = request("/api/show", Map.of("model", name), Duration.ofSeconds(10));
            if (details.hasNonNull("remote_model") || details.hasNonNull("remote_host")) continue;
            boolean completion = false;
            for (JsonNode capability : details.path("capabilities")) if (capability.asText().equals("completion")) completion = true;
            if (completion || (details.path("capabilities").isMissingNode() && !name.contains("embed"))) available.add(name);
        }
        return available;
    }
    public List<double[]> embed(String model, List<String> texts) {
        JsonNode vectors = request("/api/embed", Map.of("model", model, "input", texts, "truncate", false), Duration.ofMinutes(5)).path("embeddings");
        if (vectors.size() != texts.size()) throw new IllegalStateException("Ollama returned an incomplete embedding batch.");
        List<double[]> out = new ArrayList<>();
        vectors.forEach(v -> { double[] vector = new double[v.size()]; for (int i = 0; i < v.size(); i++) vector[i] = v.get(i).asDouble(); out.add(vector); });
        if (out.stream().anyMatch(v -> v.length == 0)) throw new IllegalStateException("Ollama returned an empty embedding.");
        return out;
    }
    public String chat(String model, List<Map<String, String>> messages) {
        if (!chatModels().contains(model)) throw new IllegalArgumentException("Choose an installed local chat model in Settings. Cloud and embedding models cannot answer questions here.");
        String answer = request("/api/chat", Map.of("model", model, "messages", messages, "stream", false,
                "options", Map.of("temperature", 0.2, "num_ctx", 16384)), Duration.ofMinutes(5)).path("message").path("content").asText();
        if (answer.isBlank()) throw new IllegalStateException("Ollama returned an empty answer. Try another chat model.");
        return answer;
    }
    public JsonNode structured(String model, String system, String prompt, Map<String, Object> schema) {
        if (!chatModels().contains(model)) throw new IllegalArgumentException("Choose an installed local chat model in Settings.");
        var response = request("/api/chat", Map.of("model", model, "messages", List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", prompt)),
                "stream", false, "format", schema, "options", Map.of("temperature", 0.2, "num_ctx", 16384, "num_predict", 6000)), Duration.ofMinutes(5));
        try { return json.readTree(response.path("message").path("content").asText()); }
        catch (Exception e) { throw new IllegalStateException("The model could not generate a valid learning plan. Retry or choose another local model.", e); }
    }
}
