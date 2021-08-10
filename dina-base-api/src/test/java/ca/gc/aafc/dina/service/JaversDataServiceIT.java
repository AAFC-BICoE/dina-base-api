package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.EmployeeDto;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.QueryBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest(classes = TestDinaBaseApp.class, properties = "dina.auditing.enabled = true")
class JaversDataServiceIT {

  private static final String AUTHOR = "dina_user";
  private static final Integer INSTANCE_ID = RandomUtils.nextInt();
  private static final String TYPE = EmployeeDto.TYPENAME;

  @Inject
  private JaversDataService javersDataService;

  @Inject
  private Javers javers;

  @Inject
  private NamedParameterJdbcTemplate jdbcTemplate;

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
  void getResourceCount_NoFilters() {
    Assertions.assertEquals(6, javersDataService.getResourceCount(null, null, null));
  }

  @Test
  void getResourceCount_FilterByInstanceId() {
    Assertions.assertEquals(
      2,
      javersDataService.getResourceCount(Integer.toString(INSTANCE_ID), TYPE, null));
  }

  @Test
  void getResourceCount_FilterByAuthor() {
    Assertions.assertEquals(
      4,
      javersDataService.getResourceCount(null, null, AUTHOR));
  }

  @Test
  void removeSnapshots() {
    EmployeeDto dto = createDto();
    javers.commit(AUTHOR, dto);
    dto.setName(RandomStringUtils.randomAlphabetic(4));
    javers.commit(AUTHOR, dto);
    String id = Integer.toString(dto.getId());

    List<CdoSnapshot> snapshots = javers.findSnapshots(QueryBuilder.byInstanceId(id, TYPE).build());
    Assertions.assertEquals(2, snapshots.size());

    javersDataService.removeSnapshots(snapshots.stream()
      .map(c -> c.getCommitId().valueAsNumber())
      .collect(Collectors.toList()), id, TYPE);

    Assertions.assertEquals(
      0,
      javers.findSnapshots(QueryBuilder.byInstanceId(id, TYPE).build()).size());
    Assertions.assertTrue(
      javers.getLatestSnapshot(dto.getId(), EmployeeDto.class).isEmpty(),
      "There should be no more snapshots for this object");
  }

  private void cleanSnapShotRepo() {
    jdbcTemplate.update("DELETE FROM jv_snapshot where commit_fk IS NOT null", Collections.emptyMap());
    jdbcTemplate.update("DELETE FROM jv_commit where commit_pk IS NOT null", Collections.emptyMap());
  }

  private static EmployeeDto createDto() {
    EmployeeDto dto = new EmployeeDto();
    dto.setId(RandomUtils.nextInt());
    return dto;
  }

}
