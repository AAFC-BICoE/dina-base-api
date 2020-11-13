package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.entity.ComplexObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class CustomFieldHandlerTest {

  private static final CustomFieldHandler<DinaMapperTest.StudentDto, DinaMapperTest.Student> CFH =
    new CustomFieldHandler<>(DinaMapperTest.StudentDto.class, DinaMapperTest.Student.class);

  @Test
  void resolveFields_ToEntity_allCustomFieldsResolved() {
    String expected_customField = "expected customField";
    String expected_oneSided = "expected oneSided";

    DinaMapperTest.StudentDto source = DinaMapperTest.StudentDto.builder()
      .customField(expected_customField)
      .name(expected_oneSided)
      .build();
    DinaMapperTest.Student target = new DinaMapperTest.Student();

    CFH.resolveFields(Set.of("customField", "oneSided"), source, target);
    Assertions.assertEquals(expected_customField, target.getCustomField().getName());
    Assertions.assertEquals(expected_oneSided, target.getOneSided());
  }

  @Test
  void resolveFields_ToDTO_allCustomFieldsResolved() {
    String expected_customField = "expected customField";
    int expected_oneSided = 3;

    DinaMapperTest.Student source = DinaMapperTest.Student.builder()
      .customField(ComplexObject.builder().name(expected_customField).build())
      .iq(expected_oneSided)
      .build();
    DinaMapperTest.StudentDto target = new DinaMapperTest.StudentDto();

    CFH.resolveFields(Set.of("customField", "oneSidedDto"), source, target);
    Assertions.assertEquals(expected_customField, target.getCustomField());
    Assertions.assertEquals(expected_oneSided, target.getOneSidedDto());
  }

  @Test
  void resolveFields_SelectedFieldsOnly() {
    String expected_customField = "expected customField";
    String expected_oneSided = "expected oneSided";

    DinaMapperTest.StudentDto source = DinaMapperTest.StudentDto.builder()
      .customField(expected_customField)
      .name(expected_oneSided)
      .build();
    DinaMapperTest.Student target = new DinaMapperTest.Student();

    CFH.resolveFields(Set.of("customField"), source, target);
    Assertions.assertEquals(expected_customField, target.getCustomField().getName());
    Assertions.assertNull(target.getOneSided());
  }

  @Test
  void hasCustomFieldResolver_FieldsProperlyIdentified() {
    Assertions.assertTrue(CFH.hasCustomFieldResolver("customField"));
    Assertions.assertTrue(CFH.hasCustomFieldResolver("oneSidedDto"));
    Assertions.assertFalse(CFH.hasCustomFieldResolver("friend"));
    Assertions.assertFalse(CFH.hasCustomFieldResolver("nickNames"));
    Assertions.assertFalse(CFH.hasCustomFieldResolver("iq"));
    Assertions.assertFalse(CFH.hasCustomFieldResolver("NoSuchField"));
  }
}
