package ca.gc.aafc.dina.validation;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.ControlledVocabularyItem;
import ca.gc.aafc.dina.service.ControlledVocabularyItemService;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.NonNull;

/**
 * Base validator to check controlled vocabulary items' keys and values.
 */
public abstract class BaseControlledVocabularyValueValidator<E extends ControlledVocabularyItem> {

  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  protected static final String CONTROLLED_VOCABULARY_INVALID_KEY = "controlledVocabulary.key.invalid";
  protected static final String CONTROLLED_VOCABULARY_ITEM_INVALID_KEY = "controlledVocabularyItem.key.invalid";
  protected static final String CONTROLLED_VOCABULARY_INVALID_VALUE = "controlledVocabulary.value.invalid";

  protected final ControlledVocabularyItemService<E> vocabItemService;
  protected final MessageSource messageSource;

  public BaseControlledVocabularyValueValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    @NonNull ControlledVocabularyItemService<E> vocabItemService
  ) {
    this.messageSource = messageSource;
    this.vocabItemService = vocabItemService;
  }

  protected void validateKey(String key, Supplier<E> supplyItem, Errors errors) {
    E vocabItem = supplyItem.get();
    if (vocabItem == null) {
      errors.reject(CONTROLLED_VOCABULARY_INVALID_KEY,
        getMessageForKey(CONTROLLED_VOCABULARY_INVALID_KEY, key));
      ValidationErrorsHelper.errorsToValidationException(errors);
    }
  }

  protected void validateItems(Map<String, String> keysAndValues, Supplier<List<E>> supplyItems, Errors errors) {
    Map<String, E> attributesPerKey = toMapByKey(supplyItems);

    Collection<?> difference = CollectionUtils.disjunction(keysAndValues.keySet(), attributesPerKey.keySet());
    if (!difference.isEmpty()) {
      errors.reject(CONTROLLED_VOCABULARY_INVALID_KEY,
          getMessageForKey(CONTROLLED_VOCABULARY_INVALID_KEY, difference.stream().findFirst().get()));
    }

    attributesPerKey.forEach((key, ma) -> {
      validateItem(ma, keysAndValues.get(key), errors);
    });
  }

  protected void validateItem(E itemDef, String assignedValue, Errors errors) {
    if (!isValidValue(itemDef.getVocabularyElementType(), assignedValue)) {
      rejectInvalidValue(errors, itemDef.getKey(), assignedValue);
    }

    String[] acceptedValues = itemDef.getAcceptedValues();
    // if acceptedValues is empty we skip the check
    if (ArrayUtils.isNotEmpty(acceptedValues) && !TypedVocabularyElementValidator.isAcceptedValue(assignedValue, acceptedValues)) {
      errors.reject(CONTROLLED_VOCABULARY_INVALID_VALUE, getMessageForKey(CONTROLLED_VOCABULARY_INVALID_VALUE, assignedValue, itemDef.getKey()));
    }
  }

  /**
   * Transform a concrete list of {@link ControlledVocabularyItem} into a map where the key is used
   * as the map's key.
   *
   * @param supplyItems supplier of a list of {@link ControlledVocabularyItem}
   *
   * @return
   */
  private Map<String, E> toMapByKey(Supplier<List<E>> supplyItems) {
    List<E> vocabItems = supplyItems.get();

    if (vocabItems.isEmpty()) {
      return Map.of();
    }

    return vocabItems.stream().collect(Collectors.toMap(
      ControlledVocabularyItem::getKey,
      vi -> vi
    ));
  }

  private void rejectInvalidValue(Errors errors, String key, String assignedValue) {
    errors.reject(CONTROLLED_VOCABULARY_INVALID_VALUE,
        getMessageForKey(CONTROLLED_VOCABULARY_INVALID_VALUE, assignedValue, key));
  }

  protected String getMessageForKey(String key, Object... objects) {
    return messageSource.getMessage(key, objects, LocaleContextHolder.getLocale());
  }

  // --- Static Validation methods ---

  static boolean isAcceptedValue(@NonNull String assignedValue, String[] acceptedValues) {
    return isAcceptedValue(assignedValue, acceptedValues, true);
  }

  static boolean isAcceptedValue(@NonNull String assignedValue, String[] acceptedValues,
                                        boolean ignoreCase) {
    return Arrays.stream(acceptedValues)
      .anyMatch(ignoreCase ? assignedValue::equalsIgnoreCase : assignedValue::equals);
  }

  /**
   * Checks if the assignedValue is valid for the {@link TypedVocabularyElement} in terms of type {@link TypedVocabularyElement.VocabularyElementType}.
   * Valid means the assignedValue can be represented/parsed in the given type.
   * Accepted values are NOT in scope for that method, {@link #isAcceptedValue(String, String[], boolean)} should be used.
   * @param tvType
   * @param assignedValue
   * @return
   */
  static boolean isValidValue(TypedVocabularyElement.VocabularyElementType tvType, String assignedValue) {
    switch(tvType) {
      case DATE :
        if (!isValidLocalDate(assignedValue)) {
          return false;
        }
        break;
      case INTEGER:
        if (!INTEGER_PATTERN.matcher(assignedValue).matches()) {
          return false;
        }
        break;
      case BOOL:
        if (!isValidBool(assignedValue)) {
          return false;
        }
        break;
      case DECIMAL:
        if (!NumberUtils.isParsable(assignedValue)) {
          return false;
        }
        break;
      case STRING:
        return true;
      default: // unknown type
        return false;
    }
    return true;
  }

  static boolean isValidLocalDate(String assignedValue) {
    try {
      LocalDate.parse(assignedValue);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  static boolean isValidBool(String assignedValue) {
    return BooleanUtils.TRUE.equals(assignedValue) || BooleanUtils.FALSE.equals(assignedValue);
  }
}
