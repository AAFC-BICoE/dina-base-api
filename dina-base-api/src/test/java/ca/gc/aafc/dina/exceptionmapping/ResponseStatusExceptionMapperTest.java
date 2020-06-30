package ca.gc.aafc.dina.exceptionmapping;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ResponseStatusExceptionMapperTest {  
 
  @Inject
  private ResponseStatusExceptionMapper responseStatusExceptionMapper;

  private static final String errorMessage = "Minio file or bucket not found";

  @Test
  public void testMapperCreatesReadableErrorMessages() throws Exception {
    try {
      throw  new ResponseStatusException(HttpStatus.NOT_FOUND, errorMessage, null);  
    } catch (ResponseStatusException e) {   
      assertEquals(e.getStatus().value(), responseStatusExceptionMapper.toErrorResponse(e).getHttpStatus());
      assertEquals(e.getMessage(), 
          responseStatusExceptionMapper.toErrorResponse(e).getErrors().stream().findFirst().get().getDetail());
    }    
  }  
}
