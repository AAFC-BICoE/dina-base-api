package ca.gc.aafc.dina.exceptionmapping;

import java.util.Collections;

import javax.inject.Named;
import javax.validation.ValidationException;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;
import io.crnk.core.engine.http.HttpStatus;

@Named
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {
  private static final Integer STATUS_ON_ERROR = HttpStatus.UNPROCESSABLE_ENTITY_422;

  @Override
  public ErrorResponse toErrorResponse(ValidationException exception) {
    return new ErrorResponse(
        Collections.singletonList(
            ErrorData.builder()
                .setStatus(STATUS_ON_ERROR.toString())
                .setTitle("Validation error")
                .setDetail(exception.getMessage())
                .build()
        ),
        STATUS_ON_ERROR
    );
  }

  @Override
  public ValidationException fromErrorResponse(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  @Override
  public boolean accepts(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }
}
