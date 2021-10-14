package ca.gc.aafc.dina.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@AllArgsConstructor
public class CorsConfiguration {

  private final CorsConfig corsConfig;

  @Bean
  @ConditionalOnProperty(name = "cors.origins")
  CorsConfigurationSource corsConfigurationSource() {
    org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
    configuration.setAllowedOrigins(corsConfig.getOrigins());
    configuration.setAllowedMethods(Arrays.asList("GET","POST", "PUT", "PATCH", "DELETE", "OPTION"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  boolean corsEnabled() {
    return corsConfig.corsEnabled();
  }

  @Configuration
  @ConfigurationProperties(prefix = "cors")
  @Data
  static class CorsConfig {
    private List<String> origins;

    public boolean corsEnabled() {
      return CollectionUtils.isNotEmpty(origins);
    }
  }
}
