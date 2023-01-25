package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.inject.Named;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class ManagedAttributeValueValidator<E extends ManagedAttribute> implements Validator {

  private static final String MANAGED_ATTRIBUTE_INVALID_VALUE = "managedAttribute.value.invalid";
  private static final String MANAGED_ATTRIBUTE_INVALID_KEY = "managedAttribute.key.invalid";
  private static final String MANAGED_ATTRIBUTE_INVALID_KEY_CONTEXT = "managedAttribute.keyContext.invalid";
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  private final ManagedAttributeService<E> dinaService;
  private final MessageSource messageSource;

  public ManagedAttributeValueValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    @NonNull ManagedAttributeService<E> dinaService
  ) {
    this.messageSource = messageSource;
    this.dinaService = dinaService;
  }

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return Map.class.isAssignableFrom(clazz);
  }

  @Override
  @SuppressWarnings("unchecked") // We check with checkIncomingParameter()
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    checkIncomingParameter(target);
    validateElements((Map<String, String>) target, errors, null);
  }

  @SuppressWarnings("unchecked") // We check with checkIncomingParameter()
  public void validate(@NonNull Object target, @NonNull Errors errors, ValidationContext context) {
    checkIncomingParameter(target);
    validateElements((Map<String, String>) target, errors, context);
  }

  /**
   * Same as {@link #validate(DinaEntity, Map, ValidationContext)} but without a {@link ValidationContext}.
   * @param entity
   * @param managedAttributes
   * @param <D>
   */
  public <D extends DinaEntity> void validate(D entity, Map<String, String> managedAttributes) {
    validate(entity, managedAttributes, null);
  }
  
  /**
   * Validates the managedAttributes attached to the provided entity using a {@link ValidationContext}.
   * @param entity
   * @param managedAttributes
   * @param validationContext will be used to call preValidateValue
   * @param <D>
   * @throws javax.validation.ValidationException
   */
  public <D extends DinaEntity> void validate(D entity, Map<String, String> managedAttributes, ValidationContext validationContext) {
    validate(managedAttributes, ValidationErrorsHelper.newErrorsObject(entity), validationContext);
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
   * Find the concrete {@link ManagedAttribute} mapping based on a set of keys.
   * By default {@link ValidationContext} is ignored but a subclass can override this function
   * to make use of it if the uniqueness of the managed attribute uses more columns than the key.
   *
   * @param keys
   * @param validationContext
   * @return
   */
  protected Map<String, E> findAttributesForValidation(Set<String> keys, ValidationContext validationContext) {
    return dinaService.findAttributesForKeys(keys);
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
      TypedVocabularyElement.VocabularyElementType maType = ma.getVocabularyElementType();
      String assignedValue = attributesAndValues.get(key);

      if(preValidateValue(ma, assignedValue, errors, validationContext)) {

        switch(maType) {
          case DATE :
            if (!isValidLocalDate(assignedValue)) {
              rejectInvalidValue(errors, key, assignedValue);
            }
            break;
          case INTEGER:
            if (!INTEGER_PATTERN.matcher(assignedValue).matches()) {
              rejectInvalidValue(errors, key, assignedValue);
            }
            break;
          case BOOL:
            if (!isValidBool(assignedValue)) {
              rejectInvalidValue(errors, key, assignedValue);
            }
            break;
          case DECIMAL:
            if(!NumberUtils.isParsable(assignedValue)) {
              rejectInvalidValue(errors, key, assignedValue);
            }
            break;
          default: //noop
        }

        String[] acceptedValues = ma.getAcceptedValues();
        if (isNotAnAcceptedValue(assignedValue, acceptedValues)) {
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

  private void checkIncomingParameter(Object target) {
    if (!supports(target.getClass())) {
      throw new IllegalArgumentException("this validator can only validate the type: " + Map.class.getSimpleName());
    }
    ((Map<?, ?>) target).forEach((o, o2) -> {
      if (!String.class.isAssignableFrom(o.getClass()) || !String.class.isAssignableFrom(o2.getClass())) {
        throw new IllegalArgumentException(
          "This validator can only validate maps with keys and values as strings");
      }
    });
  }

  private static boolean isNotAnAcceptedValue(@NonNull String assignedValue, String[] acceptedValues) {
    return ArrayUtils.isNotEmpty(acceptedValues) && Arrays.stream(acceptedValues)
      .noneMatch(assignedValue::equalsIgnoreCase);
  }

  private void rejectInvalidValue(Errors errors, String key, String assignedValue) {
    errors.reject(MANAGED_ATTRIBUTE_INVALID_VALUE,
        getMessageForKey(MANAGED_ATTRIBUTE_INVALID_VALUE, assignedValue, key));
  }

  private String getMessageForKey(String key, Object... objects) {
    return messageSource.getMessage(key, objects, LocaleContextHolder.getLocale());
  }

  private static boolean isValidLocalDate(String assignedValue) {
    try {
      LocalDate.parse(assignedValue);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  private static boolean isValidBool(String assignedValue) {
    return BooleanUtils.TRUE.equals(assignedValue) || BooleanUtils.FALSE.equals(assignedValue);
  }

}
