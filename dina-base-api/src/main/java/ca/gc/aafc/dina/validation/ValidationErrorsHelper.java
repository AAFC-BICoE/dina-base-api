package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import javax.validation.ValidationException;
import java.util.Optional;

/**
 * Collections of helper methods to work with validation errors and exceptions.
 */
public final class ValidationErrorsHelper {

  /**
   * Utility class
   */
  private ValidationErrorsHelper() {
  }

  /**
   * Create a new {@link Errors} object with the {@link DinaEntity} as object name.
   *
   * @param entity
   * @param <E>
   */
  public static <E extends DinaEntity> Errors newErrorsObject(E entity) {
    return newErrorsObject(
        entity.getUuid() != null ? entity.getUuid().toString() : "", entity);
  }

  /**
   * Create a new {@link Errors} object with a specific object name.
   *
   * @param identifier
   * @param target
   * @return
   */
  public static Errors newErrorsObject(String identifier, Object target) {
    return new BeanPropertyBindingResult(target, identifier);
  }

  /**
   * Check the provided Errors object and throw a {@link ValidationException} if there is at least
   * one error in the object. The exception will only contain 1 error in the exception even if the object
   * provides more than 1.
   * @param errors
   * @throws ValidationException
   */
  public static void errorsToValidationException(Errors errors) {

    if (errors == null || !errors.hasErrors()) {
      return;
    }

    Optional<String> errorMsg = errors.getAllErrors()
        .stream()
        .map(err -> StringUtils.defaultIfBlank(err.getDefaultMessage(), err.toString()))
        .findAny();

    errorMsg.ifPresent(msg -> {
      throw new ValidationException(msg);
    });
  }

}
