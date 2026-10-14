package dev.folio;

import dev.folio.paper.ArxivClient;
import dev.folio.retrieval.TextChunker;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RetrievalPrimitivesTest {
    @Test void acceptsArxivFormatsAndRejectsUntrustedAddresses() {
        assertThat(ArxivClient.parseId("https://arxiv.org/pdf/1706.03762v7.pdf")).isEqualTo("1706.03762v7");
        assertThat(ArxivClient.parseId("arXiv:1706.03762")).isEqualTo("1706.03762");
        assertThat(ArxivClient.parseId("https://arxiv.org/abs/cond-mat/0207270")).isEqualTo("cond-mat/0207270");
        for (String address : new String[]{"https://arxiv.org.evil.test/abs/1706.03762", "http://localhost/abs/1706.03762", "https://arxiv.org:444/abs/1706.03762", "https://arxiv.org@evil.test/abs/1706.03762", "https://arxiv.org/abs/../../secret"})
            assertThatThrownBy(() -> ArxivClient.parseId(address)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void chunksCoverTheTextWithOverlapWithoutInfiniteLoops() {
        String text = "word ".repeat(1000) + "unique ending text that must remain in the final passage";
        var result = new TextChunker().split(text);
        assertThat(result).hasSizeGreaterThan(1).allMatch(c -> c.length() <= 1600);
        assertThat(result.getLast()).endsWith("unique ending text that must remain in the final passage");
        assertThat(result.get(1)).startsWith(result.getFirst().substring(result.getFirst().length() - 199).trim());
        assertThat(new TextChunker().split(" ")).isEmpty();
    }
    @Test void similarityRanksMatchingDirectionsAndRejectsDimensionMismatch() {
        assertThat(TextChunker.cosine(new double[]{1, 0}, new double[]{2, 0})).isEqualTo(1);
        assertThat(TextChunker.cosine(new double[]{1, 0}, new double[]{0, 1})).isZero();
        assertThat(TextChunker.cosine(new double[]{0, 0}, new double[]{1, 1})).isZero();
        assertThatThrownBy(() -> TextChunker.cosine(new double[]{1}, new double[]{1, 2})).isInstanceOf(IllegalStateException.class);
    }
}
