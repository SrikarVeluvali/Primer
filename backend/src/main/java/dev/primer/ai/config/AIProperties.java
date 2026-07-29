package dev.primer.ai.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Configuration for the AI feature, bound from the {@code primer.ai.*} properties.
 * <p>
 * {@code provider} is the Spring bean name of the active {@link dev.primer.ai.AIInterface}
 * implementation. Adding a new provider means writing a new {@code @Service("<name>")} class;
 * switching providers means changing only this one property value.
 */
@ConfigurationProperties(prefix = "primer.ai")
public class AIProperties {

    /**
     * Bean name of the active {@link dev.primer.ai.AIInterface} implementation, e.g. "gemini".
     */
    private String provider = "gemini";

    /**
     * Maximum accepted size for an uploaded image.
     */
    private DataSize maxImageSize = DataSize.ofMegabytes(15);

    /**
     * Content types accepted for uploaded images.
     */
    private List<String> allowedImageTypes = List.of("image/png", "image/jpeg", "image/webp");

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public DataSize getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(DataSize maxImageSize) {
        this.maxImageSize = maxImageSize;
    }

    public List<String> getAllowedImageTypes() {
        return allowedImageTypes;
    }

    public void setAllowedImageTypes(List<String> allowedImageTypes) {
        this.allowedImageTypes = allowedImageTypes;
    }
}
