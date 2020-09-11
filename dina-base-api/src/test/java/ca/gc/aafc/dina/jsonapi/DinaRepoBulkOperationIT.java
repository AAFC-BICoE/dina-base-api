package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIOperationBuilder;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.crnk.core.engine.http.HttpMethod;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.annotations.NaturalId;
import org.javers.core.metamodel.annotation.PropertyName;
import org.javers.core.metamodel.annotation.TypeName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class DinaRepoBulkOperationIT extends BaseRestAssuredTest {

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

  @Test
  void bulkPost() {
    ProjectDTO project1 = createProjectDTO();
    ProjectDTO project2 = createProjectDTO();
    Map<String, Object> project1Map = projectToMap(project1, UUID.randomUUID().toString());
    Map<String, Object> project2Map = projectToMap(project2, UUID.randomUUID().toString());

    List<Map<String, Object>> operationMap = JsonAPIOperationBuilder.newBuilder()
      .addOperation(HttpMethod.POST, ProjectDTO.RESOURCE_TYPE, project1Map)
      .addOperation(HttpMethod.POST, ProjectDTO.RESOURCE_TYPE, project2Map)
      .buildOperation();

    ValidatableResponse operationResponse = sendOperation(operationMap);

    Integer returnCodePerson1 = operationResponse.extract().body().jsonPath().getInt("[0].status");
    String project1ID = operationResponse.extract().body().jsonPath().getString("[0].data.id");

    Integer returnCodePerson2 = operationResponse.extract().body().jsonPath().getInt("[1].status");
    String project2ID = operationResponse.extract().body().jsonPath().getString("[1].data.id");

    Assertions.assertEquals(201, returnCodePerson1);
    Assertions.assertEquals(201, returnCodePerson2);
    assertPersonFromResponse(sendGet(ProjectDTO.RESOURCE_TYPE, project1ID, 200), project1);
    assertPersonFromResponse(sendGet(ProjectDTO.RESOURCE_TYPE, project2ID, 200), project2);
  }

  private static void assertPersonFromResponse(ValidatableResponse response, ProjectDTO dto) {
    JsonPath jsonPath = response.extract().body().jsonPath();
    Assertions.assertEquals(dto.name, jsonPath.getString("data.attributes.name"));
  }

  private static Map<String, Object> projectToMap(ProjectDTO project, String id) {
    return JsonAPITestHelper.toJsonAPIMap(
      ProjectDTO.RESOURCE_TYPE,
      JsonAPITestHelper.toAttributeMap(project),
      null,
      id);
  }

  private static ProjectDTO createProjectDTO() {
    return ProjectDTO.builder()
      .name(RandomStringUtils.randomAlphabetic(5))
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
