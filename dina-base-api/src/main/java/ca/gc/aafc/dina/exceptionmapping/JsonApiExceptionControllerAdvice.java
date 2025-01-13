package ca.gc.aafc.dina.exceptionmapping;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.toedter.spring.hateoas.jsonapi.JsonApiError;
import com.toedter.spring.hateoas.jsonapi.JsonApiErrors;

import ca.gc.aafc.dina.repository.DinaRepositoryV2;

/**
 * Exception handling for {@link DinaRepositoryV2}
 */
@RestControllerAdvice(assignableTypes = DinaRepositoryV2.class)
public class JsonApiExceptionControllerAdvice {

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleValidationException(ValidationException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
      JsonApiErrors.create().withError(
        JsonApiError.create()
          .withCode(Integer.toString(HttpStatus.UNPROCESSABLE_ENTITY.value()))
          .withStatus(HttpStatus.UNPROCESSABLE_ENTITY.toString())
          .withTitle("Validation error")
          .withDetail(ex.getMessage()))
    );
  }

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleConstraintViolationException(ConstraintViolationException ex) {
    JsonApiErrors errors = JsonApiErrors.create();
    ex.getConstraintViolations()
      .stream()
      .map(cv -> JsonApiError.create()
        .withCode(Integer.toString(HttpStatus.UNPROCESSABLE_ENTITY.value()))
        .withStatus(HttpStatus.UNPROCESSABLE_ENTITY.toString())
        .withTitle("Constraint violation")
        .withDetail(generateDetail(cv))
        .withSourcePointer(generateSourcePointer(cv))
      )
      .forEach(errors::withError);

    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
      errors);
  }

  private static String generateDetail(ConstraintViolation<?> cv) {
    return String.join(" ", cv.getPropertyPath().toString(), cv.getMessage());
  }

  private static String generateSourcePointer(ConstraintViolation<?> cv) {
    return cv.getPropertyPath().toString();
  }
}
