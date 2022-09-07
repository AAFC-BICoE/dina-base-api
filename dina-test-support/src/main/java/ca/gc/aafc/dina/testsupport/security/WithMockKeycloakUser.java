package ca.gc.aafc.dina.testsupport.security;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to mock a Keycloak user for testing purpose.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockKeycloakSecurityContextFactory.class)
public @interface WithMockKeycloakUser {

  String username() default "test user";

  /**
   * Format {"group:role", "group2:role2"}
   *
   * @return
   */
  String[] groupRole() default "";

  String agentIdentifier() default "";

  String internalIdentifier() default "";

  boolean failOnInvalidNotation() default true;

}
