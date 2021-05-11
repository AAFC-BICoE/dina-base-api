package ca.gc.aafc.dina;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import ca.gc.aafc.dina.property.YamlPropertyLoaderFactory;

@PropertySource(value = "classpath:supported-languages.yml",
  factory = YamlPropertyLoaderFactory.class)
@Configuration
@ConfigurationProperties
public class SupportedLanguagesConfiguration {

  private final List<String> supportedLanguages;
  
  public SupportedLanguagesConfiguration(List<String> supportedLanguages) {
    this.supportedLanguages = supportedLanguages;
  }

  public List<String> getSupportedLanguages() {
    return supportedLanguages;
  }
}
