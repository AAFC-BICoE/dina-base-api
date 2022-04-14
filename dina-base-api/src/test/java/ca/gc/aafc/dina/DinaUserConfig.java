package ca.gc.aafc.dina;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;

/** When you need an Authenticated user bean */
@Configuration
public class DinaUserConfig {

  public static final String AUTH_USER_NAME = "username";

  @Bean
  public DinaAuthenticatedUser user() {
    return DinaAuthenticatedUser.builder().username(DinaUserConfig.AUTH_USER_NAME).build();
  }

}
