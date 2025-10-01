package ca.gc.aafc.dina.validation;

import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.entity.ControlledVocabularyItem;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.service.ControlledVocabularyItemService;
import ca.gc.aafc.dina.service.ControlledVocabularyService;

import javax.inject.Named;
import lombok.NonNull;

import static ca.gc.aafc.dina.entity.ControlledVocabulary.ControlledVocabularyClass.CONTROLLED_TERM;

/**
 * Validator tailored to validate keys of {@link ControlledVocabularyItem} when the vocabulary class is CONTROLLED_TERM
 */
public abstract class ControlledTermValueValidator<T extends ControlledVocabulary, E extends ControlledVocabularyItem> extends BaseControlledVocabularyValueValidator<E> {

  private final ControlledVocabularyService<T> vocabService;

  public ControlledTermValueValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    ControlledVocabularyService<T> vocabService,
    @NonNull ControlledVocabularyItemService<E> vocabItemService
  ) {
    super(messageSource, vocabItemService);
    this.vocabService = vocabService;
  }

  public <D extends DinaEntity> String validateAndStandardize(D entity, String vocabKey, String vocabItemKey) {

    T vocab = vocabService.findOneByKey(vocabKey);
    if (vocab == null || CONTROLLED_TERM != vocab.getVocabClass()) {
      Errors errors = ValidationErrorsHelper.newErrorsObject(entity);
      errors.reject(CONTROLLED_VOCABULARY_INVALID_KEY,
        getMessageForKey(CONTROLLED_VOCABULARY_INVALID_KEY, vocabKey));
      ValidationErrorsHelper.errorsToValidationException(errors);
    }

    return validateKeyAndStandardize(entity, vocabItemKey, () -> vocabItemService.findOneByKey(vocabItemKey.toLowerCase(), vocab.getUuid(), null));
  }
}
