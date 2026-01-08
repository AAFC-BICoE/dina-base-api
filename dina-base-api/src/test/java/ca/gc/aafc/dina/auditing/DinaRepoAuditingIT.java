package ca.gc.aafc.dina.auditing;

import ca.gc.aafc.dina.DinaUserConfig;
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.config.PersonTestConfig;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.exception.ResourceGoneException;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import org.apache.commons.lang3.RandomStringUtils;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.SnapshotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import javax.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
  classes = {TestDinaBaseApp.class, DinaUserConfig.class, PersonTestConfig.class},
  properties = "dina.auditing.enabled = true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaRepoAuditingIT {

  @Inject
  private Javers javers;

  @Inject
  private DinaRepositoryV2<PersonDTO, Person> dinaRepository;

  @Inject
  private DinaRepositoryV2<DepartmentDto, Department> departmentRepository;

  @Inject
  private NamedParameterJdbcTemplate jdbcTemplate;

  @BeforeEach
  public void beforeEach() {
    cleanSnapShotRepo();
  }

  @Test
  @Transactional
  public void create_SnapShotsPersisted() throws ResourceGoneException, ResourceNotFoundException {
    UUID departmentUUID = UUID.randomUUID();
    DepartmentDto dto = DepartmentDto.builder().location("loc").uuid(departmentUUID).build();
    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(null, DepartmentDto.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    departmentRepository.create(docToCreate, null);

    PersonDTO personDto = createPersonDto();
    JsonApiDocument personDocToCreate = JsonApiDocuments
      .createJsonApiDocumentWithRelToOne(null, PersonDTO.TYPE_NAME, JsonAPITestHelper.toAttributeMap(personDto),
        Map.of("department", JsonApiDocument.ResourceIdentifier.builder()
          .type(DepartmentDto.TYPE_NAME).id(departmentUUID).build()));

    UUID id = dinaRepository.create(personDocToCreate, null).getDto().getUuid();

    CdoSnapshot result = javers.getLatestSnapshot(id.toString(), PersonDTO.class).get();
    assertEquals(SnapshotType.INITIAL, result.getType());
    assertEquals(DinaUserConfig.AUTH_USER_NAME, result.getCommitMetadata().getAuthor());

    //cleanup
    dinaRepository.delete(id);
  }

  @Test
  @Transactional
  public void update_SnapShotsPersisted() throws ResourceGoneException, ResourceNotFoundException {
    PersonDTO personDto = createPersonDto();
    JsonApiDocument personDocToCreate = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(personDto));
    UUID id = dinaRepository.create(personDocToCreate, null).getDto().getUuid();

    JsonApiDocument personDocToUpdate = JsonApiDocuments.createJsonApiDocument(id, PersonDTO.TYPE_NAME,
      Map.of("name", RandomStringUtils.random(4)));

    dinaRepository.update(personDocToUpdate);
    CdoSnapshot result = javers.getLatestSnapshot(id.toString(), PersonDTO.class).get();
    assertEquals(SnapshotType.UPDATE, result.getType());
    assertEquals(DinaUserConfig.AUTH_USER_NAME, result.getCommitMetadata().getAuthor());

    //cleanup
    dinaRepository.delete(id);
  }

  @Test
  @Transactional
  public void delete_SnapShotsPersisted() throws ResourceGoneException, ResourceNotFoundException {
    PersonDTO personDto = createPersonDto();
    JsonApiDocument personDocToCreate = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(personDto));
    UUID id = dinaRepository.create(personDocToCreate, null).getDto().getUuid();

    dinaRepository.delete(id);
    CdoSnapshot result = javers.getLatestSnapshot(id.toString(), PersonDTO.class).get();
    assertEquals(SnapshotType.TERMINAL, result.getType());
    assertEquals(DinaUserConfig.AUTH_USER_NAME, result.getCommitMetadata().getAuthor());
  }

  private void cleanSnapShotRepo() {
    jdbcTemplate.update(
      "DELETE FROM jv_snapshot where commit_fk IS NOT null",
      Collections.emptyMap());
    jdbcTemplate.update(
      "DELETE FROM jv_commit where commit_pk IS NOT null",
      Collections.emptyMap());
  }

  private PersonDTO createPersonDto() {
    return PersonDTO.builder()
      .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
      .name("DinaRepoAuditingIT_" + RandomStringUtils.random(4)).build();
  }

}
