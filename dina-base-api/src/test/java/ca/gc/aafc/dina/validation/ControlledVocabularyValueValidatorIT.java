package ca.gc.aafc.dina.validation;

import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ValidationException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.entity.MyControlledVocabulary;
import ca.gc.aafc.dina.entity.MyControlledVocabularyItem;
import ca.gc.aafc.dina.entity.ma.TestManagedAttributeUsage;
import ca.gc.aafc.dina.service.ControlledVocabularyItemService;
import ca.gc.aafc.dina.service.ControlledVocabularyService;
import ca.gc.aafc.dina.service.ControlledVocabularyServiceIT;
import ca.gc.aafc.dina.service.ManagedAttributeServiceIT;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
@Import(ControlledVocabularyServiceIT.MyControlledVocabularyServiceTestConfig.class)
@Transactional
public class ControlledVocabularyValueValidatorIT {

  @Inject
  private ControlledVocabularyService<MyControlledVocabulary> controlledVocabularyService;

  @Inject
  private ControlledVocabularyItemService<MyControlledVocabularyItem> controlledVocabularyItemService;

  @Inject
  private ControlledVocabularyValueValidator<MyControlledVocabularyItem> myControlledVocabularyValueValidator;

  @Test
  void test() {
    MyControlledVocabulary managedAttribute = controlledVocabularyService
      .create(MyControlledVocabulary.builder()
        .uuid(ControlledVocabularyServiceIT.CONTROLLED_VOCAB_UUID)
        .type(ControlledVocabulary.ControlledVocabularyType.MANAGED_ATTRIBUTE)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.QUALIFIED_VALUE)
        .name("Managed Attribute").build());

    MyControlledVocabularyItem managedAttributeItem = controlledVocabularyItemService
      .create(MyControlledVocabularyItem.builder()
        .uuid(UUID.randomUUID())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.INTEGER)
        .group("grp")
        .name("managed attribute 1").build());
    managedAttributeItem.setControlledVocabulary(managedAttribute);
    controlledVocabularyItemService.update(managedAttributeItem);


    TestManagedAttributeUsage ma = TestManagedAttributeUsage
      .builder()
      .managedAttributes(Map.of("managed_attribute_1", "12"))
      .build();
    myControlledVocabularyValueValidator.validate(ma, ma.getManagedAttributes());

    ma.setManagedAttributes(Map.of("managed_attribute_1", "xy"));
    assertThrows(
      ValidationException.class, () -> myControlledVocabularyValueValidator.validate(ma, ma.getManagedAttributes()));
  }

}
