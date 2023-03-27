package ca.gc.aafc.dina.vocabulary;

import ca.gc.aafc.dina.i18n.MultilingualTitle;

/**
 * Define the main interface of a vocabulary element in its simplest form.
 *
 */
public interface VocabularyElement {

  /**
   * Immutable and stable key representing the vocabulary.
   * Used a stable identifier without the system boundaries.
   *
   * @return
   */
  String getKey();

  /**
   * Common human-readable name given to the vocabulary.
   * Optional.
   * @return
   */
  String getName();

  /**
   * Usually a URI pointing to a term definition outside the system boundaries.
   * Optional.
   * @return
   */
  String getTerm();

  MultilingualTitle getMultilingualTitle();

}
