package ca.gc.aafc.dina.exceptionmapping;

import javax.inject.Named;

import org.springframework.web.server.ResponseStatusException;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;

@Named
public class ResponseStatusExceptionMapper implements ExceptionMapper<ResponseStatusException> {
  
  @Override
  public ErrorResponse toErrorResponse(ResponseStatusException exception) {
    ErrorData error = ErrorData.builder().setDetail(exception.getMessage()).build();
    return ErrorResponse.builder().setStatus(exception.getStatus().value()).setSingleErrorData(error).build();
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
