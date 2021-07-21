package ca.gc.aafc.dina.exceptionmapping;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Named;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.engine.error.ExceptionMapper;
import io.crnk.core.engine.http.HttpStatus;

/**
 * Maps javax.validation.ConstraintViolationException to user-friendly errors to be displayed in
 * JSONAPI.
 */
@Named
public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<ConstraintViolationException> {
  
  private static final Integer STATUS_ON_ERROR = HttpStatus.UNPROCESSABLE_ENTITY_422;

  @Override
  public ErrorResponse toErrorResponse(ConstraintViolationException exception) {
    return new ErrorResponse(
        exception.getConstraintViolations()
            .stream()
            .map(cv -> ErrorData.builder()
                .setStatus(STATUS_ON_ERROR.toString())
                .setTitle("Constraint violation")
                .setDetail(String.join(" ", cv.getLeafBean().getClass().getName(), violationName(cv), cv.getMessage()))
                .setSourcePointer(violationName(cv))
                .build())
            .collect(Collectors.toList()),
            STATUS_ON_ERROR
    );
  }

  @Override
  public ConstraintViolationException fromErrorResponse(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  @Override
  public boolean accepts(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  private String violationName(ConstraintViolation<?> cv) {
    String name = StreamSupport.stream(cv.getPropertyPath().spliterator(), false)
      .map(Path.Node::getName)
      .reduce((head, tail) -> tail)
      .orElseGet(() -> cv.getPropertyPath().toString());
    return name;
  }
  
}
