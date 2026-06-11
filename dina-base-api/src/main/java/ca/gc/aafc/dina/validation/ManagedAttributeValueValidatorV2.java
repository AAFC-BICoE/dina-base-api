package ca.gc.aafc.dina.validation;

import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.ControlledVocabularyItem;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.ControlledVocabularyItemService;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import jakarta.inject.Named;
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
   * Checks if a {@link ControlledVocabularyItem} can be deleted.
   * The goal of this method is to check if the item is used.
   * @param controlledVocabularyItem
   * @return
   */
  public boolean canBeDeleted(E controlledVocabularyItem) {
    return false;
  }

  /**
   * Checks if this is  value validator is applicable to {@link ControlledVocabularyItem}.
   * <p>
   * Returns true if value validator and item both belong to the same controlled vocabulary
   * and have the same Dina component.
   *
   * @param controlledVocabularyItem the item to compare against
   * @return true if this value validator is applicable to the given item, false otherwise
   */
  public boolean isApplicableTo(E controlledVocabularyItem) {
    return getControlledVocabularyUuid().equals(
      controlledVocabularyItem.getControlledVocabulary().getUuid())
      && Objects.equals(getDinaComponent(), controlledVocabularyItem.getDinaComponent());
  }

  public <D extends DinaEntity> void validate(D entity, Map<String, String> managedAttributes) {
    validate(managedAttributes, ValidationErrorsHelper.newErrorsObject(entity));
  }

  public void validate(String objIdentifier, Object target, Map<String, String> managedAttributes) {
    Objects.requireNonNull(target);
    validate(managedAttributes, ValidationErrorsHelper.newErrorsObject(objIdentifier, target));
  }

  /**
   * Internal validate method that is throwing {@link jakarta.validation.ValidationException} if there is
   * any errors
   * @param managedAttributes
   * @param errors
   */
  private void validate(Map<String, String> managedAttributes, Errors errors) {
    validateItems(managedAttributes, ()->  vocabItemService.findAllByKeys(managedAttributes.keySet(), getControlledVocabularyUuid(), getDinaComponent()), errors);
    ValidationErrorsHelper.errorsToValidationException(errors);
  }
}
