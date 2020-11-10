package ca.gc.aafc.dina.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomUtils;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.SnapshotType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ca.gc.aafc.dina.DinaUserConfig;
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.service.AuditService.AuditInstance;

@SpringBootTest(classes = TestDinaBaseApp.class, properties = "dina.auditing.enabled = true")
public class AuditServiceIT {

  @Inject
  private Javers javers;

  @Inject
  private AuditService serviceUnderTest;

  @Inject
  private NamedParameterJdbcTemplate jdbcTemplate;

  private static final String AUTHOR = "dina_user";
  private static final String TYPE = EmployeeDto.TYPENAME;
  private static final Integer INSTANCE_ID = RandomUtils.nextInt();

  /**
   * Persists 6 snap shots in total for each test. Total expected commits for
   * Author = 4, Instance = 2, No Filter = 6
   */
  @BeforeEach
  public void beforeEachTest() {
    cleanSnapShotRepo();

    // Has Author 2 Commits
    EmployeeDto hasAuthor = createDto();
    javers.commit(AUTHOR, hasAuthor);
    hasAuthor.setName("update");
    javers.commit(AUTHOR, hasAuthor);

    // Anonymous Author 2 Commits
    EmployeeDto noAuthor = createDto();
    javers.commit("Anonymous", noAuthor);
    noAuthor.setName("update");
    javers.commit("Anonymous", noAuthor);

    // Has Author With specific instance id 2 commits
    EmployeeDto withInstanceID = createDto();
    withInstanceID.setId(INSTANCE_ID);
    javers.commit(AUTHOR, withInstanceID);
    withInstanceID.setName("update");
    javers.commit(AUTHOR, withInstanceID);
  }

  @Test
  public void findAll_whenNoFilter_AllSnapShotsReturned() {
    List<CdoSnapshot> results = serviceUnderTest.findAll(null, null, 10, 0);
    assertEquals(6, results.size());
  }

  @Test
  public void findAll_whenFilteredByInstance_snapshotsFiltered() {
    AuditInstance instance = AuditInstance.builder()
      .type(TYPE)
      .id(Integer.toString(INSTANCE_ID))
      .build();
    List<CdoSnapshot> results = serviceUnderTest.findAll(instance, null, 10, 0);
    assertEquals(2, results.size());
    results.forEach(shot -> 
      assertEquals(
        String.join("/", TYPE, Integer.toString(INSTANCE_ID)),
        shot.getGlobalId().toString()));
  }

  @Test
  public void findAll_whenFilteredByAuthor_snapshotsFiltered() {
    List<CdoSnapshot> results = serviceUnderTest.findAll(null, AUTHOR, 10, 0);
    assertEquals(4, results.size());
    results.forEach(shot -> assertEquals(AUTHOR, shot.getCommitMetadata().getAuthor()));
  }

  @Test
  public void findAll_WithLimit_LimitsResults() {
    List<CdoSnapshot> results = serviceUnderTest.findAll(null, null, 1, 0);
    assertEquals(1, results.size());
  }

  @Test
  public void findAll_WithOffset_ResultsOffset() {
    List<CdoSnapshot> results = serviceUnderTest.findAll(null, null, 10, 5);
    assertEquals(1, results.size());
  }

  @Test
  public void getResouceCount_NoFilter_ReturnsAllCount() {
    Long expected = serviceUnderTest.getResouceCount(null, null);
    assertEquals(Long.valueOf(6), expected);
  }

  @Test
  public void getResouceCount_AuthorFilter_ReturnsFilteredCount() {
    Long expected = serviceUnderTest.getResouceCount(AUTHOR, null);
    assertEquals(Long.valueOf(4), expected);
  }

  @Test
  public void getResouceCount_InstanceFilter_ReturnsFilteredCount() {
    AuditInstance instance = AuditInstance.builder()
      .type(TYPE)
      .id(Integer.toString(INSTANCE_ID))
      .build();
    Long expected = serviceUnderTest.getResouceCount(null, instance);
    assertEquals(Long.valueOf(2), expected);
  }

  @Test
  public void audit_SnapShotPersistedWithAuthor() {
    EmployeeDto dto = createDto();
    serviceUnderTest.audit(dto);

    CdoSnapshot result = javers.getLatestSnapshot(dto.getId(), EmployeeDto.class).orElse(null);
    assertNotNull(result);
    assertEquals(DinaUserConfig.AUTH_USER_NAME, result.getCommitMetadata().getAuthor());
  }

  @Test
  public void auditDeleteEvent_TerminalSnapShotPersistedWithAuthor() {
    EmployeeDto dto = createDto();
    serviceUnderTest.audit(dto);
    serviceUnderTest.auditDeleteEvent(dto);

    CdoSnapshot result = javers.getLatestSnapshot(dto.getId(), EmployeeDto.class).orElse(null);
    assertNotNull(result);
    assertEquals(SnapshotType.TERMINAL, result.getType());
    assertEquals(DinaUserConfig.AUTH_USER_NAME, result.getCommitMetadata().getAuthor());
  }

  @Test
  void removeSnapshots() {
    EmployeeDto dto = createDto();
    serviceUnderTest.audit(dto);

    CdoSnapshot result = javers.getLatestSnapshot(dto.getId(), EmployeeDto.class).orElse(null);
    assertNotNull(result);

    serviceUnderTest.removeSnapshots(AuditInstance.builder()
      .type(TYPE)
      .id(Integer.toString(dto.getId()))
      .build());

    Assertions.assertTrue(javers.getLatestSnapshot(dto.getId(), EmployeeDto.class).isEmpty(),
      "There should be no more snapshots for this object");
  }

  private static EmployeeDto createDto() {
    EmployeeDto dto = new EmployeeDto();
    dto.setId(RandomUtils.nextInt());
    return dto;
  }

  private void cleanSnapShotRepo() {
    jdbcTemplate.update("DELETE FROM jv_snapshot where commit_fk IS NOT null", Collections.emptyMap());
    jdbcTemplate.update("DELETE FROM jv_commit where commit_pk IS NOT null", Collections.emptyMap());
  }

}
