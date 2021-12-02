package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest(classes = {TestDinaBaseApp.class, JsonbKeyValuePredicateIT.JsonbPredicateITConfig.class},
  properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(initializers = {PostgresTestContainerInitializer.class})
@ExtendWith(SpringExtension.class)
@Transactional
public class JsonbKeyValuePredicateIT {

  @Inject
  private BaseDAO baseDAO;

  private static final String KEY = "customKey";

  @Test
  void jsonb_predicate_building() throws JsonProcessingException {
    String expectedValue = "CustomValue";

    JsonbPredicateITConfig.Tank tank = newTank(expectedValue);
    JsonbPredicateITConfig.Tank another = newTank("another");

    persistTank(expectedValue, tank);
    persistTank("another", another);
  
    CriteriaBuilder builder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<JsonbPredicateITConfig.Tank> criteria = builder.createQuery(JsonbPredicateITConfig.Tank.class);
    Root<JsonbPredicateITConfig.Tank> root = criteria.from(JsonbPredicateITConfig.Tank.class);

    Predicate predicate = new JsonbKeyValuePredicate<JsonbPredicateITConfig.Tank>()
        .onKey("jsonData", KEY)
        .buildUsing(root, builder, expectedValue);

    criteria.where(predicate).select(root);
    List<JsonbPredicateITConfig.Tank> resultList = baseDAO.resultListFromCriteria(criteria, 0, 20);
  
    Assertions.assertEquals(1, resultList.size());
    Assertions.assertEquals(tank.getUuid(), resultList.get(0).getUuid());
    Assertions.assertEquals(expectedValue, resultList.get(0).getJsonData().get(KEY));
  }

  @Test
  void jsonb_arrayPredicate_building() throws JsonProcessingException {
    String expectedValue = "CustomValue";

    JsonbPredicateITConfig.Tank tank = newTank(expectedValue);
    JsonbPredicateITConfig.Tank another = newTank("another");

    persistTank(expectedValue, tank);
    persistTank("another", another);

    CriteriaBuilder builder = baseDAO.getCriteriaBuilder();
    CriteriaQuery<JsonbPredicateITConfig.Tank> criteria = builder.createQuery(JsonbPredicateITConfig.Tank.class);
    Root<JsonbPredicateITConfig.Tank> root = criteria.from(JsonbPredicateITConfig.Tank.class);

    Predicate predicate = new JsonbKeyValuePredicate<JsonbPredicateITConfig.Tank>()
        .onKey("jsonListData", "value")
        .buildUsing(root, builder, expectedValue);

    criteria.where(predicate).select(root);
    List<JsonbPredicateITConfig.Tank> resultList = baseDAO.resultListFromCriteria(criteria, 0, 20);

    Assertions.assertEquals(1, resultList.size());
    Assertions.assertEquals(tank.getUuid(), resultList.get(0).getUuid());
    Assertions.assertEquals(expectedValue, resultList.get(0).getJsonData().get(KEY));
  }

  private void persistTank(String expectedValue, JsonbPredicateITConfig.Tank tank) {
    baseDAO.create(tank);
    JsonbPredicateITConfig.Tank result = baseDAO.findOneByNaturalId(
      tank.getUuid(),
      JsonbPredicateITConfig.Tank.class);
    Assertions.assertEquals(expectedValue, result.getJsonData().get(KEY));
    Assertions.assertEquals(expectedValue, result.getJsonListData().get(0).getValue());
  }

  private JsonbPredicateITConfig.Tank newTank(String expectedValue) {
    Map<String, String> data = Map.of(KEY, expectedValue);
    return JsonbPredicateITConfig.Tank.builder()
      .id(RandomUtils.nextInt(0, 10000))
      .jsonData(data)
      .jsonListData(new ArrayList<>(List.of(
        JsonbPredicateITConfig.Tank.JsonTank.builder().value(expectedValue).build())))
      .uuid(UUID.randomUUID())
      .build();
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = JsonbPredicateITConfig.class)
  public static class JsonbPredicateITConfig {

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
    @Table(name = "tank")
    public static class Tank implements DinaEntity {
      private String createdBy;
      private OffsetDateTime createdOn;
      @Id
      private Integer id;
      @NaturalId
      private UUID uuid;

      @Type(type = "jsonb")
      @Column(columnDefinition = "jsonb")
      private Map<String, String> jsonData;

      @Type(type = "jsonb")
      @Column(columnDefinition = "jsonb")
      private List<JsonTank> jsonListData;

      @Data
      @Builder
      @NoArgsConstructor
      @AllArgsConstructor
      public static class JsonTank {
        private String value;
      }
    }

  }
}
