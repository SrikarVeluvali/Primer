package dev.primer.ai;

import java.util.regex.Pattern;

/**
 * Recovers the JSON document from a model reply that carries more than JSON.
 * <p>
 * Asking for structured output does not guarantee a reply that is only structured output. Reasoning
 * models emit a {@code <think>} block first, chat-tuned models like to wrap answers in Markdown
 * fences, and some prepend a sentence of commentary. All three are common enough across providers
 * that handling them belongs here rather than in any one provider or prompt.
 */
public final class StructuredOutputSanitizer {

    private static final Pattern THINK_BLOCK =
            Pattern.compile("<think>.*?</think>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern UNCLOSED_THINK_BLOCK =
            Pattern.compile("^\\s*<think>.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE_FENCE =
            Pattern.compile("```(?:json)?\\s*(.*?)\\s*```", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private StructuredOutputSanitizer() {
    }

    public static String sanitize(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String cleaned = THINK_BLOCK.matcher(content).replaceAll("").trim();
        // A reply cut off mid-reasoning leaves an opening tag with no close and no JSON after it.
        // Blanking it here produces a clearer "empty response" failure than a parse error would.
        cleaned = UNCLOSED_THINK_BLOCK.matcher(cleaned).replaceAll("").trim();

        var fence = CODE_FENCE.matcher(cleaned);
        if (fence.find()) {
            cleaned = fence.group(1).trim();
        }

        return extractDocument(cleaned);
    }

    /**
     * Trims anything outside the outermost JSON object or array, so a leading "Here you go:" or a
     * trailing remark does not break parsing. Braces inside strings are skipped so a prompt or label
     * containing one cannot end the document early.
     */
    private static String extractDocument(String content) {
        int start = firstIndexOfEither(content, '{', '[');
        if (start < 0) {
            return content;
        }

        char open = content.charAt(start);
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = start; index < content.length(); index++) {
            char character = content.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (character == '\\') {
                escaped = true;
                continue;
            }
            if (character == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (character == open) {
                depth++;
            } else if (character == close) {
                depth--;
                if (depth == 0) {
                    return content.substring(start, index + 1);
                }
            }
        }
        return content.substring(start);
    }

    private static int firstIndexOfEither(String content, char first, char second) {
        int firstIndex = content.indexOf(first);
        int secondIndex = content.indexOf(second);
        if (firstIndex < 0) {
            return secondIndex;
        }
        if (secondIndex < 0) {
            return firstIndex;
        }
        return Math.min(firstIndex, secondIndex);
    }
}
