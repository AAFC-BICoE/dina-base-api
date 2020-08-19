package ca.gc.aafc.dina.auditing;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.security.DevUserConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.SnapshotType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
  classes = {TestConfiguration.class},
  properties = {"dina.auditing.enabled = true",
    "dev-user.enabled = true",
    "keycloak.enabled = false"})
public class DinaRepoAuditingIT {

  @Inject
  private Javers javers;

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Inject
  private NamedParameterJdbcTemplate jdbcTemplate;

  @BeforeEach
  public void beforeEach() {
    cleanSnapShotRepo();
  }

  @Test
  public void create_SnapShotsPersisted() {
    UUID id = dinaRepository.create(createPersonDto()).getUuid();
    CdoSnapshot result = javers.getLatestSnapshot(id.toString(), PersonDTO.class).get();
    assertEquals(SnapshotType.INITIAL, result.getType());
    assertEquals(DevUserConfig.USERNAME, result.getCommitMetadata().getAuthor());
  }

  @Test
  public void update_SnapShotsPersisted() {
    UUID id = dinaRepository.create(createPersonDto()).getUuid();

    dinaRepository.save(PersonDTO.builder().uuid(id).name(RandomStringUtils.random(4)).build());
    CdoSnapshot result = javers.getLatestSnapshot(id.toString(), PersonDTO.class).get();
    assertEquals(SnapshotType.UPDATE, result.getType());
    assertEquals(DevUserConfig.USERNAME, result.getCommitMetadata().getAuthor());
  }

  @Test
  public void delete_SnapShotsPersisted() {
    UUID id = dinaRepository.create(createPersonDto()).getUuid();

    dinaRepository.delete(id);
    CdoSnapshot result = javers.getLatestSnapshot(id.toString(), PersonDTO.class).get();
    assertEquals(SnapshotType.TERMINAL, result.getType());
    assertEquals(DevUserConfig.USERNAME, result.getCommitMetadata().getAuthor());
  }

  private void cleanSnapShotRepo() {
    jdbcTemplate.update("DELETE FROM jv_snapshot where commit_fk IS NOT null", Collections.emptyMap());
    jdbcTemplate.update("DELETE FROM jv_commit where commit_pk IS NOT null", Collections.emptyMap());
  }

  private PersonDTO createPersonDto() {
    return PersonDTO.builder()
      .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
      .name(RandomStringUtils.random(4)).build();
  }

}
