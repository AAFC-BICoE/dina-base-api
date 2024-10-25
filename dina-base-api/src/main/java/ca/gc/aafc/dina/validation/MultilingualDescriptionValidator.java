package ca.gc.aafc.dina.validation;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.SupportedLanguagesConfiguration;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualDescription.MultilingualPair;

import java.util.List;
import javax.inject.Named;
import lombok.NonNull;

@Component
public class MultilingualDescriptionValidator extends DinaBaseValidator<MultilingualDescription> {

  private static final String UNSUPPORT_LANGUAGE = "multilingual.description.unsupported";

  private final SupportedLanguagesConfiguration supportedLanguagesConfiguration;

  public MultilingualDescriptionValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    @NonNull SupportedLanguagesConfiguration supportedLanguagesConfiguration
  ) {
    super(MultilingualDescription.class, messageSource);
    this.supportedLanguagesConfiguration = supportedLanguagesConfiguration;
  }

  @Override
  public void validateTarget(MultilingualDescription target, Errors errors) {
    List<String> supportedLanguages = supportedLanguagesConfiguration.getSupportedLanguages();
    for (MultilingualPair multilingualPair : target.getDescriptions()) {
      if (!supportedLanguages.contains(multilingualPair.getLang())) {
        errors.reject(UNSUPPORT_LANGUAGE, getMessage(UNSUPPORT_LANGUAGE, multilingualPair.getLang()));
      }
    }
  }
}
