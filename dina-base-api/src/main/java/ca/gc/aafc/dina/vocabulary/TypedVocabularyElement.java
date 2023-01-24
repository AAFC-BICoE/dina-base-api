package ca.gc.aafc.dina.vocabulary;

import ca.gc.aafc.dina.i18n.MultilingualDescription;

/**
 * Defines the main interface of a vocabulary with a type.
 */
public interface TypedVocabularyElement extends VocabularyElement {

  enum VocabularyElementType {
    INTEGER, STRING, DATE, BOOL, DECIMAL
  }

  /**
   * Immutable key representing the managed attribute.
   *
   * @return
   */
  String getKey();
  void setKey(String key);

  VocabularyElementType getVocabularyElementType();
  String[] getAcceptedValues();

  MultilingualDescription getMultilingualDescription();

}
