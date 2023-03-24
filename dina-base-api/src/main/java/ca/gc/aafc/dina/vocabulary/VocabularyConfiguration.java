package ca.gc.aafc.dina.vocabulary;

import java.util.List;
import java.util.Map;

public class VocabularyConfiguration<T extends VocabularyElement> {
  
  private final Map<String, List<T>> vocabulary;

  public VocabularyConfiguration(Map<String, List<T>> vocabulary) {
    this.vocabulary = vocabulary;
  }

  public Map<String, List<T>> getVocabulary() {
    return vocabulary;
  }

}
