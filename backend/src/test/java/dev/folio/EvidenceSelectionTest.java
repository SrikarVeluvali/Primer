package dev.folio;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.folio.retrieval.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EvidenceSelectionTest {
    private static final String PROSE = "The proposed architecture addresses sequence modeling using parallel attention mechanisms. Experiments compare model quality, training time, and performance on two translation benchmarks, showing improved accuracy and lower computational cost.";
    @Test void stripsRepeatedDiagramLabelsButPreservesNarrativeAndCaptions() {
        String diagram = "Input-Input Layer5\nThe The The The\nLaw Law Law Law\nwill will will will\n<pad> <pad> <pad>\n14\n";
        String clean = PassageCleaner.clean(diagram + "Figure 4: The attention heads demonstrate how the model resolves long distance dependencies.\n" + PROSE);
        assertThat(clean).doesNotContain("Layer5", "The The", "Law Law", "<pad>").contains("Figure 4:", "proposed architecture");
        assertThat(PassageCleaner.readable(clean)).isTrue();
        assertThat(PassageCleaner.readable(PassageCleaner.clean(diagram))).isFalse();
    }
    @Test void paperOverviewsUseAbstractIntroductionResultsAndConclusionInsteadOfDiagramAppendices() {
        UUID paper = UUID.randomUUID();
        var repository = mock(ChunkRepository.class);
        var fixture = List.of(
            new Chunk(paper, 1, 0, "Abstract\nWe propose a new attention architecture. " + PROSE, "[0,1]"),
            new Chunk(paper, 2, 1, "1 Introduction\n" + PROSE, "[0,1]"),
            new Chunk(paper, 8, 2, "6 Results\nOur model achieves improved benchmark scores. " + PROSE, "[0,1]"),
            new Chunk(paper, 10, 3, "7 Conclusion\n" + PROSE, "[0,1]"),
            new Chunk(paper, 14, 4, "Figure 4: " + PROSE, "[1,0]"),
            new Chunk(paper, 11, 5, "[1] First reference in the bibliography.\n[2] Second reference.\n[3] Third reference.\n" + PROSE, "[1,0]")
        );
        when(repository.findByPaperIdOrderByPosition(paper)).thenReturn(fixture);
        var matches = new LocalVectorIndex(repository, new ObjectMapper()).overview(paper, 6);
        assertThat(matches).extracting(VectorIndex.Match::page).containsExactly(1, 10, 2, 8).doesNotContain(14, 11);
        assertThat(RetrievalService.isOverview("Summarize the main contribution and key findings of this paper.")).isTrue();
        assertThat(RetrievalService.isOverview("Explain scaled dot-product attention.")).isFalse();
    }
}
