package dev.primer;

import dev.primer.ai.config.AIProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import dev.primer.config.PdfExtractionProperties;
import dev.primer.config.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, PdfExtractionProperties.class, AIProperties.class})
public class PrimerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrimerApplication.class, args);
    }
}
