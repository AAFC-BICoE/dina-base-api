package ca.gc.aafc.dina;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import ca.gc.aafc.dina.property.YamlPropertyLoaderFactory;

@PropertySource(value = "classpath:supported-languages.yml",
  factory = YamlPropertyLoaderFactory.class)
@Configuration
@ConfigurationProperties
public class SupportedLanguagesConfiguration {

  private final List<String> supportedStringLanguages;
  
  public SupportedLanguagesConfiguration(List<String> supportedStringLanguages) {
    this.supportedStringLanguages = supportedStringLanguages;
  }

  public List<String> getSupportedStringLanguages() {
    return supportedStringLanguages;
  }
  
  public List<Locale> getSupportedLocaleLanguages() {
    List<Locale> locales = new ArrayList<>();
    for (String s : supportedStringLanguages) {
      locales.add(new Locale(s));
    }
    return locales;
  }
}
