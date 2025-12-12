package ca.gc.aafc.dina.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Loaded from application.yml
 */
@ConfigurationProperties(prefix = "elasticsearch")
@Component
@Getter
@Setter
@Validated
public class ElasticSearchProperties {

  @NotBlank
  private String host;

  /**
   * If not provided the value will be 0
   */
  private int port;

  private String certPath;

  private String username;
  private String password;
}
