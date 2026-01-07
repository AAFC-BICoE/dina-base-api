package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.config.PersonTestConfig;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.exception.ResourceGoneException;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;

import java.util.UUID;
import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
  classes = {TestDinaBaseApp.class, PersonTestConfig.class},
  properties = "keycloak.enabled: true"
)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class WithMockKeycloakUserIT {

  private static final String GROUP_1 = "group 1";

  @Inject
  private DinaRepositoryV2<PersonDTO, Person> dinaRepository;

  @Inject
  private DinaAuthenticatedUser currentUser;

  @WithMockKeycloakUser(groupRole = {"group 1:user", "group 3:user"})
  @Test
  @Transactional
  public void create_AuthorizedGroup_CreatesObject()
      throws ResourceGoneException, ResourceNotFoundException {
    PersonDTO dto = PersonDTO.builder().uuid(UUID.randomUUID()).group(GROUP_1).name("name").build();

    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    PersonDTO result = dinaRepository.create(docToCreate, null).getDto();
    assertNotNull(result.getUuid());

    // Clean up the person created for this test.
    dinaRepository.delete(result.getUuid());
  }

  @WithMockKeycloakUser(
    agentIdentifier = "agent one",
    internalIdentifier = "internal",
    groupRole = {"group 1:user", "group 3:user"})
  @Test
  public void withMockedUser_UserMocked() {
    assertEquals("internal", currentUser.getInternalIdentifier());
    assertEquals("agent one", currentUser.getAgentIdentifier());
  }

  @WithMockKeycloakUser(groupRole = {GROUP_1 + ": user"})
  @Test
  public void withMockKeycloakUser_onSpaceBeforeRole_RoleExtracted() {
    assertEquals(DinaRole.USER, currentUser.getRolesPerGroup().get(GROUP_1).iterator().next());
  }

}
