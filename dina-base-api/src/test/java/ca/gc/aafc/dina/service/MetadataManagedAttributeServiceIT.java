package ca.gc.aafc.dina.service;

import java.time.OffsetDateTime;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.MetadataManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import groovy.transform.builder.Builder;
import lombok.Data;
import lombok.NonNull;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, MetadataManagedAttributeServiceIT.MetadataManagedAttributeConfig.class})
public class MetadataManagedAttributeServiceIT {

  @TestConfiguration
  @EntityScan(basePackageClasses = MetadataManagedAttributeConfig.TestMetadataManagedAttribute.class)
  static class MetadataManagedAttributeConfig {

    @Data
    @Builder
    @Entity
    static class TestMetadataManagedAttribute implements MetadataManagedAttribute {
      @Id
      @GeneratedValue
      private Integer id;
      private UUID uuid;
      private String createdBy;
      private String assignedValue;
      private OffsetDateTime createdOn;
      private ManagedAttribute managedAttribute;
      private Map.Entry<String, String> metadata;

    }

    @Data
    @Builder
    @Entity
    static class TestManagedAttribute implements ManagedAttribute {
      @Id
      @GeneratedValue
      private Integer id;
      private UUID uuid;
      private String name;
      private String key;
      private ManagedAttributeType managedAttributeType;
      private String[] acceptedValues;
      private String createdBy;
      private OffsetDateTime createdOn;
    }

    @Service
    static class TestMetadataManagedAttributeService extends MetadataManagedAttributeService<TestMetadataManagedAttribute> {
      public TestMetadataManagedAttributeService(@NonNull BaseDAO baseDAO) {
        super(baseDAO);
      }
    }
  }
  
}
