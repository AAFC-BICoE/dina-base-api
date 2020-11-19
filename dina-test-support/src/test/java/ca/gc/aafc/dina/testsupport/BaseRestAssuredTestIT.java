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

  private static final String RESOURCE_PATH = "crnk-test-data";

  public BaseRestAssuredTestIT() {
    super(RESOURCE_PATH);
  }
  
  /**
   * Testing a complete CRUD cycle using {@link BaseRestAssuredTest} methods.
   */
  @Test
	public void baseClass_OnCRUDOperations_ExpectedReturnCodesReturned() {
    CrnkTestData testData = CrnkTestData.builder().note("note").build();

    ValidatableResponse postResponse = sendPost(
      JsonAPITestHelper.toJsonAPIMap(RESOURCE_PATH, testData));
    
    String id =  JsonAPITestHelper.extractId(postResponse);

    sendGet(id);
    
    CrnkTestData updatedTestData = CrnkTestData.builder().note("updated note").build();
    sendPatch(id,
      JsonAPITestHelper.toJsonAPIMap(RESOURCE_PATH, 
      JsonAPITestHelper.toAttributeMap(updatedTestData), null,  null));

    // re-get and make sure the note is updated
    sendGet(id)
      .body("data.attributes.note", Matchers.equalTo("updated note"));
    
    sendDelete(id);

    sendGet("", id, HttpStatus.NOT_FOUND.value());

	}
}
