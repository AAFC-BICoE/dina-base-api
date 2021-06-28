package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.CustomFieldAdapter;
import ca.gc.aafc.dina.mapper.DinaFieldAdapter;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import io.restassured.response.ValidatableResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.hibernate.annotations.NaturalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;

@SpringBootTest(
  classes = {TestDinaBaseApp.class, OneToManyDinaServiceTest.OneToManyHibernateHelperTestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OneToManyDinaServiceTest extends BaseRestAssuredTest {

  public static final String PARENT_TYPE_NAME = "A";
  public static final String CHILD_TYPE_NAME = "B";
  private String firstResourceBId;
  private String secondResourceBid;

  protected OneToManyDinaServiceTest() {
    super("");
  }

  @BeforeEach
  void setUp() {
    firstResourceBId = postNewChild();
    secondResourceBid = postNewChild();
  }

  @Test
  void childResolution_OnPost() {
    String expectedInternalChild = RandomStringUtils.randomAlphabetic(3);
    String parentId = postParentWithChild(firstResourceBId, List.of(expectedInternalChild));
    findParentById(parentId)
      .body("data.relationships.children.data", Matchers.hasSize(1))
      .body("data.relationships.children.data[0].id", Matchers.is(firstResourceBId))
      .body("data.attributes.internalChildren[0]", Matchers.is(expectedInternalChild));
    findChildById(firstResourceBId).body("data.relationships.parent.data.id", Matchers.is(parentId));
  }

  @Test
  void childResolution_OnPatch_AddAndRemove() {
    String parentId = postParentWithChild(
      firstResourceBId, List.of(RandomStringUtils.randomAlphabetic(3)));
    String expectedInternalChild = RandomStringUtils.randomAlphabetic(3);

    sendPatch(PARENT_TYPE_NAME, parentId, Map.of(
      "data", Map.of(
        "relationships", Map.of("children", Map.of(
          "data", List.of(Map.of("type", CHILD_TYPE_NAME, "id", secondResourceBid)))),
        "attributes", Map.of("internalChildren", List.of(expectedInternalChild)),
        "type", PARENT_TYPE_NAME)));

    findParentById(parentId)
      .body("data.relationships.children.data", Matchers.hasSize(1))
      .body("data.relationships.children.data[0].id", Matchers.is(secondResourceBid))
      .body("data.attributes.internalChildren[0]", Matchers.is(expectedInternalChild));

    findChildById(secondResourceBid).body("data.relationships.parent.data.id", Matchers.is(parentId));
    findChildById(firstResourceBId).body("data.relationships.parent.data", Matchers.nullValue());
  }

  @Test
  void childResolution_OnPatch_RemoveAll() {
    String parentId = postParentWithChild(firstResourceBId, List.of(
      RandomStringUtils.randomAlphabetic(3), RandomStringUtils.randomAlphabetic(3)));

    sendPatch(PARENT_TYPE_NAME, parentId, Map.of(
      "data",
      Map.of("relationships",
        Map.of("children", Map.of("data", List.of())),
        "attributes", Map.of("internalChildren", List.of()),
        "type", PARENT_TYPE_NAME)));

    findParentById(parentId)
      .body("data.relationships.children.data", Matchers.empty())
      .body("data.attributes.internalChildren", Matchers.empty());
    findChildById(firstResourceBId).body("data.relationships.parent.data", Matchers.nullValue());
  }

  @Test
  void childResolution_OnDelete() {
    String parentId = postParentWithChild(firstResourceBId, List.of(
      RandomStringUtils.randomAlphabetic(3), RandomStringUtils.randomAlphabetic(3)));
    findParentById(parentId)
      .body("data.relationships.children.data", Matchers.hasSize(1))
      .body("data.relationships.children.data[0].id", Matchers.is(firstResourceBId));
    sendDelete(PARENT_TYPE_NAME, parentId);
    findChildById(firstResourceBId).body("data.relationships.parent.data", Matchers.nullValue());
  }

  @Test
  void parentResolution_OnPost() {
    String parentId = postParentWithChild(firstResourceBId, null);
    String childId = postNewChildWithParent(parentId);
    findChildById(childId).body("data.relationships.parent.data.id", Matchers.is(parentId));
    findParentById(parentId)
      .body("data.relationships.children.data", Matchers.hasSize(2))
      .body("data.relationships.children.data.id", Matchers.containsInAnyOrder(firstResourceBId, childId));
  }

  @Test
  void parentResolution_OnPatch_AddAndRemove() {
    String parentId = postParentWithChild(firstResourceBId, null);
    String childId = postNewChild();

    sendPatch(CHILD_TYPE_NAME, childId, Map.of(
      "data", Map.of(
        "relationships",
        Map.of("parent", Map.of("data", Map.of("type", PARENT_TYPE_NAME, "id", parentId))),
        "type", CHILD_TYPE_NAME)));
    findChildById(childId).body("data.relationships.parent.data.id", Matchers.is(parentId));

    HashMap<String, Object> empty = new HashMap<>();
    empty.put("data", null);
    sendPatch(CHILD_TYPE_NAME, childId, Map.of(
      "data",
      Map.of("relationships", Map.of("parent", empty), "type", CHILD_TYPE_NAME)));
    findChildById(childId).body("data.relationships.parent.data", Matchers.nullValue());
    findParentById(parentId)
      .body("data.relationships.children.data", Matchers.hasSize(1))
      .body("data.relationships.children.data[0].id", Matchers.is(firstResourceBId));
  }

  @Test
  void parentResolution_OnDelete() {
    String parentId = postParentWithChild(firstResourceBId, null);
    String childId = postNewChildWithParent(parentId);
    findChildById(childId).body("data.relationships.parent.data.id", Matchers.is(parentId));

    sendDelete(CHILD_TYPE_NAME, childId);
    findParentById(parentId)
      .body("data.relationships.children.data", Matchers.hasSize(1))
      .body("data.relationships.children.data[0].id", Matchers.is(firstResourceBId));
  }

  private ValidatableResponse findChildById(String id) {
    return given()
      .header(CRNK_HEADER).port(testPort).basePath(basePath)
      .queryParams(Map.of("include", "parent"))
      .get("B/" + id).then();
  }

  private ValidatableResponse findParentById(String id) {
    return given()
      .header(CRNK_HEADER).port(testPort).basePath(basePath)
      .queryParams(Map.of("include", "children"))
      .get("A/" + id).then();
  }

  private String postParentWithChild(String childId, List<String> internalChildren) {
    return sendPost(
      PARENT_TYPE_NAME,
      JsonAPITestHelper.toJsonAPIMap(
        PARENT_TYPE_NAME,
        JsonAPITestHelper.toAttributeMap(newParent(internalChildren)),
        Map.of("children", Map.of("data", List.of(Map.of("type", CHILD_TYPE_NAME, "id", childId)))),
        null))
      .extract().body().jsonPath().getString("data.id");
  }

  private String postNewChild() {
    return postNewChildWithParent(null);
  }

  private String postNewChildWithParent(String parentId) {
    Map<String, Object> body;
    if (StringUtils.isBlank(parentId)) {
      body = JsonAPITestHelper.toJsonAPIMap(CHILD_TYPE_NAME, JsonAPITestHelper.toAttributeMap(newDtoB()));
    } else {
      body = JsonAPITestHelper.toJsonAPIMap(CHILD_TYPE_NAME, JsonAPITestHelper.toAttributeMap(newDtoB()),
        Map.of("parent", Map.of("data", Map.of("type", PARENT_TYPE_NAME, "id", parentId))),
        null);
    }
    return sendPost(CHILD_TYPE_NAME, body).extract().body().jsonPath().getString("data.id");
  }

  private static OneToManyHibernateHelperTestConfig.ParentDto newParent(List<String> internalChildren) {
    OneToManyHibernateHelperTestConfig.ParentDto parent = new OneToManyHibernateHelperTestConfig.ParentDto();
    parent.setCreatedBy("dina");
    parent.setGroup("parent");
    parent.setInternalChildren(internalChildren);
    return parent;
  }

  private static OneToManyHibernateHelperTestConfig.ChildDto newDtoB() {
    OneToManyHibernateHelperTestConfig.ChildDto child = new OneToManyHibernateHelperTestConfig.ChildDto();
    child.setCreatedBy("dina");
    child.setGroup(RandomStringUtils.randomAlphabetic(3));
    return child;
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = OneToManyHibernateHelperTestConfig.class)
  public static class OneToManyHibernateHelperTestConfig {

    @Bean
    public DinaRepository<ParentDto, Parent> testRepoA(
      ParentService parentService
    ) {
      return new DinaRepository<>(
        parentService,
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ParentDto.class),
        ParentDto.class,
        Parent.class,
        null,
        null,
        new BuildProperties(new Properties())
      );
    }

    @Bean
    public DinaRepository<ChildDto, Child> testRepoB(
      ChildService childService
    ) {
      return new DinaRepository<>(
        childService,
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ChildDto.class),
        ChildDto.class,
        Child.class,
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
    public static class Parent implements DinaEntity {
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
      private List<Child> children;

      @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
      private List<InternalChild> internalChildren;

    }

    @Data
    @JsonApiResource(type = "A")
    @RelatedEntity(Parent.class)
    @CustomFieldAdapter(adapters = ParentDto.InternalChildAdapter.class)
    public static class ParentDto {
      @JsonApiId
      private UUID uuid;
      private String createdBy;
      private OffsetDateTime createdOn;
      private String group;

      @JsonApiRelation
      private List<ChildDto> children;

      @IgnoreDinaMapping(reason = "custom mapped")
      private List<String> internalChildren;

      public static class InternalChildAdapter
        implements DinaFieldAdapter<
        ParentDto,
        Parent,
        List<String>,
        List<InternalChild>> {
        @Override
        public List<String> toDTO(List<InternalChild> internalChildren) {
          return internalChildren == null ? null : internalChildren.stream()
            .map(InternalChild::getName).collect(Collectors.toList());
        }

        @Override
        public List<InternalChild> toEntity(List<String> strings) {
          return strings == null ? null : strings.stream()
            .map(s -> InternalChild.builder().name(s).build()).collect(Collectors.toList());
        }

        @Override
        public Consumer<List<InternalChild>> entityApplyMethod(Parent entityRef) {
          return entityRef::setInternalChildren;
        }

        @Override
        public Consumer<List<String>> dtoApplyMethod(ParentDto dtoRef) {
          return dtoRef::setInternalChildren;
        }

        @Override
        public Supplier<List<InternalChild>> entitySupplyMethod(Parent entityRef) {
          return entityRef::getInternalChildren;
        }

        @Override
        public Supplier<List<String>> dtoSupplyMethod(ParentDto dtoRef) {
          return dtoRef::getInternalChildren;
        }
      }
    }

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Child implements DinaEntity {
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
      private Parent parent;
    }

    @Data
    @JsonApiResource(type = "B")
    @RelatedEntity(Child.class)
    public static class ChildDto {
      @JsonApiId
      private UUID uuid;
      private String createdBy;
      private OffsetDateTime createdOn;
      private String group;

      @JsonApiRelation
      private ParentDto parent;
    }

    @Data
    @Entity
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternalChild {
      @Id
      @GeneratedValue
      private Integer id;
      @ManyToOne
      private Parent parent;
      @Column(name = "name", unique = true)
      private String name;
    }

    @Service
    public static class ParentService extends OneToManyDinaService<Parent> {

      public ParentService(
        @NonNull BaseDAO baseDAO,
        @NonNull SmartValidator validator
      ) {
        super(baseDAO, validator,
          List.of(
            new OneToManyFieldHandler<>(Child.class, child -> child::setParent, Parent::getChildren,
              "parent", child -> child.setParent(null),
              (child, child2) -> child.getUuid().equals(child2.getUuid())),
            new OneToManyFieldHandler<>(InternalChild.class, child -> child::setParent,
              Parent::getInternalChildren, "parent",
              baseDAO::delete, (child, child2) -> child.getName().equalsIgnoreCase(child2.getName()))));
      }

      @Override
      protected void preCreate(Parent entity) {
        entity.setUuid(UUID.randomUUID());
      }

      @Override
      public void preHandleUpdate(Parent entity) {
        if (CollectionUtils.isNotEmpty(entity.getInternalChildren())) {
          List<InternalChild> resolved = new ArrayList<>();
          entity.getInternalChildren().forEach(incoming -> {
            Optional<InternalChild> byName = findByName(incoming.getName());
            byName.ifPresentOrElse(resolved::add, () -> resolved.add(incoming));
          });
          entity.setInternalChildren(resolved);
        }
      }

      private Optional<InternalChild> findByName(String name) {
        return this.findAll(
          InternalChild.class,
          (criteriaBuilder, root, em) -> new Predicate[]{
            criteriaBuilder.equal(root.get("name"), name)
          },
          null,
          0,
          Integer.MAX_VALUE).stream().findFirst();
      }

    }

    @Service
    public static class ChildService extends DefaultDinaService<Child> {

      public ChildService(
        @NonNull BaseDAO baseDAO,
        @NonNull SmartValidator validator
      ) {
        super(baseDAO, validator);
      }

      @Override
      protected void preCreate(Child entity) {
        entity.setUuid(UUID.randomUUID());
      }

      @Override
      protected void preUpdate(Child entity) {
        super.preUpdate(entity);
      }
    }

  }

}