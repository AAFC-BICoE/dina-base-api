package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.restassured.http.Header;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hamcrest.Matchers;
import org.hibernate.annotations.NaturalId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@SpringBootTest(
  classes = {TestDinaBaseApp.class, OneToManyHibernateHelperTest.OneToManyHibernateHelperTestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class OneToManyHibernateHelperTest extends BaseRestAssuredTest {

  private static final Header CRNK_HEADER = new Header("crnk-compact", "true");

  protected OneToManyHibernateHelperTest() {
    super("");
  }

  @Test
  void name() {
    OneToManyHibernateHelperTestConfig.DtoB child = new OneToManyHibernateHelperTestConfig.DtoB();
    child.setCreatedBy("dina");
    child.setGroup("1");
    String childId1 = sendPost(
      "B",
      JsonAPITestHelper.toJsonAPIMap("B", JsonAPITestHelper.toAttributeMap(child))).extract()
      .body()
      .jsonPath()
      .getString("data.id");

    OneToManyHibernateHelperTestConfig.DtoB child2 = new OneToManyHibernateHelperTestConfig.DtoB();
    child2.setCreatedBy("dina");
    child2.setGroup("2");
    String childId2 = sendPost(
      "B",
      JsonAPITestHelper.toJsonAPIMap("B", JsonAPITestHelper.toAttributeMap(child2))).extract()
      .body().jsonPath().getString("data.id");

    OneToManyHibernateHelperTestConfig.DtoA parent = new OneToManyHibernateHelperTestConfig.DtoA();
    parent.setCreatedBy("dina");
    parent.setGroup("parent");
    String parentId = sendPost(
      "A",
      JsonAPITestHelper.toJsonAPIMap("A", JsonAPITestHelper.toAttributeMap(parent),
        Map.of(
          "children",
          Map.of(
            "data", List.of(Map.of(
              "type", "B",
              "id", childId1
            ))
          )
        ),
        null
      )).extract().body().jsonPath().getString("data.id");

    given()
      .header(CRNK_HEADER).port(testPort).basePath(basePath)
      .queryParams(Map.of("include", "children"))
      .get("A/" + parentId)
      .then().log().all(true)
      .body("data.relationships.children.data", Matchers.arrayWithSize(1))
      .body("data.relationships.children.data[0].id", Matchers.is(childId1));

    sendPatch(parentId, Map.of("Data", Map.of("relationships", Map.of(
      "children",
      Map.of(
        "data", List.of(Map.of(
          "type", "B",
          "id", childId2
        ))
      )
    )))).log().all(true);

    given()
      .header(CRNK_HEADER).port(testPort).basePath(basePath)
      .queryParams(Map.of("include", "children"))
      .get("A/" + parentId)
      .then()
      .body("data.relationships.children.data", Matchers.arrayWithSize(1))
      .body("data.relationships.children.data[0].id", Matchers.is(childId2));
  }

  private static QuerySpec getChildQuerySpec() {
    return new QuerySpec(OneToManyHibernateHelperTestConfig.DtoB.class);
  }

  private static QuerySpec getParentQuerySpec() {
    return new QuerySpec(OneToManyHibernateHelperTestConfig.DtoB.class);
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = OneToManyHibernateHelperTestConfig.class)
  public static class OneToManyHibernateHelperTestConfig {

    @Bean
    public DinaRepository<DtoA, A> testRepoA(
      ServiceA serviceA
    ) {
      return new DinaRepository<>(
        serviceA,
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(DtoA.class),
        DtoA.class,
        A.class,
        null,
        null,
        new BuildProperties(new Properties())
      );
    }

    @Bean
    public DinaRepository<DtoB, B> testRepoB(
      ServiceB serviceB
    ) {
      return new DinaRepository<>(
        serviceB,
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(DtoB.class),
        DtoB.class,
        B.class,
        null,
        null,
        new BuildProperties(new Properties())
      );
    }

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class A implements DinaEntity {
      private String createdBy;
      private OffsetDateTime createdOn;
      @Column(name = "group_name")
      private String group;
      @Id
      @GeneratedValue
      private Integer id;
      @NaturalId
      private UUID uuid;

      @OneToMany(mappedBy = "parent")
      private List<B> children;
    }

    @Data
    @JsonApiResource(type = "A")
    @RelatedEntity(A.class)
    public static class DtoA {
      @JsonApiId
      private UUID uuid;
      private String createdBy;
      private OffsetDateTime createdOn;
      private String group;

      @JsonApiRelation
      private List<DtoB> children;
    }

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class B implements DinaEntity {
      private String createdBy;
      private OffsetDateTime createdOn;
      @Column(name = "group_name")
      private String group;
      @Id
      @GeneratedValue
      private Integer id;
      @NaturalId
      private UUID uuid;

      @ManyToOne
      private A parent;
    }

    @Data
    @JsonApiResource(type = "B")
    @RelatedEntity(B.class)
    public static class DtoB {
      @JsonApiId
      private UUID uuid;
      private String createdBy;
      private OffsetDateTime createdOn;
      private String group;

      @JsonApiRelation
      private DtoA parent;
    }

    @Service
    public static class ServiceA extends DefaultDinaService<A> {

      public ServiceA(
        @NonNull BaseDAO baseDAO,
        @NonNull SmartValidator validator
      ) {
        super(baseDAO, validator);
      }

      @Override
      protected void preCreate(A entity) {
        entity.setUuid(UUID.randomUUID());
      }
    }

    @Service
    public static class ServiceB extends DefaultDinaService<B> {

      public ServiceB(
        @NonNull BaseDAO baseDAO,
        @NonNull SmartValidator validator
      ) {
        super(baseDAO, validator);
      }

      @Override
      protected void preCreate(B entity) {
        entity.setUuid(UUID.randomUUID());
      }
    }

  }

}