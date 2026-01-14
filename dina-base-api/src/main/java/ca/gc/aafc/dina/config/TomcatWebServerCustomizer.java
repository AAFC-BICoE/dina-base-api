package ca.gc.aafc.dina.config;

import javax.inject.Named;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

/**
 * Adds custom configuration to Spring Boot's embedded Tomcat server.
 */
@Named
public class TomcatWebServerCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
  @Override
  public void customize(TomcatServletWebServerFactory factory) {
    // Allow square brackets in URLs because they are often used in JSONAPI requests.
    factory.addConnectorCustomizers(connector -> connector.setProperty("relaxedQueryChars", "[]"));
  }
}
