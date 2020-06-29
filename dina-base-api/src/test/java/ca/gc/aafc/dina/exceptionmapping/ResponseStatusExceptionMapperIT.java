package ca.gc.aafc.dina.exceptionmapping;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ResponseStatusExceptionMapperIT {  
 
  @Autowired
  private MockMvc mockMvc;  

  private final static String bucketUnderTest = "testBucket";
  private final static String fileUnderTest = "9ada0de3-b190-44d8-992d-f4d532bc11fb";
  private static String exceptionUnderTest = "exception";
   
  
  @Before
  public void setUpTest() throws JsonProcessingException, IOException {
    
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualObj = mapper.readTree(new File("exceptionResponse.json"));
    exceptionUnderTest = actualObj.asText(); 
    
  }  

  @Test
  public void downLoadFile_whenFileDoesNotExist_mapperCreatesReadableErrorMessages() throws Exception {
    ResultActions actions = mockMvc.perform(MockMvcRequestBuilders.get("/file" + File.separator+ bucketUnderTest + File.separator + fileUnderTest ));
    actions.andExpect(status().is(404)).andExpect( (ResultMatcher) MockMvcResultMatchers.content().json(exceptionUnderTest));    
  }  

}
