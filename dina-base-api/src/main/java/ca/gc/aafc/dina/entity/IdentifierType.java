package ca.gc.aafc.dina.entity;

import java.util.List;

import ca.gc.aafc.dina.vocabulary.VocabularyElement;

public interface IdentifierType extends VocabularyElement, DinaEntity {

  /**
   * The key should not contain dot(.) See {@link VocabularyElement#getKey()}
   * @param key
   */
  void setKey(String key);

  /**
   * The component (material-sample, project) where this identifier type is expected to be used.
   */
  List<String> getDinaComponents();

  /**
   * Like wikidata. A URI template where "$1" can be automatically replaced with the value
   * assigned to the identifier.
   */
  String getUriTemplate();

}
