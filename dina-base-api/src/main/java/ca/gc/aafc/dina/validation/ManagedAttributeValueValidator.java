package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttribute.ManagedAttributeType;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
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
import java.util.regex.Pattern;

public class ManagedAttributeValueValidator<E extends ManagedAttribute> implements Validator {

  private static final String MANAGED_ATTRIBUTE_INVALID_VALUE = "managedAttribute.value.invalid";
  private static final String MANAGED_ATTRIBUTE_INVALID_KEY = "managedAttribute.key.invalid";
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

  /**
   * See {@link #validate(DinaEntity, Map, ValidationContext)}
   * @param entity
   * @param managedAttributes
   * @param <D>
   */
  public <D extends DinaEntity> void validate(D entity, Map<String, String> managedAttributes) {
    validate(entity, managedAttributes, null);
  }
  
  /**
   * Validates the managedAttributes attached to the provided entity.
   * @param entity
   * @param managedAttributes
   * @param <D>
   * @throws javax.validation.ValidationException
   */
  public <D extends DinaEntity> void validate(D entity, Map<String, String> managedAttributes, ValidationContext validationContext) {
    Errors errors = ValidationErrorsHelper.newErrorsObject(entity);
    validateElements(managedAttributes, errors, validationContext);
    ValidationErrorsHelper.errorsToValidationException(errors);
  }

  private void validateElements(Map<String, String> attributesAndValues, Errors errors, ValidationContext validationContext) {
    Map<String, E> attributesPerKey = dinaService.findAttributesForKeys(attributesAndValues.keySet());

    Collection<?> difference = CollectionUtils.disjunction(attributesAndValues.keySet(), attributesPerKey.keySet());
    if (!difference.isEmpty()) {
      errors.reject(MANAGED_ATTRIBUTE_INVALID_KEY, getMessageForKey(MANAGED_ATTRIBUTE_INVALID_KEY, difference.stream().findFirst().get()));
    }

    attributesPerKey.forEach((key, ma) -> {
      ManagedAttributeType maType = ma.getManagedAttributeType();
      String assignedValue = attributesAndValues.get(key);

      if(preValidateValue(ma, assignedValue, errors, validationContext)) {
        if (maType == ManagedAttributeType.DATE && isNotValidDate(assignedValue)) {
          errors.reject(MANAGED_ATTRIBUTE_INVALID_VALUE,
            getMessageForKey(MANAGED_ATTRIBUTE_INVALID_VALUE, assignedValue, key));
        }

        if (maType == ManagedAttributeType.INTEGER && !INTEGER_PATTERN.matcher(assignedValue).matches()) {
          errors.reject(MANAGED_ATTRIBUTE_INVALID_VALUE,
              getMessageForKey(MANAGED_ATTRIBUTE_INVALID_VALUE, assignedValue, key));
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

  private String getMessageForKey(String key, Object... objects) {
    return messageSource.getMessage(key, objects, LocaleContextHolder.getLocale());
  }

  private static boolean isNotValidDate(String assignedValue) {
    try {
      LocalDate.parse(assignedValue);
    } catch (DateTimeParseException e) {
      return true;
    }
    return false;
  }

}
