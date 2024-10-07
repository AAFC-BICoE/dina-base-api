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

  protected String getMessage(String key, Object... objects) {
    return messageSource.getMessage(key, objects, LocaleContextHolder.getLocale());
  }
}
