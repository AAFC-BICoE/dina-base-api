package ca.gc.aafc.dina.exceptionmapping;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ResponseStatusExceptionMapperIT {  
 
  @Inject
  private ResponseStatusExceptionMapper responseStatusExceptionMapper;
  
  @Inject
  private TestFileController ftc;

  private static final String bucketUnderTest = "testBucket";
  private static final String fileUnderTest = "9ada0de3-b190-44d8-992d-f4d532bc11fb";

  @Test
  public void downLoadFile_whenFileDoesNotExist_mapperCreatesReadableErrorMessages() throws Exception {
    try {
      ftc.downloadFile(bucketUnderTest, fileUnderTest);
    } catch (ResponseStatusException e) {
      assertEquals(e.getStatus().ordinal(), responseStatusExceptionMapper.toErrorResponse(e).getHttpStatus());
      assertEquals(e.getMessage(), 
          responseStatusExceptionMapper.toErrorResponse(e).getErrors().stream().findFirst().get().getDetail());
    }    
  }  
}
