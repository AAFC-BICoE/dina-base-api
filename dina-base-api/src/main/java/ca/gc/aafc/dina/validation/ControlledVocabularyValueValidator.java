package ca.gc.aafc.dina.validation;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.ControlledVocabularyItem;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.ControlledVocabularyItemService;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.NonNull;

public abstract class ControlledVocabularyValueValidator<E extends ControlledVocabularyItem> {

  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  private static final String MANAGED_ATTRIBUTE_INVALID_VALUE = "managedAttribute.value.invalid";
  private static final String MANAGED_ATTRIBUTE_INVALID_KEY = "managedAttribute.key.invalid";
  private static final String MANAGED_ATTRIBUTE_INVALID_KEY_CONTEXT = "managedAttribute.keyContext.invalid";

  private final ControlledVocabularyItemService<E> vocabItemService;
  private final MessageSource messageSource;

  public ControlledVocabularyValueValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    @NonNull ControlledVocabularyItemService<E> vocabItemService
  ) {
    this.messageSource = messageSource;
    this.vocabItemService = vocabItemService;
  }

  /**
   * UUID of the ControlledVocabulary this validator is validating items for.
   * @return
   */
  public abstract UUID getControlledVocabularyUuid();

  /**
   * Optional, dinaComponent restriction to scope ControlledVocabularyItem.
   * @return
   */
  public abstract String getDinaComponent();

  /**
   * Same as {@link #validate(DinaEntity, Map, ValidationContext)} but without a {@link ValidationContext}.
   * @param entity
   * @param keyAndAssignedValue
   * @param <D>
   */
  public <D extends DinaEntity> void validate(D entity, Map<String, String> keyAndAssignedValue) {
    validate(entity, keyAndAssignedValue, null);
  }
  
  /**
   * Validates the managedAttributes attached to the provided entity using a {@link ValidationContext}.
   * @param entity
   * @param keyAndAssignedValue
   * @param validationContext will be used to call preValidateValue
   * @param <D>
   * @throws javax.validation.ValidationException
   */
  public <D extends DinaEntity> void validate(D entity, Map<String, String> keyAndAssignedValue, ValidationContext validationContext) {
    validate(keyAndAssignedValue, ValidationErrorsHelper.newErrorsObject(entity), validationContext);
  }

  /**
   * Validates the managedAttributes attached to the provided object using a {@link ValidationContext}.
   * @param objIdentifier an identifier used in the error message to identify the target
   * @param target
   * @param managedAttributes
   * @param validationContext will be used to call preValidateValue
   * @throws javax.validation.ValidationException
   */
  public void validate(String objIdentifier, Object target, Map<String, String> managedAttributes, ValidationContext validationContext) {
    Objects.requireNonNull(target);
    Errors errors = ValidationErrorsHelper.newErrorsObject(objIdentifier, target);
    validate(managedAttributes, errors, validationContext);
  }

  /**
   * Internal validate method that is using {@link ValidationContext}
   * @param managedAttributes
   * @param errors
   * @param validationContext
   */
  private void validate(Map<String, String> managedAttributes, Errors errors, ValidationContext validationContext) {
    validateElements(managedAttributes, errors, validationContext);
    ValidationErrorsHelper.errorsToValidationException(errors);
  }

  /**
   * Find the concrete {@link ControlledVocabularyItem} based on a set of keys.
   * By default {@link ValidationContext} is ignored but a subclass can override this function
   * to make use of it if the uniqueness of the managed attribute uses more columns than the key.
   *
   * @param keys
   * @param validationContext
   * @return
   */
  protected Map<String, E> findAttributesForValidation(Set<String> keys, ValidationContext validationContext) {
    List<E> vocabItems = vocabItemService.findAllByKeys(keys, getControlledVocabularyUuid(), getDinaComponent());

    if(vocabItems.isEmpty()) {
      return Map.of();
    }

    return vocabItems.stream().collect(Collectors.toMap(
      ControlledVocabularyItem::getKey,
      vi -> vi
    ));
  }

  private void validateElements(Map<String, String> attributesAndValues, Errors errors, ValidationContext validationContext) {
    Map<String, E> attributesPerKey = findAttributesForValidation(attributesAndValues.keySet(), validationContext);

    Collection<?> difference = CollectionUtils.disjunction(attributesAndValues.keySet(), attributesPerKey.keySet());
    if (!difference.isEmpty()) {
      if (validationContext == null) {
        errors.reject(MANAGED_ATTRIBUTE_INVALID_KEY,
            getMessageForKey(MANAGED_ATTRIBUTE_INVALID_KEY, difference.stream().findFirst().get()));
      } else {
        errors.reject(MANAGED_ATTRIBUTE_INVALID_KEY_CONTEXT,
            getMessageForKey(MANAGED_ATTRIBUTE_INVALID_KEY_CONTEXT,
                difference.stream().findFirst().get(), validationContext.toString()));
      }
    }

    attributesPerKey.forEach((key, ma) -> {
      String assignedValue = attributesAndValues.get(key);

      if (preValidateValue(ma, assignedValue, errors, validationContext)) {

        if (!isValidValue(ma.getVocabularyElementType(), assignedValue)) {
          rejectInvalidValue(errors, key, assignedValue);
        }

        String[] acceptedValues = ma.getAcceptedValues();
        // if acceptedValues is empty we skip the check
        if (ArrayUtils.isNotEmpty(acceptedValues) && !TypedVocabularyElementValidator.isAcceptedValue(assignedValue, acceptedValues)) {
          errors.reject(MANAGED_ATTRIBUTE_INVALID_VALUE, getMessageForKey(MANAGED_ATTRIBUTE_INVALID_VALUE, assignedValue, key));
        }
      }
    });
  }

  /**
   * Override this method to add additional validation before a value is validated for a specific managed attribute.
   * @param managedAttributeDefinition
   * @param value
   * @param errors
   * @param validationContext optional, can be null. The ValidationContext is simply the one provided to validate method
   * @return true if the validation of the value should proceed or false if it should not since there is already an error
   */
  protected boolean preValidateValue(E managedAttributeDefinition, String value, Errors errors, ValidationContext validationContext) {
    return true;
  }

  private void rejectInvalidValue(Errors errors, String key, String assignedValue) {
    errors.reject(MANAGED_ATTRIBUTE_INVALID_VALUE,
        getMessageForKey(MANAGED_ATTRIBUTE_INVALID_VALUE, assignedValue, key));
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
