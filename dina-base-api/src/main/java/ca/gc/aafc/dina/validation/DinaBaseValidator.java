package ca.gc.aafc.dina.validation;

import lombok.NonNull;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Base class for validation.
 * @param <E>
 */
public abstract class DinaBaseValidator<E> implements Validator {

  private final Class<E> supportedClass;
  private final MessageSource messageSource;

  public DinaBaseValidator(Class<E> supportedClass, MessageSource messageSource) {
    this.supportedClass = supportedClass;
    this.messageSource = messageSource;
  }

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return supportedClass.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    checkIncomingParameter(target);
    validateTarget(supportedClass.cast(target), errors);
  }

  public abstract void validateTarget(E target, Errors errors);

  protected void checkIncomingParameter(Object target) {
    if (!supports(target.getClass())) {
      throw new IllegalArgumentException(
        "This validator can only validate the type: " + supportedClass.getSimpleName());
    }
  }

  /**
   * Get a parametrized message based on the current Locale.
   *
   * @param key  key of the message
   * @param args arguments to format the message
   * @return the message in the current Locale or the EN version as fallback
   */
  protected String getMessage(String key, Object... args) {
    return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
  }

  /**
   * Get a message based on the current Locale.
   *
   * @param key  key of the message
   * @return the message in the current Locale or the EN version as fallback
   */
  protected String getMessage(String key) {
    return getMessage(key, (Object) null);
  }
}
