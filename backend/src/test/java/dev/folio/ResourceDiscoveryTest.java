package dev.folio;

import dev.folio.learn.ResourceDiscovery;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ResourceDiscoveryTest {
    @Test void filtersUnrelatedSearchResultsAndRequiresRealResourceUrls() {
        assertThat(ResourceDiscovery.relevant("Transformer attention","Transformer attention tutorial for neural networks")).isTrue();
        assertThat(ResourceDiscovery.relevant("Transformer attention","Log into Facebook and recover your account")).isFalse();
        assertThat(ResourceDiscovery.allowed("https://www.youtube.com/watch?v=test","youtube")).isTrue();
        assertThat(ResourceDiscovery.allowed("https://www.youtube.com/results?search_query=test","youtube")).isFalse();
        assertThat(ResourceDiscovery.allowed("https://www.reddit.com/r/learnmachinelearning/comments/test/discussion/","reddit")).isTrue();
        assertThat(ResourceDiscovery.allowed("https://www.reddit.com/search/?q=test","reddit")).isFalse();
        assertThat(ResourceDiscovery.allowed("javascript:alert(1)","blog")).isFalse();
        assertThat(ResourceDiscovery.allowed("https://127.0.0.1/private","blog")).isFalse();
        assertThat(ResourceDiscovery.allowed("https://youtube.com.evil.example/watch?v=test","youtube")).isFalse();
    }
}
