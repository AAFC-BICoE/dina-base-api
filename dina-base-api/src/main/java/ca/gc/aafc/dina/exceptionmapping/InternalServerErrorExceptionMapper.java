package ca.gc.aafc.dina.exceptionmapping;

import ca.gc.aafc.dina.security.TextHtmlSanitizer;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;
import io.crnk.core.exception.InternalServerErrorException;

import javax.inject.Named;
import java.util.List;

/**
 * dina specific exception mapper for Crnk InternalServerErrorException.
 * The main goal is to sanitize the error title and message.
 */
@Named
public class InternalServerErrorExceptionMapper implements ExceptionMapper<InternalServerErrorException> {

  @Override
  public ErrorResponse toErrorResponse(InternalServerErrorException e) {
    return new ErrorResponse(
            List.of(sanitizeErrorData(e.getErrorData())), e.getHttpStatus());
  }

  @Override
  public InternalServerErrorException fromErrorResponse(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  @Override
  public boolean accepts(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  public ErrorData sanitizeErrorData(ErrorData errorData) {
    return ErrorData.builder()
            .setStatus(errorData.getStatus())
            .setCode(errorData.getCode())
            .setTitle(TextHtmlSanitizer.sanitizeText(errorData.getTitle()))
            .setDetail(TextHtmlSanitizer.sanitizeText(errorData.getDetail()))
            .setSourcePointer(errorData.getSourcePointer())
            .build();
  }
}
