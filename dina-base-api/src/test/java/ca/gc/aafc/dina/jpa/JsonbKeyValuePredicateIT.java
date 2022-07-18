package ca.gc.aafc.dina.jpa;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import ca.gc.aafc.dina.entity.JsonbCar;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;

@SpringBootTest(classes = {TestDinaBaseApp.class, JsonbKeyValuePredicateIT.JsonbPredicateITConfig.class})
@ContextConfiguration(initializers = PostgresTestContainerInitializer.class)
@ExtendWith(SpringExtension.class)
@Transactional
public class JsonbKeyValuePredicateIT {

  private static final String KEY = "customKey";

  @Inject
  private BaseDAO baseDAO;

  @Test
  void jsonb_predicate_building() throws JsonProcessingException {
    String expectedValue = "CustomValue";

    JsonbCar car = newCar(expectedValue);
    JsonbCar another = newCar("another");

    persistCar(expectedValue, car);
    persistCar("another", another);

    CriteriaBuilder builder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<JsonbCar> criteria = builder.createQuery(JsonbCar.class);
    Root<JsonbCar> root = criteria.from(JsonbCar.class);

    Predicate predicate = JsonbKeyValuePredicate.onKey("jsonData", KEY)
        .buildUsing(root, builder, expectedValue, true);

    criteria.where(predicate).select(root);
    List<JsonbCar> resultList = baseDAO.resultListFromCriteria(criteria, 0, 20);

    Assertions.assertEquals(1, resultList.size());
    Assertions.assertEquals(car.getUuid(), resultList.get(0).getUuid());
    Assertions.assertEquals(expectedValue, resultList.get(0).getJsonData().get(KEY));
  }

  @Test
  void jsonb_predicate_building_caseInsensitive() throws JsonProcessingException {
    String expectedValue = "CustomValue";

    // Test using the expected value but in upper case.
    JsonbCar car = newCar(expectedValue.toUpperCase());
    JsonbCar another = newCar("another");

    persistCar(expectedValue, car);
    persistCar("another", another);
  
    CriteriaBuilder builder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<JsonbCar> criteria = builder.createQuery(JsonbCar.class);
    Root<JsonbCar> root = criteria.from(JsonbCar.class);

    // Test with case sensitive turned off. 
    Predicate predicate = JsonbKeyValuePredicate.onKey("jsonData", KEY)
        .buildUsing(root, builder, expectedValue, false);

    criteria.where(predicate).select(root);
    List<JsonbCar> resultList = baseDAO.resultListFromCriteria(criteria, 0, 20);
  
    Assertions.assertEquals(1, resultList.size());
    Assertions.assertEquals(car.getUuid(), resultList.get(0).getUuid());
    Assertions.assertEquals(expectedValue.toUpperCase(), resultList.get(0).getJsonData().get(KEY));
  }

  @Test
  void jsonb_arrayPredicate_building() throws JsonProcessingException {
    String expectedValue = "CustomValue";

    JsonbCar car = newCar(expectedValue);
    JsonbCar another = newCar("another");

    persistCar(expectedValue, car);
    persistCar("another", another);

    CriteriaBuilder builder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<JsonbCar> criteria = builder.createQuery(JsonbCar.class);
    Root<JsonbCar> root = criteria.from(JsonbCar.class);

    Predicate predicate = JsonbKeyValuePredicate.onKey("jsonListData", "value")
        .buildUsing(root, builder, expectedValue, true);

    criteria.where(predicate).select(root);
    List<JsonbCar> resultList = baseDAO.resultListFromCriteria(criteria, 0, 20);

    Assertions.assertEquals(1, resultList.size());
    Assertions.assertEquals(car.getUuid(), resultList.get(0).getUuid());
    Assertions.assertEquals(expectedValue, resultList.get(0).getJsonListData().get(0).getValue());
  }

  private void persistCar(String expectedValue, JsonbCar car) {
    baseDAO.create(car);
    JsonbCar result = baseDAO.findOneByNaturalId(
            car.getUuid(), JsonbCar.class);
    
    // Ensure the tanks were persisted correctly.
    Assertions.assertEquals(expectedValue.toLowerCase(), result.getJsonData().get(KEY).toLowerCase());
    Assertions.assertEquals(expectedValue.toLowerCase(), result.getJsonListData().get(0).getValue().toLowerCase());
  }

  private JsonbCar newCar(String expectedValue) {
    Map<String, String> data = Map.of(KEY, expectedValue);
    return JsonbCar.builder()
      .id(RandomUtils.nextInt(0, 10000))
      .jsonData(data)
      .jsonListData(List.of(JsonbCar.CarDetails.builder().value(expectedValue).build()))
      .uuid(UUID.randomUUID())
      .build();
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = JsonbPredicateITConfig.class)
  public static class JsonbPredicateITConfig {


  }
}
