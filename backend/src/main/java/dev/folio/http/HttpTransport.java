package dev.folio.http;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Blocking transport with bounded response bodies and connect/read timeouts. */
public final class HttpTransport {
    private HttpTransport() {}
    public record Response(int status, String location, byte[] body) {
        public String text() { return new String(body, StandardCharsets.UTF_8); }
    }
    public static Response request(URI uri, String jsonBody, Duration timeout, int byteLimit) throws IOException {
        return request(uri,jsonBody,timeout,byteLimit,java.util.Map.of());
    }
    public static Response request(URI uri, String jsonBody, Duration timeout, int byteLimit, java.util.Map<String,String> headers) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout((int) timeout.toMillis());
        connection.setRequestProperty("User-Agent", "Primer/0.1 (local research reader)");
        headers.forEach(connection::setRequestProperty);
        if (jsonBody != null) {
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (var output = connection.getOutputStream()) { output.write(jsonBody.getBytes(StandardCharsets.UTF_8)); }
        }
        try {
            int status = connection.getResponseCode();
            String location = connection.getHeaderField("Location");
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            byte[] body = new byte[0];
            if (input != null) {
                try (input) { body = input.readNBytes(byteLimit + 1); }
                if (body.length > byteLimit) throw new IllegalStateException("The response exceeds the supported download limit.");
            }
            return new Response(status, location, body);
        } finally { connection.disconnect(); }
    }
}
