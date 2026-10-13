package dev.folio.paper;

import org.springframework.stereotype.Component;
import dev.folio.http.HttpTransport;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class ArxivClient {
    private static final Pattern ID = Pattern.compile("(?:\\d{4}\\.\\d{4,5}|[a-zA-Z-]+(?:\\.[A-Z]{2})?/\\d{7})(?:v[1-9]\\d*)?");
    public static String parseId(String value) {
        String id = value.trim();
        if (id.startsWith("arXiv:")) id = id.substring(6).trim();
        if (id.startsWith("http://") || id.startsWith("https://")) {
            URI uri;
            try { uri = URI.create(id); } catch (Exception e) { throw new IllegalArgumentException("Enter a valid arXiv link or paper ID."); }
            if (!Set.of("arxiv.org", "www.arxiv.org", "export.arxiv.org").contains(uri.getHost()) || uri.getUserInfo() != null || uri.getPort() != -1)
                throw new IllegalArgumentException("Use an arxiv.org abstract or PDF link.");
            String path = uri.getPath();
            if (!path.startsWith("/abs/") && !path.startsWith("/pdf/")) throw new IllegalArgumentException("Use an arXiv abstract or PDF link.");
            id = path.substring(5).replaceFirst("\\.pdf$", "");
        }
        if (!ID.matcher(id).matches()) throw new IllegalArgumentException("Enter a valid arXiv link or paper ID, such as 1706.03762.");
        return id;
    }
    private byte[] fetch(String url, int limit) throws Exception {
        URI uri = URI.create(url);
        for (int redirects = 0; redirects < 4; redirects++) {
            if (!"https".equals(uri.getScheme()) || !Set.of("arxiv.org", "export.arxiv.org", "www.arxiv.org").contains(uri.getHost()) || uri.getPort() != -1 || uri.getUserInfo() != null)
                throw new IllegalStateException("arXiv returned an unsupported download address.");
            var response = HttpTransport.request(uri, null, Duration.ofSeconds(90), limit);
            if (response.status() >= 300 && response.status() < 400) {
                if (response.location() == null) throw new IllegalStateException("arXiv returned an invalid redirect.");
                uri = uri.resolve(response.location()); continue;
            }
            if (response.status() != 200) throw new IllegalStateException("arXiv returned HTTP " + response.status() + ". Check the paper ID or retry later.");
            return response.body();
        }
        throw new IllegalStateException("Too many arXiv redirects.");
    }
    public void metadata(Paper paper) throws Exception {
        byte[] xml = fetch("https://export.arxiv.org/api/query?id_list=" + URLEncoder.encode(paper.arxivId, java.nio.charset.StandardCharsets.UTF_8), 2_000_000);
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
        var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        var entries = document.getElementsByTagName("entry");
        if (entries.getLength() == 0) throw new IllegalArgumentException("Paper not found on arXiv. Check the paper ID.");
        Element entry = (Element) entries.item(0);
        paper.title = text(entry, "title").replaceAll("\\s+", " ").trim();
        if (paper.title.equals("Error")) throw new IllegalArgumentException("Paper not found on arXiv. Check the paper ID.");
        paper.abstractText = text(entry, "summary").trim(); paper.published = text(entry, "published");
        List<String> authors = new ArrayList<>();
        var authorNodes = entry.getElementsByTagName("author");
        for (int i = 0; i < authorNodes.getLength(); i++) authors.add(text((Element) authorNodes.item(i), "name"));
        paper.authors = String.join(", ", authors);
    }
    private static String text(Element element, String tag) {
        var nodes = element.getElementsByTagName(tag); return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }
    public byte[] pdf(String id) throws Exception { return fetch("https://arxiv.org/pdf/" + id, 25_000_000); }
}
