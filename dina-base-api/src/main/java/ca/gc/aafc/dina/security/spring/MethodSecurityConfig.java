package ca.gc.aafc.dina.security.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

/**
 * Enables spring method security using a custom spring expression handler.
 * Configuration depends on property keycloak.enabled = true. Spring method
 * security will not be active without keycloak.
 */
@Configuration
@ConditionalOnProperty(value = "keycloak.enabled", havingValue = "true")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

  @Autowired
  private ApplicationContext context;

  @Override
  protected MethodSecurityExpressionHandler createExpressionHandler() {
    DinaSecurityExpressionHandler handler = new DinaSecurityExpressionHandler();
    handler.setApplicationContext(context);
    return handler;
  }

}
