package ca.gc.aafc.dina.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
  private static List<CustomFieldResolverSpec<StudentDto>> entityResolvers = new ArrayList<>();

  private static DinaMapper<StudentDto, Student> mapper;

  @BeforeAll
  public static void init() {
    initEntity();
    initDtoResolvers();
    initEntityResolvers();
    mapper = new DinaMapper<>(StudentDto.class, Student.class, dtoResolvers, entityResolvers);
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
    assertNull(dto.getCustomField());
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

  @Test
  public void applyDtoToEntity_BaseAttributesTest_SelectedFieldsMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    HashSet<String> selectedFields = new HashSet<>();
    selectedFields.add("name");

    Map<Class<?>, Set<String>> selectedFieldPerClass = new HashMap<>();
    selectedFieldPerClass.put(StudentDto.class, selectedFields);

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, new HashSet<>());

    assertEquals(dtoToMap.getName(), result.getName());
    // Assert value not mapped - not included in selected fields
    assertEquals(0, result.getIq());
    assertNull(result.getCustomField());
  }

  @Test
  public void applyDtoToEntity_RelationShipTest_RelationsMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    HashSet<String> selectedFields = new HashSet<>();
    selectedFields.add("name");
    selectedFields.add("iq");

    Map<Class<?>, Set<String>> selectedFieldPerClass = new HashMap<>();
    selectedFieldPerClass.put(Student.class, selectedFields);
    selectedFieldPerClass.put(StudentDto.class, selectedFields);

    HashSet<String> relations = new HashSet<>();
    relations.add("friend");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    assertEquals(dtoToMap.getFriend().getName(), result.getFriend().getName());
    assertEquals(dtoToMap.getFriend().getIq(), result.getFriend().getIq());
  }

  @Test
  public void applyDtoToEntity_ResolversTest_FieldResolversMapping() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    HashSet<String> selectedFields = new HashSet<>();
    selectedFields.add("customField");

    Map<Class<?>, Set<String>> selectedFieldPerClass = new HashMap<>();
    selectedFieldPerClass.put(StudentDto.class, selectedFields);

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, new HashSet<>());

    // DTOs complex object (String) -> Entity (ComplexObject.name) 
    assertEquals(dtoToMap.getCustomField(), result.getCustomField().getName());
  }

  private StudentDto createDTO() {
    StudentDto dto = StudentDto
      .builder()
      .name("new Name")
      .iq(2700)
      .customField("customField")
      .friend(StudentDto.builder().name("best friend").iq(10000).build())
      .build();
    return dto;
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
        .resolver(student -> student.getCustomField() == null ? "" : student.getCustomField().getName())
        .build();
    dtoResolvers.add(customFieldResolver);
  }

  /*
   * Init Dto resolver to map the Entity complex object name (ComplexObject.name)
   * to DTOs complex object (String)
   */
  private static void initEntityResolvers() {
    CustomFieldResolverSpec<StudentDto> customFieldResolver = CustomFieldResolverSpec.<StudentDto>builder()
        .field("customField")
        .resolver(
          student -> student.getCustomField() == null ?
          null : ComplexObject.builder().name(student.getCustomField()).build())
        .build();
    entityResolvers.add(customFieldResolver);
  }
}
