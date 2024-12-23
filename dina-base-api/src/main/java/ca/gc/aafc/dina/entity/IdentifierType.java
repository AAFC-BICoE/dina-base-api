package ca.gc.aafc.dina.entity;

import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.vocabulary.VocabularyElement;

import lombok.Getter;

@Getter
public class IdentifierType implements VocabularyElement {

  /**
   * The key should not contain dot(.) See {@link VocabularyElement#getKey()}
   */
  private String key;

  private String name;

  // usually a URI
  private String term;

  private MultilingualTitle multilingualTitle;

  /**
   * Like wikidata. A URI template where "$1" can be automatically replaced with the value
   * assigned to the identifier.
   */
  private String uriTemplate;

  public void setKey(String key) {
    
  }

}
