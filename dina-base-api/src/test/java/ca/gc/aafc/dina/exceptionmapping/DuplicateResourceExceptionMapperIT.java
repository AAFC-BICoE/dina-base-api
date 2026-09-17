package ca.gc.aafc.dina.exceptionmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.toedter.spring.hateoas.jsonapi.JsonApiError;
import com.toedter.spring.hateoas.jsonapi.JsonApiErrors;

import ca.gc.aafc.dina.exception.DuplicateResourceException;

public class DuplicateResourceExceptionMapperIT {

  private final JsonApiExceptionControllerAdvice exceptionControllerAdvice = new JsonApiExceptionControllerAdvice();

  @Test
  public void testIllegalStateException() {

    try {
      throw DuplicateResourceException.create("person", "123", "name");
    } catch (DuplicateResourceException exception) {

      JsonApiErrors
        apiErrors = exceptionControllerAdvice.handleDuplicateResourceException(exception).getBody();
      // Assert correct http status.
      assertEquals("422", apiErrors.getErrors().getFirst().getStatus());
      assertEquals("duplicate_resource", apiErrors.getErrors().getFirst().getCode());

      // Get the errors sorted by detail. The default error order is not consistent.
      List<JsonApiError> errors = apiErrors.getErrors()
        .stream()
        .sorted(Comparator.comparing(JsonApiError::getDetail))
        .toList();

      assertEquals(1, errors.size());
      // Assert correct error message, status and title
      assertEquals("Duplicate person detected. Existing resource ID: 123", errors.getFirst().getDetail());
    }
  }
}
