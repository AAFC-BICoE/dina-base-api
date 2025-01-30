package ca.gc.aafc.dina.validation;

import java.util.List;
import java.util.Optional;
import javax.inject.Named;

import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.vocabulary.VocabularyElementConfiguration;

public abstract class VocabularyBasedValidator <T> extends DinaBaseValidator<T> {

  public static final String KEY_NOT_FOUND = "vocabulary.keyNotFound";

  public VocabularyBasedValidator(Class<T> supportedClass, @Named("validationMessageSource") MessageSource messageSource) {
    super(supportedClass, messageSource);
  }

  /**
   * Checks if the provided value matches an entry in vocabularyElement list by comparing it to keys (ignore case).
   * @param value value to validate and standardize
   * @param fieldName used to report error
   * @param vocabularyElements valid elements for the vocabulary
   * @param errors stores the errors
   * @return standardized value of the provided value or the same value if an error occurred
   */
  protected String validateAndStandardizeValueAgainstVocabulary(String value, String fieldName, List<VocabularyElementConfiguration> vocabularyElements, Errors errors) {
    Optional<VocabularyElementConfiguration>
      foundVocabularyElement = findInVocabulary(value, vocabularyElements);
    if (foundVocabularyElement.isPresent()) {
      return foundVocabularyElement.get().getKey();
    } else {
      String errorMessage = getMessage(KEY_NOT_FOUND, value, fieldName);
      errors.rejectValue(fieldName, KEY_NOT_FOUND, errorMessage);
    }
    return value;
  }

  /**
   * Finds the first CollectionVocabularyElement where the key is matching (ignore case) the provided value.
   * @param key
   * @param vocabularyElements
   * @return
   */
  protected Optional<VocabularyElementConfiguration> findInVocabulary(String key, List<VocabularyElementConfiguration> vocabularyElements) {
    return vocabularyElements.stream().filter(o -> o.getKey().equalsIgnoreCase(key)).findFirst();
  }
}
