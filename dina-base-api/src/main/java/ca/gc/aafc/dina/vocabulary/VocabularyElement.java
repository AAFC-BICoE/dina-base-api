package ca.gc.aafc.dina.vocabulary;

import ca.gc.aafc.dina.i18n.MultilingualTitle;

/**
 * Define the main interface of a vocabulary element in its simplest form.
 */
public interface VocabularyElement {

  String getName();

  String getTerm();

  MultilingualTitle getMultilingualTitle();

}
