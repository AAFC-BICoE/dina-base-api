package ca.gc.aafc.dina.jsonapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIOperationBuilder;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.crnk.core.engine.http.HttpMethod;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Integration test making sure the operation endpoint is available and working as expected.
 */
@SpringBootTest(classes = ca.gc.aafc.dina.TestConfiguration.class ,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = { "dev-user.enabled: true", "keycloak.enabled: false" })
public class OperationJsonApiIT extends BaseRestAssuredTest {

  public OperationJsonApiIT() {
    super("");
  }

  /**
   * Testing a complete CRUD cycle using {@link BaseRestAssuredTest} methods.
   */
  @Test public void operations_OnCRUDOperations_ExpectedReturnCodesReturned() {

    PersonDTO person1 = PersonDTO.builder()
        .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
        .name(RandomStringUtils.randomAlphabetic(4)).build();
    PersonDTO person2 = PersonDTO.builder()
        .nickNames(Arrays.asList("a", "w", "y").toArray(new String[0]))
        .name(RandomStringUtils.randomAlphabetic(4)).build();

    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
        .addOperation(HttpMethod.POST, PersonDTO.TYPE_NAME, JsonAPITestHelper
            .toJsonAPIMap(PersonDTO.TYPE_NAME, JsonAPITestHelper.toAttributeMap(person1), null,
                UUID.randomUUID().toString())) //we need to provide an identifier for operation
        .addOperation(HttpMethod.POST, PersonDTO.TYPE_NAME, JsonAPITestHelper
            .toJsonAPIMap(PersonDTO.TYPE_NAME, JsonAPITestHelper.toAttributeMap(person2), null,
                UUID.randomUUID().toString())).buildOperation();

    ValidatableResponse operationResponse = sendOperation(operationMap, 200);

    Integer returnCode = operationResponse.extract().body().jsonPath().getInt("[0].status");
    assertEquals(201, returnCode);
  }

}
