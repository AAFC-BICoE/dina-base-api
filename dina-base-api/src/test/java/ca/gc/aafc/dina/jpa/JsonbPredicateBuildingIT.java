package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.BasePostgresItContext;
import ca.gc.aafc.dina.entity.JsonbCar;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Transactional
public class JsonbPredicateBuildingIT extends BasePostgresItContext {

  private static final String KEY = "customKey";
  private static final JsonbValueSpecification<JsonbCar> JSONB_VAL_SPECS = new JsonbValueSpecification<>(
      "jsonData", KEY);

  @Inject
  private BaseDAO baseDAO;

  @Test
  void jsonb_predicate_building() {

    String expectedValue = "CustomValue";

    JsonbCar tank = newCar(expectedValue);
    JsonbCar another = newCar("another");

    persistCar(expectedValue, tank);
    persistCar( "another", another);

    CriteriaBuilder builder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<JsonbCar> criteria = builder.createQuery(JsonbCar.class);
    Root<JsonbCar> root = criteria.from(JsonbCar.class);

    Predicate predicate = JSONB_VAL_SPECS.toPredicate(root, builder, expectedValue);
    criteria.where(predicate).select(root);
    List<JsonbCar> resultList = baseDAO.resultListFromCriteria(criteria, 0, 20);

    Assertions.assertEquals(1, resultList.size());
    Assertions.assertEquals(tank.getUuid(), resultList.get(0).getUuid());
    Assertions.assertEquals(expectedValue, resultList.get(0).getJsonData().get(KEY));
  }

  private void persistCar(String expectedValue, JsonbCar car) {
    baseDAO.create(car);
    Map<String, Object> expectedData = baseDAO.findOneByNaturalId(
      car.getUuid(), JsonbCar.class).getJsonData();
    Assertions.assertEquals(expectedValue, expectedData.get(KEY));
  }

  private JsonbCar newCar(String expectedValue) {
    return JsonbCar.builder()
      .id(RandomUtils.nextInt(0, 10000))
      .jsonData(Map.of(KEY, expectedValue))
      .uuid(UUID.randomUUID())
      .build();
  }
}
