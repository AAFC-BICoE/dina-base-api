package ca.gc.aafc.dina.validation;

import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.ControlledVocabularyItem;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.ControlledVocabularyItemService;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Named;
import lombok.NonNull;

/**
 * Validator tailored to validate managed attributes.
 * Always working on a single vocabulary representing the managed attribute.
 */
public abstract class ManagedAttributeValueValidatorV2<E extends ControlledVocabularyItem> extends BaseControlledVocabularyValueValidator<E> {

  public ManagedAttributeValueValidatorV2(
    @Named("validationMessageSource") MessageSource messageSource,
    @NonNull ControlledVocabularyItemService<E> vocabItemService
  ) {
    super(messageSource, vocabItemService);
  }

  /**
   * UUID of the ControlledVocabulary representing managed attribute.
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
   * @param managedAttributes
   * @param <D>
   */
  public <D extends DinaEntity> void validate(D entity, Map<String, String> managedAttributes) {
  //  validate(entity, keyAndAssignedValue, null);
    validate(managedAttributes, ValidationErrorsHelper.newErrorsObject(entity));
  }

  /**
   * Validates the managedAttributes attached to the provided object using a {@link ValidationContext}.
   * @param objIdentifier an identifier used in the error message to identify the target
   * @param target
   * @param managedAttributes
   * @param validationContext will be used to call preValidateValue
   * @throws javax.validation.ValidationException
   */
  public void validate(String objIdentifier, Object target, Map<String, String> managedAttributes) {
    Objects.requireNonNull(target);
    Errors errors = ValidationErrorsHelper.newErrorsObject(objIdentifier, target);
    validate(managedAttributes, errors);
  }

  /**
   * Internal validate method that is using {@link ValidationContext}
   * @param managedAttributes
   * @param errors
   * @param validationContext
   */
  private void validate(Map<String, String> managedAttributes, Errors errors) {
    validateItems(managedAttributes, ()->  vocabItemService.findAllByKeys(managedAttributes.keySet(), getControlledVocabularyUuid(), getDinaComponent()), errors);
    ValidationErrorsHelper.errorsToValidationException(errors);
  }

  /**
   * Override this method to add additional validation before a value is validated for a specific managed attribute.
   * @param managedAttributeDefinition
   * @param value
   * @param errors
   * @param validationContext optional, can be null. The ValidationContext is simply the one provided to validate method
   * @return true if the validation of the value should proceed or false if it should not since there is already an error
   */
  protected boolean preValidateValue(E managedAttributeDefinition, String value, Errors errors) {
    return true;
  }

}
