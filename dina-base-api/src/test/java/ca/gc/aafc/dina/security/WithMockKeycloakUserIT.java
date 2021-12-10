package ca.gc.aafc.dina.security;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.inject.Inject;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
  classes = TestDinaBaseApp.class,
  properties = "keycloak.enabled: true"
)
public class WithMockKeycloakUserIT {

  private static final String GROUP_1 = "group 1";

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;
  @Inject
  private DinaAuthenticatedUser currentUser;

  @WithMockKeycloakUser(groupRole = {"group 1:staff", "group 3:staff"})
  @Test
  public void create_AuthorizedGroup_CreatesObject() {
    PersonDTO dto = PersonDTO.builder().uuid(UUID.randomUUID()).group(GROUP_1).name("name").build();
    PersonDTO result = dinaRepository.create(dto);
    assertNotNull(result.getUuid());

    dinaRepository.delete(result.getUuid());
  }

  @WithMockKeycloakUser(
    agentIdentifier = "agent one",
    internalIdentifier = "internal",
    groupRole = {"group 1:staff", "group 3:staff"})
  @Test
  public void withMockedUser_UserMocked() {
    Assertions.assertEquals("internal", currentUser.getInternalIdentifier());
    Assertions.assertEquals("agent one", currentUser.getAgentIdentifier());
  }

}
