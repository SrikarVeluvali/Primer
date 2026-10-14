package dev.folio.api;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.*;

@Configuration
public class WebConfig {
    /** NIO2 also avoids legacy Windows JDK selector pipe failures. */
    @Bean TomcatServletWebServerFactory webServer() {
        var server = new TomcatServletWebServerFactory();
        server.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
        return server;
    }
}
