package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIOperationBuilder;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test making sure the operation endpoint is available and working as expected.
 */
@SpringBootTest(classes = TestDinaBaseApp.class ,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = { "dev-user.enabled: true", "keycloak.enabled: false" })
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class OperationJsonApiIT extends BaseRestAssuredTest {

  public OperationJsonApiIT() {
    super("");
  }

  /**
   * Testing a complete CRUD cycle using {@link BaseRestAssuredTest} methods.
   */
  @Test
  public void operations_OnCRUDOperations_ExpectedReturnCodesReturned() {
    PersonDTO person1 = PersonDTO.builder()
        .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
        .name("OperationJsonApiIT_" + RandomStringUtils.randomAlphabetic(4)).build();
    String person1Uuid = UUID.randomUUID().toString();
    PersonDTO person2 = PersonDTO.builder()
        .nickNames(Arrays.asList("a", "w", "y").toArray(new String[0]))
        .name("OperationJsonApiIT_" +RandomStringUtils.randomAlphabetic(4)).build();

    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
        .addOperation(HttpMethod.POST, PersonDTO.TYPE_NAME, JsonAPITestHelper
            .toJsonAPIMap(PersonDTO.TYPE_NAME, JsonAPITestHelper.toAttributeMap(person1), person1Uuid)) // Crnk requires an identifier even if it's a POST
        .addOperation(HttpMethod.POST, PersonDTO.TYPE_NAME, JsonAPITestHelper
            .toJsonAPIMap(PersonDTO.TYPE_NAME, JsonAPITestHelper.toAttributeMap(person2), UUID.randomUUID().toString()))
        .buildOperation();

    ValidatableResponse operationResponse = sendOperation(operationMap);
    assertEquals(201, operationResponse.extract().body().jsonPath().getInt("[0].status"));
    assertEquals(201, operationResponse.extract().body().jsonPath().getInt("[1].status"));

    String person1AssignedId = operationResponse.extract().body().jsonPath().getString("[0].data.id");
    String person2AssignedId = operationResponse.extract().body().jsonPath().getString("[1].data.id");
    assertNotEquals("Assigned id should differ from the one provided", person1Uuid, person1AssignedId);

    //cleanup
    sendDelete(PersonDTO.TYPE_NAME, person1AssignedId);
    sendDelete(PersonDTO.TYPE_NAME, person2AssignedId);
  }

  @Test
  public void operations_onUnsafeInvalidType_errorDetailSanitized() {
    PersonDTO person1 = PersonDTO.builder()
            .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
            .name("OperationJsonApiIT_" + RandomStringUtils.randomAlphabetic(4)).build();
    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
            .addOperation(HttpMethod.POST, PersonDTO.TYPE_NAME, JsonAPITestHelper
                    .toJsonAPIMap("invalidtype<iframe src=javascript:alert(32311)>", JsonAPITestHelper.toAttributeMap(person1), UUID.randomUUID().toString())) // Crnk requires an identifier even if it's a POST
            .buildOperation();
    ValidatableResponse operationResponse = sendOperation(operationMap);
    // response should be sanitized
    assertEquals("Repository for a resource not found: invalidtype", operationResponse.extract().body().jsonPath().getString("[0].errors[0].detail"));
  }
  
}
