package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.CarDriver;
import ca.gc.aafc.dina.entity.JsonbCar;
import ca.gc.aafc.dina.entity.JsonbMethod;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.security.auth.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SpringBootTest(classes = {
  TestDinaBaseApp.class, SimpleFilterResolverJsonbIT.DinaFilterResolverJsonbITConfig.class})
@ContextConfiguration(initializers = {PostgresTestContainerInitializer.class})
@ExtendWith(SpringExtension.class)
@Transactional
public class SimpleFilterResolverJsonbIT {

  private static final String KEY = "customKey";

  @Inject
  private DinaRepository<DinaFilterResolverJsonbITConfig.JsonbCarDto, JsonbCar> carRepo;

  @Inject
  private DinaRepository<DinaFilterResolverJsonbITConfig.JsonbMethodDto, JsonbMethod> methodRepo;

  @Inject
  private DinaRepository<DinaFilterResolverJsonbITConfig.CarDriverDto, CarDriver> commanderRepo;

  @Test
  void simpleFilter_NestedJsonbField() {
    String expectedValue = "CustomValue";
    DinaFilterResolverJsonbITConfig.JsonbCarDto sub = createCar(newCar(expectedValue));
    DinaFilterResolverJsonbITConfig.JsonbCarDto anotherSub = createCar(newCar("AnotherValue"));

    DinaFilterResolverJsonbITConfig.CarDriverDto expectedCommander = commanderRepo.create(
      DinaFilterResolverJsonbITConfig.CarDriverDto.builder().car(sub).build());
    commanderRepo.create(
      DinaFilterResolverJsonbITConfig.CarDriverDto.builder().car(anotherSub).build());

    Assertions.assertEquals(
      2,
      commanderRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.CarDriverDto.class)).size());

    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.CarDriverDto.class);
    querySpec.addFilter(PathSpec.of("car", "jsonData", KEY).filter(FilterOperator.EQ, expectedValue));
    querySpec.includeRelation(PathSpec.of("car"));

    ResourceList<DinaFilterResolverJsonbITConfig.CarDriverDto> results = commanderRepo.findAll(querySpec);
    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(expectedCommander.getUuid(), results.get(0).getUuid());
  }

  @Test
  void simpleFilter_FilterOnJsonB_jsonbDefinedOnField() {
    String expectedValue = "CustomValue";
    DinaFilterResolverJsonbITConfig.JsonbCarDto dto = createCar(newCar(expectedValue));
    createCar(newCar("AnotherValue"));

    Assertions.assertEquals(
      2,
      carRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class)).size());

    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class);
    querySpec.addFilter(PathSpec.of("jsonData", KEY).filter(FilterOperator.EQ, expectedValue));

    ResourceList<DinaFilterResolverJsonbITConfig.JsonbCarDto> results = carRepo.findAll(querySpec);
    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(dto.getUuid(), results.get(0).getUuid());
  }

  @Test
  void simpleFilter_FilterOnJsonB_jsonbDefinedOnMethod() {
    String expectedValue = "CustomValue";
    DinaFilterResolverJsonbITConfig.JsonbMethodDto dto = createMethod(newMethod(expectedValue));
    createMethod(newMethod("AnotherValue"));

    Assertions.assertEquals(
      2,
      methodRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbMethodDto.class)).size());

    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbMethodDto.class);
    querySpec.addFilter(PathSpec.of("jsonData", KEY).filter(FilterOperator.EQ, expectedValue));

    ResourceList<DinaFilterResolverJsonbITConfig.JsonbMethodDto> results = methodRepo.findAll(querySpec);
    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(dto.getUuid(), results.get(0).getUuid());
  }

  @Test
  void simpleFilter_NestedJsonbKey() {
    String nestedKey = "nestedKey";
    String expectedValue = "CustomValue";

    DinaFilterResolverJsonbITConfig.JsonbCarDto expectedSub = newCar(expectedValue);
    expectedSub.setJsonData(Map.of(KEY, Map.of(nestedKey, expectedValue)));
    expectedSub = createCar(expectedSub);

    DinaFilterResolverJsonbITConfig.JsonbCarDto anotherSub = newCar("AnotherValue");
    anotherSub.setJsonData(Map.of(KEY, Map.of(nestedKey, "AnotherValue")));
    createCar(anotherSub);

    Assertions.assertEquals(
      2,
      carRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class)).size());

    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class);
    querySpec.addFilter(PathSpec.of("jsonData", KEY, nestedKey).filter(FilterOperator.EQ, expectedValue));

    ResourceList<DinaFilterResolverJsonbITConfig.JsonbCarDto> results = carRepo.findAll(querySpec);
    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals(expectedSub.getUuid(), results.get(0).getUuid());
  }

  private DinaFilterResolverJsonbITConfig.JsonbCarDto newCar(String expectedValue) {
    return DinaFilterResolverJsonbITConfig.JsonbCarDto.builder()
      .jsonData(Map.of(KEY, expectedValue))
      .build();
  }

  private DinaFilterResolverJsonbITConfig.JsonbMethodDto newMethod(String expectedValue) {
    return DinaFilterResolverJsonbITConfig.JsonbMethodDto.builder()
      .jsonData(Map.of(KEY, expectedValue))
      .build();
  }

  private DinaFilterResolverJsonbITConfig.JsonbCarDto createCar(DinaFilterResolverJsonbITConfig.JsonbCarDto methodDto) {
    DinaFilterResolverJsonbITConfig.JsonbCarDto dto = carRepo.create(methodDto);
    Assertions.assertEquals(dto.getUuid(), carRepo.findOne(
      dto.getUuid(), new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class)).getUuid());
    return dto;
  }

  private DinaFilterResolverJsonbITConfig.JsonbMethodDto createMethod(DinaFilterResolverJsonbITConfig.JsonbMethodDto methodDto) {
    DinaFilterResolverJsonbITConfig.JsonbMethodDto dto = methodRepo.create(methodDto);
    Assertions.assertEquals(dto.getUuid(), methodRepo.findOne(
      dto.getUuid(), new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbMethodDto.class)).getUuid());
    return dto;
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaFilterResolverJsonbITConfig.class)
  public static class DinaFilterResolverJsonbITConfig {


    @Data
    @JsonApiResource(type = "car-driver")
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(CarDriver.class)
    public static class CarDriverDto {
      @JsonApiId
      private UUID uuid;
      @JsonApiRelation
      private JsonbCarDto car;
    }

    @Bean
    public DinaRepository<CarDriverDto, CarDriver> carDriverRepo(
      BaseDAO baseDAO, SmartValidator val, BuildProperties props, ObjectMapper objMapper
    ) {
      return new DinaRepository<>(
        new DefaultDinaService<>(baseDAO, val) {
          @Override
          protected void preCreate(CarDriver entity) {
            entity.setId(RandomUtils.nextInt(0, 1000));
            entity.setUuid(UUID.randomUUID());
          }
        },
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(CarDriverDto.class),
              CarDriverDto.class,
              CarDriver.class,
        null,
        null,
        props, objMapper);
    }

    @Data
    @JsonApiResource(type = "jsonbcar")
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(JsonbCar.class)
    public static class JsonbCarDto {
      @JsonApiId
      private UUID uuid;
      private Map<String, Object> jsonData;
      private Map<String, Object> jsonDataMethodDefined;
    }

    @Bean
    public DinaRepository<JsonbCarDto, JsonbCar> jsonbCarRepo(
      BaseDAO baseDAO, SmartValidator val,
      BuildProperties props, ObjectMapper objMapper
    ) {
      return new DinaRepository<>(
        new DefaultDinaService<>(baseDAO, val) {
          @Override
          protected void preCreate(JsonbCar entity) {
            entity.setId(RandomUtils.nextInt(0, 1000));
            entity.setUuid(UUID.randomUUID());
          }
        },
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(JsonbCarDto.class),
              JsonbCarDto.class,
              JsonbCar.class,
        null,
        null,
        props, objMapper);
    }

    @Data
    @JsonApiResource(type = "jsonb-method")
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(JsonbMethod.class)
    public static class JsonbMethodDto {
      @JsonApiId
      private UUID uuid;
      private Map<String, Object> jsonData;
    }

    @Bean
    public DinaRepository<JsonbMethodDto, JsonbMethod> jsonbMethodRepo(
      BaseDAO baseDAO, SmartValidator val,
      BuildProperties props, ObjectMapper objMapper
    ) {
      return new DinaRepository<>(
        new DefaultDinaService<>(baseDAO, val) {
          @Override
          protected void preCreate(JsonbMethod entity) {
            entity.setId(RandomUtils.nextInt(0, 1000));
            entity.setUuid(UUID.randomUUID());
          }
        },
        new AllowAllAuthorizationService(),
        Optional.empty(),
        new DinaMapper<>(JsonbMethodDto.class),
              JsonbMethodDto.class,
              JsonbMethod.class,
        null,
        null,
        props, objMapper);
    }

  }
}
