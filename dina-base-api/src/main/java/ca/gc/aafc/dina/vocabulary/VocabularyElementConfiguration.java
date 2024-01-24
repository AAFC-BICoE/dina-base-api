package ca.gc.aafc.dina.vocabulary;

import ca.gc.aafc.dina.i18n.MultilingualTitle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Simple implementation of a single vocabulary element mostly used in configuration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyElementConfiguration implements VocabularyElement {

  /**
   * The key should not contain dot(.) since it is the character used to reference
   * a key within an extension. See {@link VocabularyElement#getKey()}
   */
  private String key;

  private String name;
  private String term;
  private MultilingualTitle multilingualTitle;
}
