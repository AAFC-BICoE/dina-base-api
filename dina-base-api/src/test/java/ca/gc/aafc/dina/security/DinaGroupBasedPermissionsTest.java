package ca.gc.aafc.dina.security;

import java.util.Arrays;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.DinaRepository;

import io.crnk.core.queryspec.QuerySpec;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class, properties = "keycloak.enabled: true")
public class DinaGroupBasedPermissionsTest {

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Inject
  private BaseDAO baseDAO;

  private static final String GROUP_1 = "group 1";

  @BeforeEach
  public void beforeEach() {
    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    TestDinaBaseApp.mockToken(Arrays.asList("/" + GROUP_1 + "/staff"), mockToken);

    SecurityContextHolder.getContext().setAuthentication(mockToken);
  }

  @Test
  public void read_AuthorizedGroup_ReadObject() {
    Person persisted = Person.builder().uuid(UUID.randomUUID()).group(GROUP_1).name("name").build();
    baseDAO.create(persisted);

    PersonDTO findPerson = Assertions.assertDoesNotThrow(() -> {
      return dinaRepository.findOne(persisted.getUuid(), new QuerySpec(PersonDTO.class));
    });
    Assertions.assertNotNull(findPerson);
  }

  @Test
  public void read_UnAuthorized_ThrowsAccessDeniedException() {
    Person persisted = Person.builder().uuid(UUID.randomUUID()).group("Invalid_Group").name("name").build();
    baseDAO.create(persisted);

    Assertions.assertThrows(AccessDeniedException.class, () -> dinaRepository.findOne(persisted.getUuid(), new QuerySpec(PersonDTO.class)));
  }

  @Test
  public void create_AuthorizedGroup_CreatesObject() {
    PersonDTO dto = PersonDTO.builder().uuid(UUID.randomUUID()).group(GROUP_1).name("name").build();
    PersonDTO result = dinaRepository.create(dto);
    Assertions.assertNotNull(result.getUuid());
  }

  @Test
  public void create_UnAuthorized_ThrowsAccessDeniedException() {
    PersonDTO dto = PersonDTO.builder()
      .uuid(UUID.randomUUID())
      .group("Invalid_Group")
      .name("name").build();
    Assertions.assertThrows(AccessDeniedException.class, () -> dinaRepository.create(dto));
  }

  @Test
  public void save_AuthorizedGroup_UpdatesObject() {
    String expectedName = RandomStringUtils.random(6);
    Person persisted = Person.builder().uuid(UUID.randomUUID()).group(GROUP_1).name("name").build();
    baseDAO.create(persisted);

    PersonDTO updateDto = PersonDTO.builder().uuid(persisted.getUuid()).name(expectedName).build();
    dinaRepository.save(updateDto);

    String resultName = baseDAO.findOneByNaturalId(persisted.getUuid(), Person.class).getName();
    Assertions.assertEquals(expectedName, resultName);
  }

  @Test
  public void save_UnAuthorizedGroup_ThrowsAccessDeniedException() {
    String expectedName = RandomStringUtils.random(6);
    Person persisted = Person.builder()
      .uuid(UUID.randomUUID())
      .group("Invalid_Group")
      .name("name").build();
    baseDAO.create(persisted);

    PersonDTO updateDto = PersonDTO.builder()
      .uuid(persisted.getUuid())
      .group(GROUP_1)
      .name(expectedName).build();
    Assertions.assertThrows(AccessDeniedException.class, () -> dinaRepository.save(updateDto));
  }

  @Test
  public void delete_AuthorizedGroup_UpdatesObject() {
    Person persisted = Person.builder().uuid(UUID.randomUUID()).group(GROUP_1).name("name").build();
    baseDAO.create(persisted);

    Assertions.assertNotNull(baseDAO.findOneByNaturalId(persisted.getUuid(), Person.class));
    dinaRepository.delete(persisted.getUuid());
    Assertions.assertNull(baseDAO.findOneByNaturalId(persisted.getUuid(), Person.class));
  }

  @Test
  public void delete_UnAuthorizedGroup_ThrowsAccessDeniedException() {
    Person persisted = Person.builder()
      .uuid(UUID.randomUUID())
      .group("Invalid_Group")
      .name("name").build();
    baseDAO.create(persisted);

    Assertions.assertNotNull(baseDAO.findOneByNaturalId(persisted.getUuid(), Person.class));
    Assertions.assertThrows(AccessDeniedException.class, () -> dinaRepository.delete(persisted.getUuid()));
  }

}
