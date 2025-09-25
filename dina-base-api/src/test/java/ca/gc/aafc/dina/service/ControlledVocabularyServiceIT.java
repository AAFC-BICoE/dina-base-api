package ca.gc.aafc.dina.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.validation.SmartValidator;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.entity.MyControlledVocabulary;
import ca.gc.aafc.dina.entity.MyControlledVocabularyItem;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.validation.ControlledVocabularyItemValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
@Import(ControlledVocabularyServiceIT.MyControlledVocabularyServiceTestConfig.class)
public class ControlledVocabularyServiceIT {

  @Inject
  private ControlledVocabularyService<MyControlledVocabulary> controlledVocabularyService;

  @Inject
  private ControlledVocabularyItemService<MyControlledVocabularyItem> controlledVocabularyItemService;

  @Test
  public void controlledVocabularyService_OnCreate_KeyCorrectlyGenerated() {
    MyControlledVocabulary managedAttribute = controlledVocabularyService
      .create(MyControlledVocabulary.builder()
        .uuid(UUID.randomUUID())
        .type(ControlledVocabulary.ControlledVocabularyType.SYSTEM)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.QUALIFIED_VALUE)
        .name("Protocol Data Element").build());

    assertEquals("protocol_data_element", managedAttribute.getKey());
  }

  @Test
  public void controlledVocabularyService_OnFindOne_OneReturned() {
    MyControlledVocabulary vocab1 = controlledVocabularyService
      .createAndFlush(MyControlledVocabulary.builder()
        .uuid(UUID.randomUUID())
        .type(ControlledVocabulary.ControlledVocabularyType.SYSTEM)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.CONTROLLED_TERM)
        .name("vocab 1").build());
    controlledVocabularyService
      .createAndFlush(MyControlledVocabulary.builder()
        .uuid(UUID.randomUUID())
        .type(ControlledVocabulary.ControlledVocabularyType.SYSTEM)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.CONTROLLED_TERM)
        .name("vocab 2").build());

    MyControlledVocabulary vocab = controlledVocabularyService.findOneByKey(vocab1.getKey());

    assertNotNull(vocab1.getId());
    assertEquals(vocab1.getId(), vocab.getId());
  }

  @Test
  public void controlledVocabularyItemService_OnCreate_entityCreated() {
    MyControlledVocabularyItem controlledVocabularyItem = controlledVocabularyItemService
      .create(MyControlledVocabularyItem.builder()
        .uuid(UUID.randomUUID())
        .name("Protocol Data Element 1").build());
    assertEquals("protocol_data_element_1", controlledVocabularyItem.getKey());

    MyControlledVocabulary controlledVocabulary = controlledVocabularyService
      .create(MyControlledVocabulary.builder()
        .uuid(UUID.randomUUID())
        .type(ControlledVocabulary.ControlledVocabularyType.SYSTEM)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.QUALIFIED_VALUE)
        .name("Protocol Data Element").build());

    controlledVocabularyItem.setControlledVocabulary(controlledVocabulary);
    controlledVocabularyItemService.update(controlledVocabularyItem);
    MyControlledVocabularyItem foundItem = controlledVocabularyItemService.findOneByKey(controlledVocabularyItem.getKey(), controlledVocabulary.getUuid());
    assertNotNull(foundItem);
    assertEquals(controlledVocabularyItem.getUuid(), foundItem.getUuid());
  }

  @TestConfiguration
  static class MyControlledVocabularyServiceTestConfig {

    @Service
    public static class MyControlledVocabularyService extends ControlledVocabularyService<MyControlledVocabulary> {
      public MyControlledVocabularyService(BaseDAO baseDAO, SmartValidator smartValidator) {
        super(baseDAO, smartValidator, MyControlledVocabulary.class);
      }
    }

    @Service
    public static class MyControlledVocabularyItemService extends ControlledVocabularyItemService<MyControlledVocabularyItem> {
      public MyControlledVocabularyItemService(BaseDAO baseDAO, SmartValidator smartValidator, ControlledVocabularyItemValidator validator) {
        super(baseDAO, smartValidator, MyControlledVocabularyItem.class, validator);
      }
    }

    @Bean
    public ControlledVocabularyItemValidator myControlledVocabularyItemValidator(
      @Named("validationMessageSource") MessageSource messageSource) {
      return new ControlledVocabularyItemValidator(messageSource);
    }

  }
}
