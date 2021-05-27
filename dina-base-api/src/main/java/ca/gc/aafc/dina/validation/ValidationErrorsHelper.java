package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

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
   * Create a new Errors object with the DinaEntity as object name.
   *
   * @param entity
   * @param <E>
   */
  public static <E extends DinaEntity> Errors newErrorsObject(E entity) {
    return new BeanPropertyBindingResult(entity,
        entity.getUuid() != null ? entity.getUuid().toString() : "");
  }

  /**
   * Validate the provided Object with the provided Validator and Errors objects.
   * @param errors
   * @throws ValidationException
   */
  public static void errorsToValidationException(Errors errors) {

    if (errors == null || !errors.hasErrors()) {
      return;
    }

    Optional<String> errorMsg = errors.getAllErrors()
        .stream()
        .map(ObjectError::getDefaultMessage)
        .findAny();

    errorMsg.ifPresent(msg -> {
      throw new ValidationException(msg);
    });
  }

}
