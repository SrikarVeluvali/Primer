package dev.primer.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import dev.primer.exception.InvalidArxivReferenceException;

@Component
public class ArxivReferenceParser {

    private static final Pattern ID_PATTERN = Pattern.compile("^(\\d{4}\\.\\d{4,5})(v\\d+)?$");

    public ArxivReference parse(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            throw new InvalidArxivReferenceException("An arXiv ID or arxiv.org URL is required.");
        }

        String candidate = rawReference.trim();
        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            candidate = idFromUrl(candidate);
        }

        Matcher matcher = ID_PATTERN.matcher(candidate);
        if (!matcher.matches()) {
            throw new InvalidArxivReferenceException("Use an arXiv ID such as 2607.19354 or an arxiv.org /abs/ or /pdf/ URL.");
        }

        String canonicalId = matcher.group(1) + (matcher.group(2) == null ? "" : matcher.group(2));
        return new ArxivReference(canonicalId, "https://arxiv.org/pdf/" + canonicalId);
    }

    private String idFromUrl(String rawUrl) {
        try {
            URI uri = new URI(rawUrl);
            if (!"arxiv.org".equalsIgnoreCase(uri.getHost())) {
                throw new InvalidArxivReferenceException("Only arxiv.org URLs are accepted.");
            }

            String path = uri.getPath();
            if (path == null) {
                throw new InvalidArxivReferenceException("The arXiv URL must contain an ID.");
            }
            if (path.startsWith("/abs/")) {
                return path.substring("/abs/".length());
            }
            if (path.startsWith("/pdf/")) {
                String id = path.substring("/pdf/".length());
                return id.endsWith(".pdf") ? id.substring(0, id.length() - 4) : id;
            }
        } catch (URISyntaxException exception) {
            throw new InvalidArxivReferenceException("The arXiv URL is not valid.");
        }
        throw new InvalidArxivReferenceException("Use an arxiv.org /abs/ or /pdf/ URL.");
    }
}
