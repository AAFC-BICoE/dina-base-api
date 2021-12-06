package ca.gc.aafc.dina.exceptionmapping;

import java.util.Collections;

import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;

import ca.gc.aafc.dina.exception.UnknownAttributeException;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;
import io.crnk.core.engine.http.HttpStatus;

/**
 * Maps io.crnk.core.engine.internal.utils.PropertyException to user-friendly errors to be
 * displayed in JSONAPI.
 */
@Named
public class UnknownAttributeExceptionMapper implements ExceptionMapper<UnknownAttributeException> {

  private static final Integer STATUS_ON_ERROR = HttpStatus.BAD_REQUEST_400;

  @Override
  public ErrorResponse toErrorResponse(UnknownAttributeException exception) {
    return new ErrorResponse(
      Collections.singletonList(
        ErrorData.builder()
          .setStatus(STATUS_ON_ERROR.toString())
          .setTitle("BAD_REQUEST")
          .setDetail(ExceptionUtils.throwableOfType(exception.getCause(), IllegalArgumentException.class).getMessage())
          .build()
      ),
      STATUS_ON_ERROR
    );
  }
  
  @Override
  public UnknownAttributeException fromErrorResponse(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }
  
  @Override
  public boolean accepts(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }
  
}
