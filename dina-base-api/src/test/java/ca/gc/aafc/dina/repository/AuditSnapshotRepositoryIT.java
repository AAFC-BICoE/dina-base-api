package ca.gc.aafc.dina.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import javax.inject.Inject;

import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import org.apache.commons.lang3.RandomUtils;
import org.javers.core.Javers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.AuditSnapshotDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.repository.auditlog.AuditSnapshotRepository;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.PagedMetaInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toedter.spring.hateoas.jsonapi.JsonApiConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestDinaBaseApp.class, AuditSnapshotRepositoryIT.JsonApiConfigurationTestConfig.class}, properties = "dina.auditing.enabled = true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class AuditSnapshotRepositoryIT {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Inject
  private ObjectMapper objMapper;

  @Inject
  private AuditSnapshotRepository snapshotRepo;

  @Inject
  private Javers javers;

  @Inject
  private NamedParameterJdbcTemplate jdbcTemplate;

  private static final String AUTHOR = "dina_user";
  private static final String TYPE = EmployeeDto.TYPENAME;
  private static final Integer INSTANCE_ID = RandomUtils.nextInt();
  private static final String ANONYMOUS = "Anonymous";

  @BeforeEach
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();

    cleanSnapShotRepo();
    // Has Author 2 Commits
    EmployeeDto hasAuthor = createDto();
    javers.commit(AUTHOR, hasAuthor);
    hasAuthor.setName("update");
    javers.commit(AUTHOR, hasAuthor);

    // Anonymous Author 2 Commits
    EmployeeDto noAuthor = createDto();
    javers.commit(ANONYMOUS, noAuthor);
    noAuthor.setName("update");
    javers.commit(ANONYMOUS, noAuthor);

    // Has Author With specific instance id 2 commits
    EmployeeDto withInstanceID = createDto();
    withInstanceID.setId(INSTANCE_ID);
    javers.commit(AUTHOR, withInstanceID);
    withInstanceID.setName("update");
    javers.commit(AUTHOR, withInstanceID);
  }

  @Test
  public void findAll_whenNoFilter_allSnapshotsReturned() throws Exception {

    var findAllResponse = mockMvc.perform(
        get("/" + AuditSnapshotDto.TYPE_NAME)
      )
      .andExpect(status().isOk())
      .andReturn();


    String str = findAllResponse.getResponse().getContentAsString();

    int a;
//
//    ResourceList<AuditSnapshotDto> snapshots = snapshotRepo.findAll(null);
//    assertEquals(6, snapshots.size());
//    assertEquals(6, ((PagedMetaInformation) snapshots.getMeta()).getTotalResourceCount());
  }

//  @Test
//  public void findAll_whenFilteredByInstance_snapshotsFiltered() {
//    QuerySpec qs = new QuerySpec(AuditSnapshotDto.class);
//    qs.addFilter(filter("instanceId", TYPE + "/" + INSTANCE_ID));
//    ResourceList<AuditSnapshotDto> snapshots = snapshotRepo.findAll(qs);
//    assertEquals(2, snapshots.size());
//    assertEquals(2, ((PagedMetaInformation) snapshots.getMeta()).getTotalResourceCount());
//  }
//
//  @Test
//  public void findAll_whenFilteredByAuthor_snapshotsFiltered() {
//    QuerySpec qs1 = new QuerySpec(AuditSnapshotDto.class);
//    qs1.addFilter(filter("author", AUTHOR));
//    ResourceList<AuditSnapshotDto> snapshots1 = snapshotRepo.findAll(qs1);
//    assertEquals(4, snapshots1.size());
//    assertEquals(4, ((PagedMetaInformation) snapshots1.getMeta()).getTotalResourceCount());
//
//    QuerySpec qs2 = new QuerySpec(AuditSnapshotDto.class);
//    qs2.addFilter(filter("author", "other-user"));
//    ResourceList<AuditSnapshotDto> snapshots2 = snapshotRepo.findAll(qs2);
//    assertEquals(0, snapshots2.size());
//    assertEquals(0, ((PagedMetaInformation) snapshots2.getMeta()).getTotalResourceCount());
//  }
//
//  private FilterSpec filter(String attribute, String value) {
//    return new FilterSpec(PathSpec.of(attribute), FilterOperator.EQ, value);
//  }

  private static EmployeeDto createDto() {
    EmployeeDto dto = new EmployeeDto();
    dto.setId(RandomUtils.nextInt());
    return dto;
  }

  private void cleanSnapShotRepo() {
    jdbcTemplate.update("DELETE FROM jv_snapshot where commit_fk IS NOT null", Collections.emptyMap());
    jdbcTemplate.update("DELETE FROM jv_commit where commit_pk IS NOT null", Collections.emptyMap());
  }

  @TestConfiguration
  static class JsonApiConfigurationTestConfig {
    @Bean
    public JsonApiConfiguration jsonApiConfiguration() {
      return new JsonApiConfiguration()
        .withPluralizedTypeRendered(false)
        .withPageMetaAutomaticallyCreated(false)
        .withObjectMapperCustomizer(objectMapper -> {
          objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
          objectMapper.registerModule(new JavaTimeModule());
        });
    }
  }

}
