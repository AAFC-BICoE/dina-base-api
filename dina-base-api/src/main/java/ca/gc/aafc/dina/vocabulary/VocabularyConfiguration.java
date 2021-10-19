package ca.gc.aafc.dina.vocabulary;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class VocabularyConfiguration<T extends VocabularyConfiguration.VocabularyElement> {
  
  private final Map<String, List<T>> vocabulary;

  public VocabularyConfiguration(Map<String, List<T>> vocabulary) {
    this.vocabulary = vocabulary;
  }

  public Map<String, List<T>> getVocabulary() {
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
