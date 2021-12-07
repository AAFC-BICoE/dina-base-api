package ca.gc.aafc.dina.security;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;

import io.crnk.core.queryspec.QuerySpec;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class, properties = "keycloak.enabled: true")
public class DinaGroupBasedPermissionsTest {

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Inject
  private BaseDAO baseDAO;

  private static final String PERSON_NAME = "name";
  private static final String ROLE = "STAFF";
  private static final String GROUP_VALID = "GROUP_1";
  private static final String GROUP_INVALID = "INVALID_GROUP";

  private Person validPersisted;
  private Person invalidPersisted;

  @BeforeEach
  public void setup() {
    // Create a person with the correct group.
    validPersisted = Person.builder().uuid(UUID.randomUUID()).group(GROUP_VALID).name(PERSON_NAME).build();
    baseDAO.create(validPersisted);

    // Create a person with the incorrect group.
    invalidPersisted = Person.builder().uuid(UUID.randomUUID()).group(GROUP_INVALID).name(PERSON_NAME).build();
    baseDAO.create(invalidPersisted);
  }

  @Test
  @WithMockKeycloakUser(groupRole = GROUP_VALID + ":" + ROLE)
  public void read_AuthorizedGroup_ReadObject() {
    PersonDTO findPerson = Assertions.assertDoesNotThrow(() -> {
      return dinaRepository.findOne(validPersisted.getUuid(), new QuerySpec(PersonDTO.class));
    });
    Assertions.assertNotNull(findPerson);
  }

  @Test
  @WithMockKeycloakUser(groupRole = GROUP_VALID + ":" + ROLE)
  public void read_UnAuthorized_ThrowsAccessDeniedException() {
    Assertions.assertThrows(AccessDeniedException.class, () -> 
        dinaRepository.findOne(invalidPersisted.getUuid(), new QuerySpec(PersonDTO.class)));
  }

  @Test
  @WithMockKeycloakUser(groupRole = GROUP_VALID + ":" + ROLE)
  public void create_AuthorizedGroup_CreatesObject() {
    PersonDTO dto = PersonDTO.builder().uuid(UUID.randomUUID()).group(GROUP_VALID).name(PERSON_NAME).build();
    PersonDTO result = dinaRepository.create(dto);
    Assertions.assertNotNull(result.getUuid());
  }

  @Test
  @WithMockKeycloakUser(groupRole = GROUP_VALID + ":" + ROLE)
  public void create_UnAuthorized_ThrowsAccessDeniedException() {
    PersonDTO dto = PersonDTO.builder()
      .uuid(UUID.randomUUID())
      .group(GROUP_INVALID)
      .name(PERSON_NAME).build();
    Assertions.assertThrows(AccessDeniedException.class, () -> dinaRepository.create(dto));
  }

  @Test
  @WithMockKeycloakUser(groupRole = GROUP_VALID + ":" + ROLE)
  public void save_AuthorizedGroup_UpdatesObject() {
    String expectedName = RandomStringUtils.random(6);

    PersonDTO updateDto = PersonDTO.builder().uuid(validPersisted.getUuid()).name(expectedName).build();
    dinaRepository.save(updateDto);

    String resultName = baseDAO.findOneByNaturalId(validPersisted.getUuid(), Person.class).getName();
    Assertions.assertEquals(expectedName, resultName);
  }

  @Test
  @WithMockKeycloakUser(groupRole = GROUP_VALID + ":" + ROLE)
  public void save_UnAuthorizedGroup_ThrowsAccessDeniedException() {
    String expectedName = RandomStringUtils.random(6);

    PersonDTO updateDto = PersonDTO.builder()
      .uuid(invalidPersisted.getUuid())
      .group(GROUP_VALID)
      .name(expectedName).build();
    Assertions.assertThrows(AccessDeniedException.class, () -> dinaRepository.save(updateDto));
  }

  @Test
  @WithMockKeycloakUser(groupRole = GROUP_VALID + ":" + ROLE)
  public void delete_AuthorizedGroup_UpdatesObject() {
    Assertions.assertNotNull(baseDAO.findOneByNaturalId(validPersisted.getUuid(), Person.class));
    dinaRepository.delete(validPersisted.getUuid());
    Assertions.assertNull(baseDAO.findOneByNaturalId(validPersisted.getUuid(), Person.class));
  }

  @Test
  @WithMockKeycloakUser(groupRole = GROUP_VALID + ":" + ROLE)
  public void delete_UnAuthorizedGroup_ThrowsAccessDeniedException() {
    Assertions.assertNotNull(baseDAO.findOneByNaturalId(invalidPersisted.getUuid(), Person.class));
    Assertions.assertThrows(AccessDeniedException.class, () -> dinaRepository.delete(invalidPersisted.getUuid()));
  }

}
