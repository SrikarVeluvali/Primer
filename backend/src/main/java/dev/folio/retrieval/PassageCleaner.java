package dev.folio.retrieval;

import java.util.*;
import java.util.regex.Pattern;

/** Removes diagram glyph rows without discarding their readable captions. */
public final class PassageCleaner {
    private static final Pattern WORD = Pattern.compile("\\p{L}{3,30}");
    private PassageCleaner() {}
    public static String clean(String text) {
        text = text.replaceAll("(?<=\\p{L})-\\R(?=\\p{Ll})", "");
        List<String> lines = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String line = raw.replaceAll("[\\t ]+", " ").trim();
            if (line.isEmpty() || line.matches("\\d{1,3}") || line.contains("<EOS>") || line.contains("<pad>")) continue;
            String[] tokens = line.split("\\s+");
            long distinct = Arrays.stream(tokens).map(t -> t.toLowerCase(Locale.ROOT)).distinct().count();
            if (tokens.length >= 3 && distinct == 1) continue;
            if (!WORD.matcher(line).find() && !line.contains("=")) continue;
            // Isolated diagram labels have no sentence context. Keep section headings.
            if (tokens.length < 3 && !line.matches("(?i)(?:\\d+(?:\\.\\d+)*\\s+)?(?:abstract|introduction|conclusions?|results|discussion|limitations|references|acknowledgments)")) continue;
            lines.add(line);
        }
        return String.join("\n", lines);
    }
    public static boolean readable(String text) {
        var words = WORD.matcher(text).results().map(m -> m.group().toLowerCase(Locale.ROOT)).toList();
        return words.size() >= 20 && new HashSet<>(words).size() >= 12;
    }
    public static boolean bibliography(String text) {
        return Pattern.compile("(?m)^\\[\\d+\\]").matcher(text).results().count() >= 3;
    }
}
