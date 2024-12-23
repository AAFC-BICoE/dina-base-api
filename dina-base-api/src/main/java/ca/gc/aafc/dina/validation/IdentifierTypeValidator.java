package ca.gc.aafc.dina.validation;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.entity.IdentifierType;

public class IdentifierTypeValidator extends DinaBaseValidator<IdentifierType> {

  private static final String MISSING_PLACEHOLDER_KEY = "identifierType.uriTemplate.missingPlaceholder";

  public IdentifierTypeValidator(@Named("validationMessageSource") MessageSource messageSource) {
    super(IdentifierType.class, messageSource);
  }

  @Override
  public void validateTarget(IdentifierType target, Errors errors) {

    if(StringUtils.isNotBlank(target.getUriTemplate())) {
      if(!target.getUriTemplate().contains("$1")) {
        errors.reject(MISSING_PLACEHOLDER_KEY, getMessage(MISSING_PLACEHOLDER_KEY));
      }
    }
  }
}
