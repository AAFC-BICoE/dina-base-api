package ca.gc.aafc.dina.testsupport.repository;

import java.io.UnsupportedEncodingException;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Abstract class to test repository layer using Spring MockMvc.
 * MockMvc allows to mimic a Http request to Spring controllers without running test server (webEnvironment).
 *
 */
public abstract class MockMvcBasedRepository {

  protected final String baseUrl;
  protected final ObjectMapper objMapper;

  protected MockMvcBasedRepository(String baseUrl, ObjectMapper objMapper) {
    this.baseUrl = baseUrl;
    this.objMapper = objMapper;
  }

  protected abstract MockMvc getMockMvc();

  protected MvcResult sendGet(String id) throws Exception {
    return getMockMvc().perform(
        get(baseUrl + "/" + id)
          .contentType(BaseRestAssuredTest.JSON_API_CONTENT_TYPE)
      )
      .andExpect(status().isOk())
      .andReturn();
  }

  protected JsonApiDocument toJsonApiDocument(MvcResult mvcResult)
      throws UnsupportedEncodingException, JsonProcessingException {
    return objMapper.readValue(mvcResult.getResponse().getContentAsString(), JsonApiDocument.class);
  }
}
