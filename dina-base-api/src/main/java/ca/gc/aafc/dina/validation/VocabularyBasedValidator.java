package ca.gc.aafc.dina.validation;

import java.util.List;
import java.util.Optional;
import javax.inject.Named;

import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.vocabulary.VocabularyElementConfiguration;

public abstract class VocabularyBasedValidator<T> extends DinaBaseValidator<T> {

  public static final String KEY_NOT_FOUND = "vocabulary.keyNotFound";

  public VocabularyBasedValidator(Class<T> supportedClass,
      @Named("validationMessageSource") MessageSource messageSource) {
    super(supportedClass, messageSource);
  }

  /**
   * Checks if the provided value matches an entry in vocabularyElement list by
   * comparing it to keys (ignore case).
   * 
   * @param value              value to validate and standardize
   * @param fieldName          used to report error
   * @param vocabularyElements valid elements for the vocabulary
   * @param errors             stores the errors
   * @return standardized value of the provided value or the same value if an
   *         error occurred
   */
  protected String validateAndStandardizeValueAgainstVocabulary(String value, String fieldName,
      List<? extends VocabularyElementConfiguration> vocabularyElements, Errors errors) {
    Optional<? extends VocabularyElementConfiguration> foundVocabularyElement = findInVocabulary(value, vocabularyElements);
    if (foundVocabularyElement.isPresent()) {
      return foundVocabularyElement.get().getKey();
    } else {
      String errorMessage = getMessage(KEY_NOT_FOUND, value, fieldName);
      errors.rejectValue(fieldName, KEY_NOT_FOUND, errorMessage);
    }
    return value;
  }

  /**
   * Checks that each value are in the vocabulary (matching the key,
   * case-sensitive)
   * Only one error will be reported even if multiple values are not matching.
   * 
   * @param values
   * @param fieldName
   * @param vocabularyElements
   * @param errors
   */
  protected void validateValuesAgainstVocabulary(List<String> values, String fieldName,
      List<? extends VocabularyElementConfiguration> vocabularyElements,
      Errors errors) {
    for (String value : values) {
      if (!isInVocabulary(value, vocabularyElements)) {
        String errorMessage = getMessage(KEY_NOT_FOUND, value, fieldName);
        errors.rejectValue(fieldName, KEY_NOT_FOUND, errorMessage);
        return;
      }
    }
  }

  /**
   * Checks if the provided key (case-sensitive) is in the vocabulary.
   * 
   * @param key                key to check
   * @param vocabularyElements
   * @return key is in the vocabulary or not
   */
  protected boolean isInVocabulary(String key, List<? extends VocabularyElementConfiguration> vocabularyElements) {
    for (VocabularyElementConfiguration el : vocabularyElements) {
      if (el.getKey().equals(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Finds the first CollectionVocabularyElement where the key is matching (ignore
   * case) the provided value.
   * 
   * @param key
   * @param vocabularyElements
   * @return
   */
  protected Optional<? extends VocabularyElementConfiguration> findInVocabulary(String key,
      List<? extends VocabularyElementConfiguration> vocabularyElements) {
    return vocabularyElements.stream().filter(o -> o.getKey().equalsIgnoreCase(key)).findFirst();
  }
}
