package ca.gc.aafc.dina.vocabulary;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Configuration
@ConfigurationProperties
public class VocabularyConfiguration {
  
  private final Map<String, List<VocabularyElement>> vocabulary;

  public VocabularyConfiguration(Map<String, List<VocabularyElement>> vocabulary) {
    this.vocabulary = vocabulary;
  }

  public Map<String, List<VocabularyElement>> getVocabulary() {
    return vocabulary;
  }

  @NoArgsConstructor
  @Getter
  @Setter
  public static class VocabularyElement {
    private String name;
    private String term;
    private Map<String, String> labels;
  }
}