package ca.gc.aafc.dina.exceptionmapping;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;
import io.crnk.core.engine.http.HttpStatus;
import java.util.Collections;
import javax.inject.Named;

@Named
public class IllegalStateExceptionMapper implements ExceptionMapper<IllegalStateException> {

  private static final Integer STATUS_ON_ERROR = HttpStatus.BAD_REQUEST_400;

  @Override
  public ErrorResponse toErrorResponse(IllegalStateException exception) {
    return new ErrorResponse(
      Collections.singletonList(
        ErrorData.builder()
          .setStatus(STATUS_ON_ERROR.toString())
          .setTitle("BAD_REQUEST")
          .setDetail(exception.getMessage())
          .build()
      ),
      STATUS_ON_ERROR
    );
  }

  @Override
  public IllegalStateException fromErrorResponse(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  @Override
  public boolean accepts(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }
}
