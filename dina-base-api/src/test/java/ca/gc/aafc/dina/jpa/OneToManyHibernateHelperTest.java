package ca.gc.aafc.dina.jpa;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.service.DefaultDinaService;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.NaturalId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@SpringBootTest(
  classes = {TestDinaBaseApp.class, OneToManyHibernateHelperTest.OneToManyHibernateHelperTestConfig.class})
@Transactional
class OneToManyHibernateHelperTest {

  @Inject
  private DinaRepository<OneToManyHibernateHelperTestConfig.DtoA, OneToManyHibernateHelperTestConfig.A> repoA;
  @Inject
  private DinaRepository<OneToManyHibernateHelperTestConfig.DtoB, OneToManyHibernateHelperTestConfig.B> repoB;

  @Test
  void name() {
    OneToManyHibernateHelperTestConfig.DtoB child = new OneToManyHibernateHelperTestConfig.DtoB();
    child.setUuid(UUID.randomUUID());
    child.setCreatedBy("dina");
    child.setGroup("1");
    repoB.create(child);

    OneToManyHibernateHelperTestConfig.DtoB child2 = new OneToManyHibernateHelperTestConfig.DtoB();
    child2.setUuid(UUID.randomUUID());
    child2.setCreatedBy("dina");
    child2.setGroup("2");
    repoB.create(child2);

    OneToManyHibernateHelperTestConfig.DtoA parent = new OneToManyHibernateHelperTestConfig.DtoA();
    parent.setUuid(UUID.randomUUID());
    parent.setCreatedBy("dina");
    parent.setGroup("parent");
    parent.setChildren(List.of(repoB.findOne(child.getUuid(), getChildQuerySpec())));
    repoA.create(parent);

    OneToManyHibernateHelperTestConfig.DtoA resultParent = repoA.findOne(
      parent.getUuid(),
      getParentQuerySpec());
    Assertions.assertEquals(1, resultParent.getChildren().size());
    Assertions.assertEquals(child.getUuid(), resultParent.getChildren().get(0).getUuid());

    resultParent.setChildren(new ArrayList<>(List.of(repoB.findOne(
      child2.getUuid(),
      getChildQuerySpec()))));
    repoA.save(resultParent);

    OneToManyHibernateHelperTestConfig.DtoA updatedParentResult = repoA.findOne(
      parent.getUuid(),
      getParentQuerySpec());
    Assertions.assertEquals(1, updatedParentResult.getChildren().size());
    Assertions.assertEquals(child2.getUuid(), updatedParentResult.getChildren().get(0).getUuid());
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
    }

    @Service
    public static class ServiceB extends DefaultDinaService<B> {

      public ServiceB(
        @NonNull BaseDAO baseDAO,
        @NonNull SmartValidator validator
      ) {
        super(baseDAO, validator);
      }
    }

  }

}