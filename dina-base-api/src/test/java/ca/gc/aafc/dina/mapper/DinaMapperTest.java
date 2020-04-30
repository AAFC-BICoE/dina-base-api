package ca.gc.aafc.dina.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.StudentDto;
import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Student;

public class DinaMapperTest {

  private static Student entity;

  private static List<CustomFieldResolverSpec<Student>> dtoResolvers = new ArrayList<>();

  private static DinaMapper<StudentDto, Student> mapper;

  @BeforeAll
  public static void init() {
    initEntity();
    initDtoResolvers();
    mapper = new DinaMapper<>(StudentDto.class, Student.class, dtoResolvers);
  }

  @Test
  public void toDto_BaseAttributesTest_SelectedFieldsMapped() {
    HashSet<String> selectedFields = new HashSet<>();
    selectedFields.add("name");

    Map<Class<?>, Set<String>> selectedFieldPerClass = new HashMap<>();
    selectedFieldPerClass.put(Student.class, selectedFields);

    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, new HashSet<>());

    assertEquals(entity.getName(), dto.getName());
    // Assert value not mapped - not included in selected fields
    assertEquals(0, dto.getIq());
  }

  @Test
  public void toDto_RelationShipTest_RelationsMapped() {
    Student friend = Student.builder().name("Friend").friend(entity).build();

    HashSet<String> selectedFields = new HashSet<>();
    selectedFields.add("name");
    selectedFields.add("iq");

    Map<Class<?>, Set<String>> selectedFieldPerClass = new HashMap<>();
    selectedFieldPerClass.put(Student.class, selectedFields);
    selectedFieldPerClass.put(StudentDto.class, selectedFields);

    HashSet<String> relations = new HashSet<>();
    relations.add("friend");

    StudentDto dto = mapper.toDto(friend, selectedFieldPerClass, relations);

    assertEquals(friend.getFriend().getName(), dto.getFriend().getName());
    assertEquals(friend.getFriend().getIq(), dto.getFriend().getIq());
  }

  @Test
  public void toDto_ResolversTest_FieldResolversMapping() {
    HashSet<String> selectedFields = new HashSet<>();
    selectedFields.add("customField");

    Map<Class<?>, Set<String>> selectedFieldPerClass = new HashMap<>();
    selectedFieldPerClass.put(Student.class, selectedFields);

    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, new HashSet<>());
    // Entity (ComplexObject.name) DTOs complex object (String)
    assertEquals(entity.getCustomField().getName(), dto.getCustomField());
  }

  private static void initEntity() {
    entity = Student.builder()
      .name("Test Name")
      .iq(9000)
      .customField(ComplexObject.builder().name("complex obj name").build())
      .build();
  }

  /*
   * Init Dto resolver to map the Entity complex object name (ComplexObject.name) to DTOs complex
   * object (String)
   */
  private static void initDtoResolvers() {
    CustomFieldResolverSpec<Student> customFieldResolver = CustomFieldResolverSpec.<Student>builder()
        .field("customField")
        .resolver(student -> student.getCustomField() == null ? "" : student.getCustomField().getName()).build();
    dtoResolvers.add(customFieldResolver);
  }
}
