package ca.gc.aafc.dina.testsupport;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import ca.gc.aafc.dina.testsupport.crnk.CrnkTestData;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.response.ValidatableResponse;

/**
 * Test making sure {@link BaseRestAssuredTest} is usable.
 */
@SpringBootTest(classes = TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseRestAssuredTestIT extends BaseRestAssuredTest {

  private String RESOURCE_PATH = "crnk-test-data";

  public BaseRestAssuredTestIT() {
    super("");
  }
  
  /**
   * Testing a complete CRUD cycle using {@link BaseRestAssuredTest} methods.
   */
  @Test
	public void baseClass_OnCRUDOperations_ExpectedReturnCodesReturned() {
    CrnkTestData testData = CrnkTestData.builder().note("note").build();

    ValidatableResponse postResponse = sendPost(RESOURCE_PATH, 
      JsonAPITestHelper.toJsonAPIMap(RESOURCE_PATH, 
      JsonAPITestHelper.toAttributeMap(testData), null, null));
    
    String id =  postResponse.extract()
      .body()
      .jsonPath()
      .get("data.id");

    sendGet(RESOURCE_PATH, id);
    
    CrnkTestData updatedTestData = CrnkTestData.builder().note("updated note").build();
    sendPatch(RESOURCE_PATH, id,
      JsonAPITestHelper.toJsonAPIMap(RESOURCE_PATH, 
      JsonAPITestHelper.toAttributeMap(updatedTestData), null, null));

    // re-get and make sure the note is updated
    sendGet(RESOURCE_PATH, id)
      .body("data.attributes.note", Matchers.equalTo("updated note"));
    
    sendDelete(RESOURCE_PATH, id);

    sendGet(RESOURCE_PATH, id, HttpStatus.NOT_FOUND.value());

	}
}