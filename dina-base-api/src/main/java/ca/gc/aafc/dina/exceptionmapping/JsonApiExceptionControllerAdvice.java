package ca.gc.aafc.dina.exceptionmapping;

import javax.validation.ValidationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.toedter.spring.hateoas.jsonapi.JsonApiError;

import ca.gc.aafc.dina.repository.DinaRepositoryV2;

/**
 * Exception handling for {@link DinaRepositoryV2}
 */
@RestControllerAdvice(assignableTypes = {DinaRepositoryV2.class})
public class JsonApiExceptionControllerAdvice {

  @ExceptionHandler
  public ResponseEntity<JsonApiError> handleValidationException(ValidationException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
      JsonApiError.create()
        .withCode(HttpStatus.UNPROCESSABLE_ENTITY.toString())
        .withTitle("Validation error")
        .withDetail(ex.getMessage())
        .withStatus(HttpStatus.UNPROCESSABLE_ENTITY.toString())
    );
  }
}
