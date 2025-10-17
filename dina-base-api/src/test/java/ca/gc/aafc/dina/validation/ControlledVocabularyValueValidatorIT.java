package ca.gc.aafc.dina.validation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.entity.MyControlledVocabulary;
import ca.gc.aafc.dina.entity.MyControlledVocabularyItem;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.entity.ma.TestManagedAttributeUsage;
import ca.gc.aafc.dina.service.ControlledVocabularyItemService;
import ca.gc.aafc.dina.service.ControlledVocabularyService;
import ca.gc.aafc.dina.service.ControlledVocabularyServiceIT;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ValidationException;

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
  private ManagedAttributeValueValidatorV2<MyControlledVocabularyItem> myControlledVocabularyValueValidator;

  @Inject
  private ControlledTermValueValidator<MyControlledVocabulary, MyControlledVocabularyItem> myControlledTermValueValidator;

  @Inject
  private QualifiedValueValidator<MyControlledVocabulary, MyControlledVocabularyItem> myQualifiedValueValidator;

  @Test
  void test_managedAttribute() {

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

    ((ControlledVocabularyServiceIT.MyControlledVocabularyServiceTestConfig.MyManagedAttributeValueValidator)myControlledVocabularyValueValidator).setControlledVocabularyUuid(ControlledVocabularyServiceIT.CONTROLLED_VOCAB_UUID);
    TestManagedAttributeUsage ma = TestManagedAttributeUsage
      .builder()
      .managedAttributes(Map.of("managed_attribute_1", "12"))
      .build();
    myControlledVocabularyValueValidator.validate(ma, ma.getManagedAttributes());

    // try invalid value
    ma.setManagedAttributes(Map.of("managed_attribute_1", "xy"));
    assertThrows(
      ValidationException.class, () -> myControlledVocabularyValueValidator.validate(ma, ma.getManagedAttributes()));
  }

  @Test
  void test_managedAttribute_withContext() {

    MyControlledVocabulary managedAttribute = controlledVocabularyService
      .create(MyControlledVocabulary.builder()
        .uuid(ControlledVocabularyServiceIT.CONTROLLED_VOCAB_UUID)
        .type(ControlledVocabulary.ControlledVocabularyType.MANAGED_ATTRIBUTE)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.QUALIFIED_VALUE)
        .name("Managed Attribute").build());

    MyControlledVocabularyItem item1 = controlledVocabularyItemService
      .create(MyControlledVocabularyItem.builder()
        .uuid(UUID.randomUUID())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.INTEGER)
        .dinaComponent("Context1")
        .group("grp")
        .name("vocab1").build());
    item1.setControlledVocabulary(managedAttribute);
    controlledVocabularyItemService.update(item1);

    MyControlledVocabularyItem item2 = controlledVocabularyItemService
      .create(MyControlledVocabularyItem.builder()
        .uuid(UUID.randomUUID())
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.STRING)
        .dinaComponent("Context2")
        .group("grp")
        .name("vocab1").build());
    item2.setControlledVocabulary(managedAttribute);
    controlledVocabularyItemService.update(item2);

    ((ControlledVocabularyServiceIT.MyControlledVocabularyServiceTestConfig.MyManagedAttributeValueValidator)myControlledVocabularyValueValidator).setControlledVocabularyUuid(ControlledVocabularyServiceIT.CONTROLLED_VOCAB_UUID);
    ((ControlledVocabularyServiceIT.MyControlledVocabularyServiceTestConfig.MyManagedAttributeValueValidator)myControlledVocabularyValueValidator).setDinaComponent("Context1");
    TestManagedAttributeUsage ma = TestManagedAttributeUsage
      .builder()
      .managedAttributes(Map.of("vocab_1", "12"))
      .build();
    myControlledVocabularyValueValidator.validate(ma, ma.getManagedAttributes());

    ((ControlledVocabularyServiceIT.MyControlledVocabularyServiceTestConfig.MyManagedAttributeValueValidator)myControlledVocabularyValueValidator).setDinaComponent("Context2");
    ma.setManagedAttributes(Map.of("vocab_1", "xy"));
    myControlledVocabularyValueValidator.validate(ma, ma.getManagedAttributes());
  }

  @Test
  void test_controlledTerm() {
    UUID controlledVocabularyUuid = UUID.randomUUID();

    MyControlledVocabulary managedAttribute = controlledVocabularyService
      .create(MyControlledVocabulary.builder()
        .uuid(controlledVocabularyUuid)
        .type(ControlledVocabulary.ControlledVocabularyType.SYSTEM)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.CONTROLLED_TERM)
        .name("Controlled Term").build());

    MyControlledVocabularyItem managedAttributeItem = controlledVocabularyItemService
      .create(MyControlledVocabularyItem.builder()
        .uuid(UUID.randomUUID())
        .group("grp")
        .name("controlled term 1").build());
    managedAttributeItem.setControlledVocabulary(managedAttribute);
    controlledVocabularyItemService.update(managedAttributeItem);

    Person p = Person.builder().build();
    myControlledTermValueValidator.validate(p, "controlled_term", "Controlled_Term_1");

    // try invalid key
    assertThrows(
      ValidationException.class, () -> myControlledTermValueValidator.validate(p, "controlled_term", "Controlled_Term_Invalid"));
  }

  @Test
  void test_qualifiedValueValidator() {
    UUID controlledVocabularyUuid = UUID.randomUUID();

    MyControlledVocabulary managedAttribute = controlledVocabularyService
      .create(MyControlledVocabulary.builder()
        .uuid(controlledVocabularyUuid)
        .type(ControlledVocabulary.ControlledVocabularyType.SYSTEM)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.QUALIFIED_VALUE)
        .name("Controlled Vocabulary With Values").build());

    MyControlledVocabularyItem managedAttributeItem = controlledVocabularyItemService
      .create(MyControlledVocabularyItem.builder()
        .uuid(UUID.randomUUID())
        .group("grp")
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.DATE)
        .name("the date").build());
    managedAttributeItem.setControlledVocabulary(managedAttribute);
    controlledVocabularyItemService.update(managedAttributeItem);

    Person p = Person.builder().build();
    myQualifiedValueValidator.validate(p, "controlled_vocabulary_with_values", "the_date", "2025-06-01");

    // try invalid value
    assertThrows(
      ValidationException.class, () -> myQualifiedValueValidator.validate(p, "controlled_vocabulary_with_values", "the_date", "abc"));
  }

}
