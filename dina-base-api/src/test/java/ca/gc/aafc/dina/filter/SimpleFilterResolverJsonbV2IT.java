package ca.gc.aafc.dina.filter;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.CarDriver;
import ca.gc.aafc.dina.entity.JsonbCar;
import ca.gc.aafc.dina.entity.JsonbMethod;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.mapper.CarDriverMapper;
import ca.gc.aafc.dina.mapper.JsonbCarMapper;
import ca.gc.aafc.dina.mapper.JsonbMethodMapper;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.security.auth.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.list.ResourceList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {
  TestDinaBaseApp.class, SimpleFilterResolverJsonbV2IT.DinaFilterResolverJsonbITConfig.class})
@ContextConfiguration(initializers = {PostgresTestContainerInitializer.class})
@ExtendWith(SpringExtension.class)
@Transactional
public class SimpleFilterResolverJsonbV2IT {

  private static final String KEY = "customKey";

  @Inject
  private DinaRepositoryV2<DinaFilterResolverJsonbITConfig.JsonbCarDto, JsonbCar> carRepo;

  @Inject
  private DinaRepositoryV2<DinaFilterResolverJsonbITConfig.JsonbMethodDto, JsonbMethod> methodRepo;

  @Inject
  private DinaRepositoryV2<DinaFilterResolverJsonbITConfig.CarDriverDto, CarDriver> commanderRepo;

  @Test
  void simpleFilter_NestedJsonbField() {
    String expectedValue = "CustomValue";
    DinaFilterResolverJsonbITConfig.JsonbCarDto car = createCar(newCar(expectedValue));
    DinaFilterResolverJsonbITConfig.JsonbCarDto anotherCar = createCar(newCar("AnotherValue"));

    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(
      null, DinaFilterResolverJsonbITConfig.CarDriverDto.TYPENAME,
      JsonAPITestHelper.toAttributeMap(DinaFilterResolverJsonbITConfig.CarDriverDto.builder().car(car).build())
    );

    JsonApiDocument docToCreate2 = JsonApiDocuments.createJsonApiDocument(
      null, DinaFilterResolverJsonbITConfig.CarDriverDto.TYPENAME,
      JsonAPITestHelper.toAttributeMap(DinaFilterResolverJsonbITConfig.CarDriverDto.builder().car(anotherCar).build())
    );

    var response1 = commanderRepo.create(docToCreate, null);
    var response2 = commanderRepo.create(docToCreate2, null);

    assertEquals(2,
      commanderRepo.getAll("").totalCount());

    assertEquals(1,
    commanderRepo.getAll("filter[car.jsonData.customKey]="+expectedValue + "&include=car").totalCount());
//
//    ResourceList<DinaFilterResolverJsonbITConfig.CarDriverDto> results = commanderRepo.findAll(querySpec);
//    assertEquals(1, results.size());
//    assertEquals(expectedCommander.getUuid(), results.get(0).getUuid());
  }

//  @Test
//  void simpleFilter_FilterOnJsonB_jsonbDefinedOnField() {
//    String expectedValue = "CustomValue";
//    DinaFilterResolverJsonbITConfig.JsonbCarDto dto = createCar(newCar(expectedValue));
//    createCar(newCar("AnotherValue"));
//
//    assertEquals(
//      2,
//      carRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class)).size());
//
//    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class);
//    querySpec.addFilter(PathSpec.of("jsonData", KEY).filter(FilterOperator.EQ, expectedValue));
//
//    ResourceList<DinaFilterResolverJsonbITConfig.JsonbCarDto> results = carRepo.findAll(querySpec);
//    assertEquals(1, results.size());
//    assertEquals(dto.getUuid(), results.get(0).getUuid());
//  }
//
//  @Test
//  void simpleFilter_FilterOnJsonB_jsonbDefinedOnMethod() {
//    String expectedValue = "CustomValue";
//    DinaFilterResolverJsonbITConfig.JsonbMethodDto dto = createMethod(newMethod(expectedValue));
//    createMethod(newMethod("AnotherValue"));
//
//    assertEquals(
//      2,
//      methodRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbMethodDto.class)).size());
//
//    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbMethodDto.class);
//    querySpec.addFilter(PathSpec.of("jsonData", KEY).filter(FilterOperator.EQ, expectedValue));
//
//    ResourceList<DinaFilterResolverJsonbITConfig.JsonbMethodDto> results = methodRepo.findAll(querySpec);
//    assertEquals(1, results.size());
//    assertEquals(dto.getUuid(), results.get(0).getUuid());
//  }
//
//  @Test
//  void simpleFilter_NestedJsonbKey() {
//    String nestedKey = "nestedKey";
//    String expectedValue = "CustomValue";
//
//    DinaFilterResolverJsonbITConfig.JsonbCarDto expectedSub = newCar(expectedValue);
//    expectedSub.setJsonData(Map.of(KEY, Map.of(nestedKey, expectedValue)));
//    expectedSub = createCar(expectedSub);
//
//    DinaFilterResolverJsonbITConfig.JsonbCarDto anotherSub = newCar("AnotherValue");
//    anotherSub.setJsonData(Map.of(KEY, Map.of(nestedKey, "AnotherValue")));
//    createCar(anotherSub);
//
//    assertEquals(
//      2,
//      carRepo.findAll(new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class)).size());
//
//    QuerySpec querySpec = new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbCarDto.class);
//    querySpec.addFilter(PathSpec.of("jsonData", KEY, nestedKey).filter(FilterOperator.EQ, expectedValue));
//
//    ResourceList<DinaFilterResolverJsonbITConfig.JsonbCarDto> results = carRepo.findAll(querySpec);
//    assertEquals(1, results.size());
//    assertEquals(expectedSub.getUuid(), results.get(0).getUuid());
//  }

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

