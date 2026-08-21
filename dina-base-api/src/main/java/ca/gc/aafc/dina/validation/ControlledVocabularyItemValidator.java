package ca.gc.aafc.dina.validation;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.entity.ControlledVocabularyItem;
import jakarta.inject.Named;

public class ControlledVocabularyItemValidator extends DinaBaseValidator<ControlledVocabularyItem> {

  private static final String ACCEPTED_VALUE_CONTROLLED_TERM_KEY = "controlledVocabulary.acceptedValueOnControlledTerm";
  private static final String MISSING_PLACEHOLDER_KEY = "controlledVocabulary.uriTemplate.missingPlaceholder";
  private static final String EMPTY_DESCRIPTION_KEY = "controlledVocabulary.emptyDescription";

  public ControlledVocabularyItemValidator(@Named("validationMessageSource") MessageSource messageSource) {
    super(ControlledVocabularyItem.class, messageSource);
  }

  @Override
  public void validateTarget(ControlledVocabularyItem target, Errors errors) {
    // CONTROLLED_TERM should have no value so no accepted values
    if (ControlledVocabulary.ControlledVocabularyClass.CONTROLLED_TERM ==
      (target.getControlledVocabulary() != null ? target.getControlledVocabulary().getVocabClass() :
        null)) {
      if (target.getAcceptedValues() != null && target.getAcceptedValues().length > 0) {
        errors.reject(ACCEPTED_VALUE_CONTROLLED_TERM_KEY,
          getMessage(ACCEPTED_VALUE_CONTROLLED_TERM_KEY));
      }
    }
    validateDescription(target, errors);
    validateUriTemplate(target, errors);
  }

  public void validateDescription(ControlledVocabularyItem target, Errors errors) {

    if (target.getMultilingualDescription() == null) {
      return;
    }

    if (CollectionUtils.isEmpty(target.getMultilingualDescription().getDescriptions()) ||
        target.getMultilingualDescription().hasBlankDescription()) {
      String errorMessage = getMessage(EMPTY_DESCRIPTION_KEY, null,
        LocaleContextHolder.getLocale());
      errors.rejectValue("multilingualDescription", "description.isEmpty", null, errorMessage);
    }
  }

  public void validateUriTemplate(ControlledVocabularyItem target, Errors errors) {
    if (StringUtils.isNotBlank(target.getUriTemplate())) {
      if (!target.getUriTemplate().contains("$1")) {
        errors.reject(MISSING_PLACEHOLDER_KEY, getMessage(MISSING_PLACEHOLDER_KEY));
      }
    }
  }
}
