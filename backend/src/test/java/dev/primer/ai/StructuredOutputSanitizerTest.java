package dev.primer.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StructuredOutputSanitizerTest {

    @Test
    void leavesACleanJsonObjectAlone() {
        assertThat(StructuredOutputSanitizer.sanitize("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsTheReasoningBlockAReasoningModelEmitsBeforeItsAnswer() {
        String reply = "<think>\nThe user wants questions. Let me plan.\n</think>\n{\"a\":1}";

        assertThat(StructuredOutputSanitizer.sanitize(reply)).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsAReasoningBlockThatUsesMixedCaseTags() {
        assertThat(StructuredOutputSanitizer.sanitize("<THINK>hmm</THINK>{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void returnsNothingUsableWhenTheReplyIsOnlyUnterminatedReasoning() {
        assertThat(StructuredOutputSanitizer.sanitize("<think>I was cut off mid-thought")).isEmpty();
    }

    @Test
    void unwrapsAMarkdownCodeFence() {
        assertThat(StructuredOutputSanitizer.sanitize("```json\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
    }

    @Test
    void unwrapsAnUnlabelledCodeFence() {
        assertThat(StructuredOutputSanitizer.sanitize("```\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
    }

    @Test
    void dropsCommentaryAroundTheDocument() {
        String reply = "Here is your questionnaire:\n{\"a\":1}\nLet me know if you want changes.";

        assertThat(StructuredOutputSanitizer.sanitize(reply)).isEqualTo("{\"a\":1}");
    }

    @Test
    void handlesReasoningAndFencingTogether() {
        String reply = "<think>planning</think>\nSure!\n```json\n{\"a\":1}\n```";

        assertThat(StructuredOutputSanitizer.sanitize(reply)).isEqualTo("{\"a\":1}");
    }

    @Test
    void keepsNestedObjectsIntact() {
        String reply = "prefix {\"a\":{\"b\":[1,2]},\"c\":3} suffix";

        assertThat(StructuredOutputSanitizer.sanitize(reply)).isEqualTo("{\"a\":{\"b\":[1,2]},\"c\":3}");
    }

    @Test
    void doesNotEndTheDocumentAtABraceInsideAString() {
        String reply = "{\"prompt\":\"what does } mean?\",\"a\":1}";

        assertThat(StructuredOutputSanitizer.sanitize(reply)).isEqualTo(reply);
    }

    @Test
    void doesNotEndTheDocumentAtAnEscapedQuote() {
        String reply = "{\"prompt\":\"say \\\"} \\\" aloud\",\"a\":1}";

        assertThat(StructuredOutputSanitizer.sanitize(reply)).isEqualTo(reply);
    }

    @Test
    void handlesATopLevelArray() {
        assertThat(StructuredOutputSanitizer.sanitize("noise [1,2,3] more")).isEqualTo("[1,2,3]");
    }

    @Test
    void passesNullAndBlankThroughUntouched() {
        assertThat(StructuredOutputSanitizer.sanitize(null)).isNull();
        assertThat(StructuredOutputSanitizer.sanitize("   ")).isEqualTo("   ");
    }

    @Test
    void leavesTextWithNoJsonAloneSoTheParseErrorNamesTheRealContent() {
        assertThat(StructuredOutputSanitizer.sanitize("I cannot help with that."))
                .isEqualTo("I cannot help with that.");
    }
}
