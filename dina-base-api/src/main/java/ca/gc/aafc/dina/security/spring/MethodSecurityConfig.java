package ca.gc.aafc.dina.security.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables spring method security using a custom spring expression handler.
 * Configuration depends on property keycloak.enabled = true. Spring method
 * security will not be active without keycloak.
 */
@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", havingValue = "true")
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

  @Autowired
  private ApplicationContext context;

  // Custom handler is now provided via bean instead of override
  @Bean
  public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
    DinaSecurityExpressionHandler handler = new DinaSecurityExpressionHandler();
    handler.setApplicationContext(context);
    return handler;
  }

}
