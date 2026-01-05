package ca.gc.aafc.dina.filter;

import org.apache.commons.lang3.RandomUtils;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.CarDriverDto;
import ca.gc.aafc.dina.dto.JsonbCarDto;
import ca.gc.aafc.dina.dto.JsonbMethodDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

@SpringBootTest(classes = {
  TestDinaBaseApp.class, SimpleFilterResolverJsonbV2IT.DinaFilterResolverJsonbITConfig.class})
@ContextConfiguration(initializers = {PostgresTestContainerInitializer.class})
@ExtendWith(SpringExtension.class)
@Transactional
public class SimpleFilterResolverJsonbV2IT {

  private static final String KEY = "customKey";

  @Inject
  private DinaRepositoryV2<JsonbCarDto, JsonbCar> carRepo;

  @Inject
  private DinaRepositoryV2<JsonbMethodDto, JsonbMethod> methodRepo;

  @Inject
  private DinaRepositoryV2<CarDriverDto, CarDriver> carDriverRepo;

  @Test
  void simpleFilter_NestedJsonbField() {
    String expectedValue = "CustomValue";
    JsonbCarDto car = createCar(newCar(expectedValue));
    JsonbCarDto anotherCar = createCar(newCar("AnotherValue"));

    JsonApiDocument car1ToCreate = JsonApiDocuments.createJsonApiDocument(
      null, JsonbCarDto.TYPENAME,
      JsonAPITestHelper.toAttributeMap(car)
    );
    JsonApiDocument car2ToCreate = JsonApiDocuments.createJsonApiDocument(
      null, JsonbCarDto.TYPENAME,
      JsonAPITestHelper.toAttributeMap(anotherCar)
    );

    var car1Uuid = carRepo.create(car1ToCreate, null).getDto().getUuid();
    var car2Uuid = carRepo.create(car2ToCreate, null).getDto().getUuid();


    var carDriverDto1 = createCarDriveWithCar(car1Uuid);
    var carDriverDto2 = createCarDriveWithCar(car2Uuid);

    assertEquals(2,
      carDriverRepo.getAll("").totalCount());

    var getAllResponse = carDriverRepo.getAll("filter[car.jsonData." + KEY + "]=" + expectedValue + "&include=car");
    assertEquals(1, getAllResponse .totalCount());
    assertEquals(carDriverDto1.getUuid(), getAllResponse.resourceList().getFirst().getDto().getUuid());
  }

  @Test
  void simpleFilter_FilterOnJsonB_jsonbDefinedOnField() {
    String expectedValue = "CustomValue";
    JsonbCarDto dto = createCar(newCar(expectedValue));
    createCar(newCar("AnotherValue"));

    assertEquals(2, carRepo.getAll("").totalCount());

    var getAllResponse = carRepo.getAll("filter[jsonData." + KEY + "]=" + expectedValue);
    assertEquals(1, getAllResponse.totalCount());
    assertEquals(dto.getUuid(), getAllResponse.resourceList().getFirst().getDto().getUuid());
  }

  @Test
  void simpleFilter_FilterOnJsonB_jsonbDefinedOnMethod() {
    String expectedValue = "CustomValue";
    JsonbMethodDto dto = createMethod(newMethod(expectedValue));
    createMethod(newMethod("AnotherValue"));

    assertEquals(2, methodRepo.getAll("").totalCount());

    var getAllResponse = methodRepo.getAll("filter[jsonData." + KEY + "]=" + expectedValue);
    assertEquals(1, getAllResponse.totalCount());
    assertEquals(dto.getUuid(), getAllResponse.resourceList().getFirst().getDto().getUuid());
  }

  @Test
  void simpleFilter_NestedJsonbKey() {
    String nestedKey = "nestedKey";
    String expectedValue = "CustomValue";

    JsonbCarDto expectedSub = newCar(expectedValue);
    expectedSub.setJsonData(Map.of(KEY, Map.of(nestedKey, expectedValue)));
    expectedSub = createCar(expectedSub);

    JsonbCarDto anotherSub = newCar("AnotherValue");
    anotherSub.setJsonData(Map.of(KEY, Map.of(nestedKey, "AnotherValue")));
    createCar(anotherSub);

    assertEquals(2, carRepo.getAll("").totalCount());

    var getAllResponse = carRepo.getAll("filter[jsonData." + KEY + "." + nestedKey + "]=" + expectedValue);
    assertEquals(1, getAllResponse.totalCount());
    assertEquals(expectedSub.getUuid(), getAllResponse.resourceList().getFirst().getDto().getUuid());
  }

  private JsonbCarDto newCar(String expectedValue) {
    return JsonbCarDto.builder()
      .jsonData(Map.of(KEY, expectedValue))
      .build();
  }

  private JsonbMethodDto newMethod(String expectedValue) {
    return JsonbMethodDto.builder()
      .jsonData(Map.of(KEY, expectedValue))
      .build();
  }

  private JsonbCarDto createCar(JsonbCarDto carDto) {
    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(
      null, JsonbCarDto.TYPENAME,
      JsonAPITestHelper.toAttributeMap(carDto)
    );
    return carRepo.create(docToCreate, null).getDto();
  }

  private CarDriverDto createCarDriveWithCar(UUID carUuid) {
    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocumentWithRelToOne(
      null, CarDriverDto.TYPENAME,
      JsonAPITestHelper.toAttributeMap(CarDriverDto.builder().build()),
      Map.of("car", JsonApiDocument.ResourceIdentifier.builder().id(carUuid)
        .type(JsonbCarDto.TYPENAME).build())
    );
    return carDriverRepo.create(docToCreate, null).getDto();
  }

  private JsonbMethodDto createMethod(JsonbMethodDto methodDto) {
    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(
      null, JsonbMethodDto.TYPENAME,
      JsonAPITestHelper.toAttributeMap(methodDto)
    );
    return methodRepo.create(docToCreate, null).getDto();
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaFilterResolverJsonbITConfig.class)
  public static class DinaFilterResolverJsonbITConfig {

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
