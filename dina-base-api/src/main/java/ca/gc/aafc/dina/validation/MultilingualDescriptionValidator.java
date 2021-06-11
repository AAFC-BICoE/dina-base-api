package ca.gc.aafc.dina.validation;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ca.gc.aafc.dina.SupportedLanguagesConfiguration;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import lombok.NonNull;

public class MultilingualDescriptionValidator implements Validator {

  private static final String UNSUPPORT_LANGUAGE = "managedAttribute.value.invalid";

  private final SupportedLanguagesConfiguration supportedLanguagesConfiguration;
  private final MessageSource messageSource;

  public MultilingualDescriptionValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    @NonNull SupportedLanguagesConfiguration supportedLanguagesConfiguration
  ) {
    this.messageSource = messageSource;
    this.supportedLanguagesConfiguration = supportedLanguagesConfiguration;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return MultilingualDescription.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    MultilingualDescription multilingualDescription = (MultilingualDescription) target;
    List<String> supportedLanguages = supportedLanguagesConfiguration.getSupportedLanguages();
    for (Map<String, String> map : multilingualDescription.getMultilingualPair()) {
      if (!supportedLanguages.contains(map.get("lang"))) {
        errors.reject(UNSUPPORT_LANGUAGE, messageSource.getMessage(UNSUPPORT_LANGUAGE, null, LocaleContextHolder.getLocale()));
      }
    }    
  }
  
}
