package ca.gc.aafc.dina.exceptionmapping;

import java.util.Collections;

import javax.inject.Named;

import org.springframework.dao.DataIntegrityViolationException;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;
import io.crnk.core.engine.http.HttpStatus;

/**
 * Maps org.springframework.dao.DataIntegrityViolationException to user-friendly errors to be
 * displayed in JSONAPI.
 */
@Named
public class DataIntegrityViolationExceptionMapper
    implements ExceptionMapper<DataIntegrityViolationException> {
  
  private static final Integer STATUS_ON_ERROR = HttpStatus.UNPROCESSABLE_ENTITY_422;

  @Override
  public ErrorResponse toErrorResponse(DataIntegrityViolationException exception) {
    return new ErrorResponse(
        Collections.singletonList(
            ErrorData.builder()
                .setStatus(STATUS_ON_ERROR.toString())
                .setTitle("Data integrity violation")
                .setDetail(exception.getMessage())
                .build()
        ),
        STATUS_ON_ERROR
    );
  }

  @Override
  public DataIntegrityViolationException fromErrorResponse(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  @Override
  public boolean accepts(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }
}
