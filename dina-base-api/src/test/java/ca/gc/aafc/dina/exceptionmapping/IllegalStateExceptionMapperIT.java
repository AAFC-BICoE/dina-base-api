package ca.gc.aafc.dina.exceptionmapping;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.BasePostgresItContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class IllegalStateExceptionMapperIT extends BasePostgresItContext {

  @Inject
  private IllegalStateExceptionMapper illegalStateExceptionMapper;

  @Test
  public void dtr() {

    ErrorResponse errorResponse = illegalStateExceptionMapper.toErrorResponse(new IllegalStateException("this is an illegal state"));

    // Assert correct http status.
    assertEquals(400, errorResponse.getHttpStatus());

    List<ErrorData> errors = new ArrayList<>(errorResponse.getErrors());

    assertEquals(1, errors.size());

    // Assert correct error message, status and title
    assertEquals("this is an illegal state", errors.get(0).getDetail());
    assertEquals("400", errors.get(0).getStatus());
    assertEquals("Bad Request", errors.get(0).getTitle());

  }

}