  private DinaFilterResolverJsonbITConfig.JsonbCarDto createCar(DinaFilterResolverJsonbITConfig.JsonbCarDto carDto) {
    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(
      null, DinaFilterResolverJsonbITConfig.JsonbCarDto.TYPENAME,
      JsonAPITestHelper.toAttributeMap(carDto)
    );
    return carRepo.create(docToCreate, null).getDto();
  }

//
//  private DinaFilterResolverJsonbITConfig.JsonbMethodDto createMethod(DinaFilterResolverJsonbITConfig.JsonbMethodDto methodDto) {
//    DinaFilterResolverJsonbITConfig.JsonbMethodDto dto = methodRepo.create(methodDto);
//    assertEquals(dto.getUuid(), methodRepo.findOne(
//      dto.getUuid(), new QuerySpec(DinaFilterResolverJsonbITConfig.JsonbMethodDto.class)).getUuid());
//    return dto;
//  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaFilterResolverJsonbITConfig.class)
  public static class DinaFilterResolverJsonbITConfig {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(CarDriver.class)
    @JsonApiTypeForClass(CarDriverDto.TYPENAME)
    public static class CarDriverDto implements ca.gc.aafc.dina.dto.JsonApiResource {

      static final String TYPENAME = "car-driver";

      @JsonApiId
      private UUID uuid;

      @JsonApiRelation
      private JsonbCarDto car;

      @Override
      @JsonIgnore
      public String getJsonApiType() {
        return TYPENAME;
      }

      @Override
      @JsonIgnore
      public UUID getJsonApiId() {
        return uuid;
      }
    }

    @Bean
    public DinaRepositoryV2<CarDriverDto, CarDriver> carDriverRepo(
      BaseDAO baseDAO, SmartValidator val, BuildProperties props, ObjectMapper objMapper
    ) {
      return new DinaRepositoryV2<CarDriverDto, CarDriver>(
        new DefaultDinaService<CarDriver>(baseDAO, val) {
          @Override
          protected void preCreate(CarDriver entity) {
            entity.setId(RandomUtils.nextInt(0, 1000));
            entity.setUuid(UUID.randomUUID());
          }
        },
        new AllowAllAuthorizationService(),
        Optional.empty(),
        CarDriverMapper.INSTANCE,
        CarDriverDto.class,
        CarDriver.class,
        props, objMapper);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(JsonbCar.class)
    @JsonApiTypeForClass(JsonbCarDto.TYPENAME)
    public static class JsonbCarDto implements ca.gc.aafc.dina.dto.JsonApiResource {

      static final String TYPENAME = "jsonbcar";

      @JsonApiId
      private UUID uuid;
      private Map<String, Object> jsonData;
      private Map<String, Object> jsonDataMethodDefined;

      @Override
      @JsonIgnore
      public String getJsonApiType() {
        return TYPENAME;
      }

      @Override
      @JsonIgnore
      public UUID getJsonApiId() {
        return uuid;
      }
    }

    @Bean
    public DinaRepositoryV2<JsonbCarDto, JsonbCar> jsonbCarRepo(
      BaseDAO baseDAO, SmartValidator val,
      BuildProperties props, ObjectMapper objMapper
    ) {
      return new DinaRepositoryV2<>(
        new DefaultDinaService<>(baseDAO, val) {
          @Override
          protected void preCreate(JsonbCar entity) {
            entity.setId(RandomUtils.nextInt(0, 1000));
            entity.setUuid(UUID.randomUUID());
          }
        },
        new AllowAllAuthorizationService(),
        Optional.empty(),
        JsonbCarMapper.INSTANCE,
        JsonbCarDto.class,
        JsonbCar.class,
        props, objMapper);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @RelatedEntity(JsonbMethod.class)
    @JsonApiTypeForClass(JsonbMethodDto.TYPENAME)
    public static class JsonbMethodDto implements ca.gc.aafc.dina.dto.JsonApiResource {

      static final String TYPENAME = "jsonb-method";

      @JsonApiId
      private UUID uuid;
      private Map<String, Object> jsonData;

      @Override
      @JsonIgnore
      public String getJsonApiType() {
        return TYPENAME;
      }

      @Override
      @JsonIgnore
      public UUID getJsonApiId() {
        return uuid;
      }
    }

    @Bean
    public DinaRepositoryV2<JsonbMethodDto, JsonbMethod> jsonbMethodRepo(
      BaseDAO baseDAO, SmartValidator val,
      BuildProperties props, ObjectMapper objMapper
    ) {
      return new DinaRepositoryV2<>(
        new DefaultDinaService<>(baseDAO, val) {
          @Override
          protected void preCreate(JsonbMethod entity) {
            entity.setId(RandomUtils.nextInt(0, 1000));
            entity.setUuid(UUID.randomUUID());
          }
        },
        new AllowAllAuthorizationService(),
        Optional.empty(),
        JsonbMethodMapper.INSTANCE,
              JsonbMethodDto.class,
              JsonbMethod.class,
        props, objMapper);
    }

  }
}
