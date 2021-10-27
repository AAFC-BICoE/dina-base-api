package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.security.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.crnk.core.resource.list.ResourceList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(classes = {
  TestDinaBaseApp.class, SimpleFilterResolverJsonbIT.DinaFilterResolverJsonbITConfig.class},
  properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(initializers = {PostgresTestContainerInitializer.class})
@ExtendWith(SpringExtension.class)
@Transactional
public class SimpleFilterResolverJsonbIT {

  private static final String KEY = "customKey";

  @Inject
  private DinaRepository<DinaFilterResolverJsonbITConfig.SubmarineDto,
    DinaFilterResolverJsonbITConfig.Submarine> subRepo;
  @Inject
  private DinaRepository<DinaFilterResolverJsonbITConfig.SubCommanderDto,
    DinaFilterResolverJsonbITConfig.SubCommander> commanderRepo;

  @Test
  void simpleFilter_NestedJsonbField() {
    String expectedValue = "CustomValue";
    DinaFilterResolverJsonbITConfig.SubmarineDto sub = createSub(newSub(expectedValue));
    DinaFilterResolverJsonbITConfig.SubmarineDto anotherSub = createSub(newSub("AnotherValue"));

    DinaFilterResolverJsonbITConfig.SubCommanderDto expectedCommander = commanderRepo.create(
      DinaFilterResolverJsonbITConfig.SubCommanderDto.builder().submarine(sub).build());
    commanderRepo.create(
      DinaFilterResolverJsonbITConfig.SubCommanderDto.builder().submarine(anotherSub).build());

    Assertions.assertEquals(
      2,
      commanderRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.SubCommanderDto.class)).size());

    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.SubCommanderDto.class);
    querySpec.addFilter(PathSpec.of("submarine", "jsonData", KEY).filter(FilterOperator.EQ, expectedValue));
    querySpec.includeRelation(PathSpec.of("submarine"));

    ResourceList<DinaFilterResolverJsonbITConfig.SubCommanderDto> results = commanderRepo.findAll(querySpec);
    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(expectedCommander.getUuid(), results.get(0).getUuid());
  }

  @Test
  void simpleFilter_FilterOnJsonB() {
    String expectedValue = "CustomValue";
    DinaFilterResolverJsonbITConfig.SubmarineDto dto = createSub(newSub(expectedValue));
    createSub(newSub("AnotherValue"));

    Assertions.assertEquals(
      2,
      subRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.SubmarineDto.class)).size());

    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.SubmarineDto.class);
    querySpec.addFilter(PathSpec.of("jsonData", KEY).filter(FilterOperator.EQ, expectedValue));

    ResourceList<DinaFilterResolverJsonbITConfig.SubmarineDto> results = subRepo.findAll(querySpec);
    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(dto.getUuid(), results.get(0).getUuid());
  }

  private DinaFilterResolverJsonbITConfig.SubmarineDto newSub(String expectedValue) {
    return DinaFilterResolverJsonbITConfig.SubmarineDto.builder()
      .jsonData(Map.of(KEY, expectedValue))
      .build();
  }

  private DinaFilterResolverJsonbITConfig.SubmarineDto createSub(DinaFilterResolverJsonbITConfig.SubmarineDto submarineDto) {
    DinaFilterResolverJsonbITConfig.SubmarineDto dto = subRepo.create(submarineDto);
    Assertions.assertEquals(dto.getUuid(), subRepo.findOne(
      dto.getUuid(), new QuerySpec(DinaFilterResolverJsonbITConfig.SubmarineDto.class)).getUuid());
    return dto;
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaFilterResolverJsonbITConfig.class)
  public static class DinaFilterResolverJsonbITConfig {

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Table(name = "commander")
    public static class SubCommander implements DinaEntity {
      private String createdBy;
      private OffsetDateTime createdOn;
      @Id
      private Integer id;
      @NaturalId
      private UUID uuid;
      @OneToOne
      private Submarine submarine;
    }

    @Data
    @JsonApiResource(type = "commander")
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(SubCommander.class)
    public static class SubCommanderDto {
      @JsonApiId
      private UUID uuid;
      @JsonApiRelation
      private SubmarineDto submarine;
    }

    @Bean
    public DinaRepository<SubCommanderDto, SubCommander> commanderRepo(
      BaseDAO baseDAO, SmartValidator val, BuildProperties props
    ) {
      return new DinaRepository<>(
        new DefaultDinaService<>(baseDAO, val) {
          @Override
          protected void preCreate(SubCommander entity) {
            entity.setId(RandomUtils.nextInt(0, 1000));
            entity.setUuid(UUID.randomUUID());
          }
        },
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(SubCommanderDto.class),
        SubCommanderDto.class,
        SubCommander.class,
        null,
        null,
        props);
    }

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
    @Table(name = "submarine")
    public static class Submarine implements DinaEntity {
      private String createdBy;
      private OffsetDateTime createdOn;
      @Id
      private Integer id;
      @NaturalId
      private UUID uuid;

      @Type(type = "jsonb")
      @Column(columnDefinition = "jsonb")
      private Map<String, String> jsonData;

    }

    @Data
    @JsonApiResource(type = "sub")
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(Submarine.class)
    public static class SubmarineDto {
      @JsonApiId
      private UUID uuid;
      private Map<String, String> jsonData;
    }

    @Bean
    public DinaRepository<SubmarineDto, Submarine> subRepo(
      BaseDAO baseDAO, SmartValidator val,
      BuildProperties props
    ) {
      return new DinaRepository<>(
        new DefaultDinaService<>(baseDAO, val) {
          @Override
          protected void preCreate(Submarine entity) {
            entity.setId(RandomUtils.nextInt(0, 1000));
            entity.setUuid(UUID.randomUUID());
          }
        },
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(SubmarineDto.class),
        SubmarineDto.class,
        Submarine.class,
        null,
        null,
        props);
    }

  }
}
