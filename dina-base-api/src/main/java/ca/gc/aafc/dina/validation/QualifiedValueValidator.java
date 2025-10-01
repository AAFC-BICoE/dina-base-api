package ca.gc.aafc.dina.validation;

import javax.inject.Named;
import lombok.NonNull;

import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.entity.ControlledVocabularyItem;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.ControlledVocabularyItemService;
import ca.gc.aafc.dina.service.ControlledVocabularyService;

import static ca.gc.aafc.dina.entity.ControlledVocabulary.ControlledVocabularyClass.QUALIFIED_VALUE;

/**
 * Validator tailored to validate keys and values of {@link ControlledVocabularyItem} when the vocabulary class is QUALIFIED_VALUE
 */
public class QualifiedValueValidator<T extends ControlledVocabulary, E extends ControlledVocabularyItem> extends BaseControlledVocabularyValueValidator<E> {

  private final ControlledVocabularyService<T> vocabService;

  public QualifiedValueValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    ControlledVocabularyService<T> vocabService,
    @NonNull ControlledVocabularyItemService<E> vocabItemService
  ) {
    super(messageSource, vocabItemService);
    this.vocabService = vocabService;
  }

  public <D extends DinaEntity> void validate(D entity, String vocabKey, String vocabItemKey, String value) {
    T vocab = vocabService.findOneByKey(vocabKey);

    if (vocab == null || QUALIFIED_VALUE != vocab.getVocabClass()) {
      Errors errors = ValidationErrorsHelper.newErrorsObject(entity);
      errors.reject(CONTROLLED_VOCABULARY_INVALID_KEY,
        getMessageForKey(CONTROLLED_VOCABULARY_INVALID_KEY, vocabKey));
      ValidationErrorsHelper.errorsToValidationException(errors);
    }

    E vocabItem = vocabItemService.findOneByKey(vocabItemKey, vocab.getUuid(), null);

    Errors errors = ValidationErrorsHelper.newErrorsObject(entity);
    validateItem(vocabItem, value, errors);

  }
}
