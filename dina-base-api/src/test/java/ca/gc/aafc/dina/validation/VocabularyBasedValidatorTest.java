package ca.gc.aafc.dina.validation;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;

import ca.gc.aafc.dina.BasePostgresItContext;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.vocabulary.VocabularyElementConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VocabularyBasedValidatorTest extends BasePostgresItContext {

  @Inject
  @Named("validationMessageSource")
  private MessageSource baseValidationMessageSource;

  @Test
  public void test_vocabularyBasedValidator_Functions() {
    TestVocabularyValidator tvv = new TestVocabularyValidator(Person.class, baseValidationMessageSource);

    VocabularyElementConfiguration vec = new VocabularyElementConfiguration();
    vec.setKey("key1");
    List<VocabularyElementConfiguration> vecList = List.of(vec);

    // test validateValuesAgainstVocabulary
    Person p = new Person();
    Errors err = ValidationErrorsHelper.newErrorsObject(p);
    tvv.validateValuesAgainstVocabulary(List.of("invalidKey"), "name", vecList, err);
    assertTrue(err.hasErrors());
    err = ValidationErrorsHelper.newErrorsObject(p);
    tvv.validateValuesAgainstVocabulary(List.of("key1"), "name", vecList, err);
    assertFalse(err.hasErrors());

    err = ValidationErrorsHelper.newErrorsObject(p);
    assertEquals("key1", tvv.validateAndStandardizeValueAgainstVocabulary("KeY1", "name", vecList, err));
    assertFalse(err.hasErrors());

    err = ValidationErrorsHelper.newErrorsObject(p);
    tvv.validateAndStandardizeValueAgainstVocabulary("invalidKey", "name", vecList, err);
    assertTrue(err.hasErrors());
  }

  public static final class TestVocabularyValidator extends VocabularyBasedValidator<Person> {

    public TestVocabularyValidator(Class<Person> supportedClass,
                                   MessageSource messageSource) {
      super(supportedClass, messageSource);
    }

    @Override
    public void validateTarget(Person target, Errors errors) {
      // no-op
    }
  }

}
