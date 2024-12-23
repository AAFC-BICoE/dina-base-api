package ca.gc.aafc.dina.service;

import org.hibernate.annotations.Type;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.SmartValidator;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.IdentifierType;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.validation.IdentifierTypeValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SpringBootTest(classes = {TestDinaBaseApp.class, IdentifierTypeServiceIT.IdentifierTypeServiceConfig.class})
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class IdentifierTypeServiceIT {

  @Inject
  private IdentifierTypeService<IdentifierType> identifierTypeService;

  @Test
  @Transactional
  public void identifierTypeService_OnCreate_KeyCorrectlyGenerated() {

    IdentifierType identifierType = identifierTypeService
      .create(TestIdentifierType.builder()
        .id(11)
        .uuid(UUID.randomUUID())
        .uriTemplate("https://abc.abc/$1")
        .dinaComponents(List.of("project", "person"))
        .name("dina specialIdentifier #11").build());

    assertEquals("dina_special_identifier_11", identifierType.getKey());
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = TestIdentifierType.class)
  static class IdentifierTypeServiceConfig {

    @Bean
    public IdentifierTypeService<IdentifierType> identifierTypeService(BaseDAO baseDAO,
                                                                       SmartValidator sv,
                                                                       IdentifierTypeValidator iv) {
      return new IdentifierTypeService<>(baseDAO, sv, iv) {
      };
    }

    @Bean
    public IdentifierTypeValidator identifierTypeValidator(@Named("validationMessageSource")
                                                               MessageSource messageSource) {
      return new IdentifierTypeValidator(messageSource);
    }
  }

  @Getter
  @Setter
  @SuperBuilder
  @Entity
  static class TestIdentifierType implements IdentifierType {

    @Id
    private Integer id;
    private UUID uuid;
    private String key;

    private String name;
    private String term;

    @Type(type = "jsonb")
    private MultilingualTitle multilingualTitle;

    @Type(type = "list-array")
    private List<String> dinaComponents;

    private String uriTemplate;
    private String createdBy;
    private OffsetDateTime createdOn;
  }

}
