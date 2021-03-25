package ca.gc.aafc.dina.exceptionmapping;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

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

  @Inject
  private MessageSource messageSource;
  
  @Override
  public ErrorResponse toErrorResponse(ConstraintViolationException exception) {
    return new ErrorResponse(
        exception.getConstraintViolations()
            .stream()
            .map(cv -> ErrorData.builder()
                .setStatus(STATUS_ON_ERROR.toString())
                .setTitle("Constraint violation")
                .setDetail(mapDetail(cv))
          .build())
        .collect(Collectors.toList()),
      STATUS_ON_ERROR
    );
  }

  private String mapDetail(ConstraintViolation<?> cv) {
    String messageTemplate = cv.getMessageTemplate();
    if (messageTemplate.startsWith("{") && messageTemplate.endsWith("}")) {
      String stripped = messageTemplate.substring(1, messageTemplate.length() - 1);
      if (stripped.startsWith("javax")) {
        return String.join(" ", cv.getPropertyPath().toString(), cv.getMessage());
      }
      return messageSource.getMessage(stripped, null, LocaleContextHolder.getLocale());
    } else {
      return cv.getMessage();
    }
  }

  @Override
  public ConstraintViolationException fromErrorResponse(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }

  @Override
  public boolean accepts(ErrorResponse errorResponse) {
    throw new UnsupportedOperationException("Crnk client not supported");
  }
  
}
