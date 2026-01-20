package ca.gc.aafc.dina.exceptionmapping;

import org.junit.jupiter.api.Test;

import com.toedter.spring.hateoas.jsonapi.JsonApiError;
import com.toedter.spring.hateoas.jsonapi.JsonApiErrors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;

public class IllegalStateExceptionMapperIT {

  private final JsonApiExceptionControllerAdvice exceptionControllerAdvice = new JsonApiExceptionControllerAdvice();

  @Test
  public void testIllegalStateException() {

    try {
      throw new IllegalStateException("this is an illegal state");
    } catch (IllegalStateException exception) {

      JsonApiErrors
        apiErrors = exceptionControllerAdvice.handleIllegalStateException(exception).getBody();
      // Assert correct http status.
      assertEquals("400", apiErrors.getErrors().getFirst().getCode());

      // Get the errors sorted by detail. The default error order is not consistent.
      List<JsonApiError> errors = apiErrors.getErrors()
        .stream()
        .sorted(Comparator.comparing(JsonApiError::getDetail))
        .toList();

      assertEquals(1, errors.size());
      // Assert correct error message, status and title
      assertEquals("this is an illegal state", errors.getFirst().getDetail());
      assertEquals("400", errors.getFirst().getCode());
      assertEquals("Bad Request", errors.getFirst().getTitle());
    }
  }
}
