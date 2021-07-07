package ca.gc.aafc.dina.exceptionmapping;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ResponseStatusExceptionMapperTest {  
 
  private ResponseStatusExceptionMapper responseStatusExceptionMapper = new ResponseStatusExceptionMapper();

  private static final String errorMessage = "Minio file or bucket not found";

  @Test
  public void testMapperCreatesReadableErrorMessages() throws Exception {

    ResponseStatusException rsEx = new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage, null);  

    assertEquals(rsEx.getStatus().value(), responseStatusExceptionMapper.toErrorResponse(rsEx).getHttpStatus());
    assertEquals(rsEx.getMessage(), 
          responseStatusExceptionMapper.toErrorResponse(rsEx).getErrors().stream().findFirst().get().getDetail());
   
  }  
}
