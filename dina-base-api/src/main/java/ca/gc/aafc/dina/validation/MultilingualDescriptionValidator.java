package ca.gc.aafc.dina.validation;

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ca.gc.aafc.dina.SupportedLanguagesConfiguration;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualDescription.MultilingualPair;
import lombok.NonNull;

@Component
public class MultilingualDescriptionValidator implements Validator {

  private static final String UNSUPPORT_LANGUAGE = "multilingual.description.unsupported";

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
    checkIncomingParameter(target);
    MultilingualDescription multilingualDescription = (MultilingualDescription) target;
    List<String> supportedLanguages = supportedLanguagesConfiguration.getSupportedLanguages();
    for (MultilingualPair multilingualPair : multilingualDescription.getDescriptions()) {
      if (!supportedLanguages.contains(multilingualPair.getLang())) {
        errors.reject(UNSUPPORT_LANGUAGE, messageSource.getMessage(UNSUPPORT_LANGUAGE, new Object[] {multilingualPair.getLang()}, LocaleContextHolder.getLocale()));
      }
    }    
  }

  private void checkIncomingParameter(Object target) {
    if (!supports(target.getClass())) {
      throw new IllegalArgumentException("this validator can only validate the type: " + Map.class.getSimpleName());
    }

  }

}
