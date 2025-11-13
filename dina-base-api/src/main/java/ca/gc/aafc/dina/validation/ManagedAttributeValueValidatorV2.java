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


  public <D extends DinaEntity> void validate(D entity, Map<String, String> managedAttributes) {
    validate(managedAttributes, ValidationErrorsHelper.newErrorsObject(entity));
  }

  public void validate(String objIdentifier, Object target, Map<String, String> managedAttributes) {
    Objects.requireNonNull(target);
    validate(managedAttributes, ValidationErrorsHelper.newErrorsObject(objIdentifier, target));
  }

  /**
   * Internal validate method that is throwing {@link javax.validation.ValidationException} if there is
   * any errors
   * @param managedAttributes
   * @param errors
   */
  private void validate(Map<String, String> managedAttributes, Errors errors) {
    validateItems(managedAttributes, ()->  vocabItemService.findAllByKeys(managedAttributes.keySet(), getControlledVocabularyUuid(), getDinaComponent()), errors);
    ValidationErrorsHelper.errorsToValidationException(errors);
  }
}
