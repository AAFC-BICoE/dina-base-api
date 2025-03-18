package ca.gc.aafc.dina.repository;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.Javers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toedter.spring.hateoas.jsonapi.JsonApiConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.AuditSnapshotDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.json.JsonHelper;
import ca.gc.aafc.dina.jsonapi.JSONApiDocumentStructure;
import ca.gc.aafc.dina.repository.auditlog.AuditSnapshotRepository;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

@SpringBootTest(classes = {TestDinaBaseApp.class, AuditSnapshotRepositoryIT.JsonApiConfigurationTestConfig.class}, properties = "dina.auditing.enabled = true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class AuditSnapshotRepositoryIT {

  private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE_REF = new TypeReference<>() { };
  private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() { };

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
    assertAuditEndpoint(null, null, 6);
  }

  @Test
  public void findAll_whenFilteredByInstance_snapshotsFiltered() throws Exception {
    assertAuditEndpoint("filter[instanceId]", TYPE + "/" + INSTANCE_ID, 2);
  }

  @Test
  public void findAll_whenFilteredByAuthor_snapshotsFiltered() throws Exception {
    assertAuditEndpoint("filter[author]", AUTHOR, 4);
    assertAuditEndpoint("filter[author]", "other-user", 0);
  }

  private void assertAuditEndpoint(String filterStr, String filterValue, int expectedCount) throws Exception {

    var requestBuilder = get("/" + AuditSnapshotDto.TYPE_NAME);

    if (StringUtils.isNotBlank(filterStr)) {
      requestBuilder.queryParam(filterStr, filterValue);
    }

    var findAllResponse = mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andReturn();

    JsonNode
      docs2 = objMapper.readValue(findAllResponse.getResponse().getContentAsString(), JsonNode.class);

    Optional<JsonNode> dataNode2 = JsonHelper.atJsonPtr(docs2, JSONApiDocumentStructure.DATA_PTR);
    Optional<JsonNode> metaNode2 = JsonHelper.atJsonPtr(docs2, JSONApiDocumentStructure.META_PTR);

    List<Map<String, Object>> documents2 = objMapper.readerFor(LIST_MAP_TYPE_REF)
      .readValue(dataNode2.get());

    Map<String, Object> metaBlock2 = objMapper.readerFor(MAP_TYPE_REF)
      .readValue(metaNode2.get());

    assertEquals(expectedCount, documents2.size());
    assertEquals(expectedCount, metaBlock2.get("totalResourceCount"));
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
