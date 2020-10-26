package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.ExternalResourceProviderImplementation;
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.queryspec.QuerySpec;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
public class DinaMappingLayerIT {

  @Inject
  private DinaService<Project> service;

  private DinaMappingLayer<ProjectDTO, Project> mappingLayer;

  @BeforeEach
  void setUp() {
    mappingLayer = new DinaMappingLayer<>(
      ProjectDTO.class, service, new DinaMapper<>(ProjectDTO.class));
  }

  @Test
  void mapEntitiesToDto_WhenNoRelationsIncluded_ExternalRelationsMapped() {
    Project entity1 = newProject();
    Project entity2 = newProject();

    QuerySpec querySpec = new QuerySpec(ProjectDTO.class);
    List<ProjectDTO> results = mappingLayer.mapEntitiesToDto(
      querySpec,
      Arrays.asList(entity1, entity2));

    assertProject(entity1, results.get(0));
    assertProject(entity2, results.get(1));
  }

  private void assertProject(Project entity, ProjectDTO result) {
    // Validate attributes
    Assertions.assertEquals(entity.getName(), result.getName());
    Assertions.assertEquals(entity.getUuid(), result.getUuid());
    Assertions.assertEquals(entity.getCreatedBy(), result.getCreatedBy());
    Assertions.assertTrue(entity.getCreatedOn().isEqual(result.getCreatedOn()));
    // Validate External Relation
    Assertions.assertEquals(
      entity.getAcMetaDataCreator().toString(),
      result.getAcMetaDataCreator().getId());
    Assertions.assertEquals(
      entity.getOriginalAuthor().toString(),
      result.getOriginalAuthor().getId());
  }

  private Project newProject() {
    return Project.builder()
      .uuid(UUID.randomUUID())
      .createdBy(RandomStringUtils.randomAlphabetic(5))
      .createdOn(OffsetDateTime.now())
      .name(RandomStringUtils.randomAlphabetic(5))
      .acMetaDataCreator(UUID.randomUUID())
      .originalAuthor(UUID.randomUUID())
      .build();
  }

  @TestConfiguration
  @Import(ExternalResourceProviderImplementation.class)
  static class DinaMappingLayerITITConfig {
    @Bean
    public DinaRepository<ProjectDTO, Project> projectRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      ExternalResourceProvider externalResourceProvider
    ) {
      return new DinaRepository<>(
        new DinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ProjectDTO.class),
        ProjectDTO.class,
        Project.class,
        filterResolver,
        externalResourceProvider
      );
    }

    @Bean
    public DinaRepository<TaskDTO, Task> taskRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver,
      ExternalResourceProvider externalResourceProvider
    ) {
      return new DinaRepository<>(
        new DinaService<>(baseDAO),
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(TaskDTO.class),
        TaskDTO.class,
        Task.class,
        filterResolver,
        externalResourceProvider
      );
    }
  }

}
