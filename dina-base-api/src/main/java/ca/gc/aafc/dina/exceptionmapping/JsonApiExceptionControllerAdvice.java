package ca.gc.aafc.dina.exceptionmapping;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.toedter.spring.hateoas.jsonapi.JsonApiError;
import com.toedter.spring.hateoas.jsonapi.JsonApiErrors;

import ca.gc.aafc.dina.exception.ResourceGoneException;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.exception.ResourcesGoneException;
import ca.gc.aafc.dina.exception.ResourcesNotFoundException;
import ca.gc.aafc.dina.exception.UnknownAttributeException;
import ca.gc.aafc.dina.jsonapi.JSONApiDocumentStructure;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;

/**
 * Exception handling for {@link DinaRepositoryV2}
 */
@RestControllerAdvice(assignableTypes = DinaRepositoryV2.class)
public class JsonApiExceptionControllerAdvice {

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleResourceNotFoundException(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
      JsonApiErrors.create().withError(
        JsonApiError.create()
          .withCode(Integer.toString(HttpStatus.NOT_FOUND.value()))
          .withStatus(HttpStatus.NOT_FOUND.toString())
          .withTitle("Not Found")
          .withDetail(ex.getMessage()))
    );
  }

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleResourceNotFoundException(ResourcesNotFoundException ex) {
    JsonApiErrors errors = JsonApiErrors.create();

    ex.getIdentifier()
      .stream()
      .map(docId -> JsonApiError.create()
        .withCode(Integer.toString(HttpStatus.NOT_FOUND.value()))
        .withStatus(HttpStatus.NOT_FOUND.toString())
        .withTitle("Not Found")
        .withSourcePointer(JSONApiDocumentStructure.pointerForDocumentId(docId).toString())
      )
      .forEach(errors::withError);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
  }

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleResourceGoneException(ResourceGoneException ex) {
    return ResponseEntity.status(HttpStatus.GONE).body(
      JsonApiErrors.create().withError(
        JsonApiError.create()
          .withCode(Integer.toString(HttpStatus.GONE.value()))
          .withStatus(HttpStatus.GONE.toString())
          .withTitle("Gone")
          .withDetail(ex.getMessage())
          .withAboutLink(ex.getLink()))
    );
  }

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleResourcesGoneException(ResourcesGoneException ex) {
    JsonApiErrors errors = JsonApiErrors.create();

    ex.getIdentifierLinks().entrySet()
      .stream()
      .map(idLinkPair -> JsonApiError.create()
        .withCode(Integer.toString(HttpStatus.GONE.value()))
        .withStatus(HttpStatus.GONE.toString())
        .withTitle("Gone")
        .withSourcePointer(JSONApiDocumentStructure.pointerForDocumentId(idLinkPair.getKey()).toString())
        .withAboutLink(idLinkPair.getValue())
      )
      .forEach(errors::withError);
    return ResponseEntity.status(HttpStatus.GONE).body(errors);
  }

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleUnknownAttributeException(UnknownAttributeException ex) {
    // unwrap the exception (if there is a known cause)
    return buildBadRequestResponse(ex.getCause() != null ? ex.getCause() : ex);
  }

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleIllegalArgumentException(IllegalArgumentException ex) {
    return buildBadRequestResponse(ex);
  }

  @ExceptionHandler
  public ResponseEntity<JsonApiErrors> handleInvalidDataAccessApiUsageException(InvalidDataAccessApiUsageException ex) {
    // unwrap the exception
    return buildBadRequestResponse(ex.getCause());
  }

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

  private static ResponseEntity<JsonApiErrors> buildBadRequestResponse(Throwable ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
      JsonApiErrors.create().withError(
        JsonApiError.create()
          .withCode(Integer.toString(HttpStatus.BAD_REQUEST.value()))
          .withStatus(HttpStatus.BAD_REQUEST.toString())
          .withTitle("Bad Request")
          .withDetail(ex.getMessage()))
    );
  }

  private static String generateDetail(ConstraintViolation<?> cv) {
    return String.join(" ", cv.getPropertyPath().toString(), cv.getMessage());
  }

  private static String generateSourcePointer(ConstraintViolation<?> cv) {
    return cv.getPropertyPath().toString();
  }
}
