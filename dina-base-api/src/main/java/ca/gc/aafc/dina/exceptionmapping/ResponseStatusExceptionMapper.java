package ca.gc.aafc.dina.exceptionmapping;

import org.springframework.web.server.ResponseStatusException;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;

public class ResponseStatusExceptionMapper implements ExceptionMapper<ResponseStatusException> {
  
  public static final int HTTP_ERROR_CODE = 404;

  @Override
  public ErrorResponse toErrorResponse(ResponseStatusException exception) {
    ErrorData error = ErrorData.builder().setDetail(exception.getMessage()).build();
    return ErrorResponse.builder().setStatus(HTTP_ERROR_CODE).setSingleErrorData(error).build();
  }

  @Override
  public ResponseStatusException fromErrorResponse(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  @Override
  public boolean accepts(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

}