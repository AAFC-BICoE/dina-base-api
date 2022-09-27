package ca.gc.aafc.dina.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIOperationBuilder;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.crnk.core.engine.http.HttpMethod;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            .toJsonAPIMap(PersonDTO.TYPE_NAME, JsonAPITestHelper.toAttributeMap(person2),"1234")) //the id can even be a non-uuid value
        .buildOperation();

    ValidatableResponse operationResponse = sendOperation(operationMap);

    Integer returnCode = operationResponse.extract().body().jsonPath().getInt("[0].status");
    String person1AssignedId = operationResponse.extract().body().jsonPath().getString("[0].data.id");

    assertEquals(201, returnCode);
    assertNotEquals("Assigned id should differ from the one provided", person1Uuid, person1AssignedId);
  }

}
