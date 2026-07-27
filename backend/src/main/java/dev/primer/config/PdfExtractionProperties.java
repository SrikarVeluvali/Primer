package dev.primer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "primer.pdf-extraction")
public class PdfExtractionProperties {

    private DataSize maxSourceSize = DataSize.ofMegabytes(50);
    private int maxPages = 100;
    private int renderDpi = 150;
    private int maxRenderedPages = 20;
    private DataSize maxTotalImageSize = DataSize.ofMegabytes(50);

    public DataSize getMaxSourceSize() {
        return maxSourceSize;
    }

    public void setMaxSourceSize(DataSize maxSourceSize) {
        this.maxSourceSize = maxSourceSize;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getRenderDpi() {
        return renderDpi;
    }

    public void setRenderDpi(int renderDpi) {
        this.renderDpi = renderDpi;
    }

    public int getMaxRenderedPages() {
        return maxRenderedPages;
    }

    public void setMaxRenderedPages(int maxRenderedPages) {
        this.maxRenderedPages = maxRenderedPages;
    }

    public DataSize getMaxTotalImageSize() {
        return maxTotalImageSize;
    }

    public void setMaxTotalImageSize(DataSize maxTotalImageSize) {
        this.maxTotalImageSize = maxTotalImageSize;
    }
}
