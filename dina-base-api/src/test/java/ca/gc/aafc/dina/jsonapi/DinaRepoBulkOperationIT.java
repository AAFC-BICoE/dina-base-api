package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import io.crnk.client.CrnkClient;
import io.crnk.client.http.inmemory.InMemoryHttpAdapter;
import io.crnk.core.boot.CrnkBoot;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.crnk.operations.client.OperationsCall;
import io.crnk.operations.client.OperationsClient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.annotations.NaturalId;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.inject.Inject;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class DinaRepoBulkOperationIT extends BaseRestAssuredTest {

  @Inject
  private CrnkBoot boot;
  @Inject
  private DinaRepository<ProjectDTO, Project> projectRepo;

  private OperationsClient operationsClient;

  public DinaRepoBulkOperationIT() {
    super("");
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaRepoBulkOperationIT.class)
  static class DinaRepoBulkOperationITConfig {
    @Bean
    public DinaRepository<ProjectDTO, Project> projectRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver
    ) {
      return new DinaRepository<>(
        new DinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ProjectDTO.class),
        ProjectDTO.class,
        Project.class,
        filterResolver
      );
    }
  }

  @BeforeEach
  void setUp() {
    String url = "http://localhost:8080/" + super.basePath;
    CrnkClient client = new CrnkClient(url);
    operationsClient = new OperationsClient(client);
    client.setHttpAdapter(new InMemoryHttpAdapter(boot, url));
  }

  @AfterEach
  void tearDown() {
    //Clean up test data
    projectRepo.findAll(new QuerySpec(ProjectDTO.class))
      .forEach(projectDTO -> projectRepo.delete(projectDTO.getUuid()));
  }

  @Test
  void bulkPost() {
    ProjectDTO project1 = createProjectDTO();
    ProjectDTO project2 = createProjectDTO();

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1);
    call.add(HttpMethod.POST, project2);
    call.execute();

    Assertions.assertEquals(2, projectRepo.findAll(new QuerySpec(ProjectDTO.class)).size());
    assertProject(
      project1,
      projectRepo.findOne(project1.getUuid(), new QuerySpec(ProjectDTO.class)));
    assertProject(
      project2,
      projectRepo.findOne(project2.getUuid(), new QuerySpec(ProjectDTO.class)));
  }

  @Test
  void bulkDelete() {
    ProjectDTO project1 = createProjectDTO();
    ProjectDTO project2 = createProjectDTO();

    OperationsCall call = operationsClient.createCall();
    call.add(HttpMethod.POST, project1);
    call.add(HttpMethod.POST, project2);
    call.execute();

    Assertions.assertEquals(2, projectRepo.findAll(new QuerySpec(ProjectDTO.class)).size());

    call = operationsClient.createCall();
    call.add(HttpMethod.DELETE, project1);
    call.add(HttpMethod.DELETE, project2);
    call.execute();

    Assertions.assertEquals(0, projectRepo.findAll(new QuerySpec(ProjectDTO.class)).size());
  }

  private void assertProject(ProjectDTO expected, ProjectDTO result) {
    Assertions.assertEquals(expected.getName(), result.getName());
  }

  private static ProjectDTO createProjectDTO() {
    return ProjectDTO.builder()
      .name(RandomStringUtils.randomAlphabetic(5))
      .uuid(UUID.randomUUID())
      .build();
  }

  @Data
  @Entity
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class Project implements DinaEntity {
    @Id
    @GeneratedValue
    private Integer id;
    @NaturalId
    private UUID uuid;
    private String name;
    private OffsetDateTime createdOn;
    private String createdBy;
  }

  @Data
  @JsonApiResource(type = ProjectDTO.RESOURCE_TYPE)
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(Project.class)
  @TypeName(ProjectDTO.RESOURCE_TYPE)
  public static final class ProjectDTO {
    public static final String RESOURCE_TYPE = "Project";
    @JsonApiId
    @org.javers.core.metamodel.annotation.Id
    @PropertyName("id")
    private UUID uuid;
    private String name;
  }

}
