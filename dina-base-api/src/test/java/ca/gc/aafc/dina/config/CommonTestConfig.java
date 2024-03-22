package ca.gc.aafc.dina.config;

import java.util.Properties;

import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that will create beans available for all tests.
 */
@Configuration
public class CommonTestConfig {

  @Bean
  public ResourceNameIdentifierConfig provideResourceNameIdentifierConfig() {
    return ResourceNameIdentifierConfig.builder().build();
  }

  @Bean
  public BuildProperties buildProperties() {
    Properties props = new Properties();
    props.setProperty("version", "test-api-version");
    return new BuildProperties(props);
  }

}
