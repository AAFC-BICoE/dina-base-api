package ca.gc.aafc.dina.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.StudentDto;
import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Student;

public class DinaMapperTest {

  private static List<CustomFieldResolverSpec<Student>> dtoResolvers = new ArrayList<>();
  private static List<CustomFieldResolverSpec<StudentDto>> entityResolvers = new ArrayList<>();

  private static DinaMapper<StudentDto, Student> mapper;

  @BeforeAll
  public static void init() {
    initDtoResolvers();
    initEntityResolvers();
    mapper = new DinaMapper<>(StudentDto.class, Student.class, dtoResolvers, entityResolvers);
  }

  @Test
  public void toDto_BaseAttributesTest_SelectedFieldsMapped() {
    Student entity = createEntity();

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(Student.class, ImmutableSet.of("name"));
    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, new HashSet<>());

    assertEquals(entity.getName(), dto.getName());
    // Assert value not mapped - not included in selected fields
    assertEquals(0, dto.getIq());
    assertNull(dto.getCustomField());
  }

  @Test
  public void toDto_RelationShipTest_RelationsMapped() {
    Student entity = createEntity();
    Student friend = Student.builder().name("Friend").friend(entity).build();

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap
      .of(Student.class, ImmutableSet.of("name","iq"));
    Set<String> relations = ImmutableSet.of("friend");

    StudentDto dto = mapper.toDto(friend, selectedFieldPerClass, relations);

    assertEquals(friend.getFriend().getName(), dto.getFriend().getName());
    assertEquals(friend.getFriend().getIq(), dto.getFriend().getIq());
  }

  @Test
  public void toDto_ResolversTest_FieldResolversMapping() {
    Student entity = createEntity();

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      Student.class, ImmutableSet.of("customField"));

    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, new HashSet<>());

    // Entity (ComplexObject.name) DTOs complex object (String)
    assertEquals(entity.getCustomField().getName(), dto.getCustomField());
  }

  @Test
  public void toDto_NothingSelected_NothingMapped() {
    Student entity = createEntity();
    StudentDto dto = mapper.toDto(entity, new HashMap<>(), new HashSet<>());

    assertNull(dto.getName());
    assertNull(dto.getCustomField());
    assertNull(dto.getFriend());
    assertEquals(0, dto.getIq());
  }

  @Test
  public void applyDtoToEntity_BaseAttributesTest_SelectedFieldsMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    String expectedName = "expected name";
    dtoToMap.setName(expectedName);

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      StudentDto.class, ImmutableSet.of("name"));

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, new HashSet<>());

    assertEquals(expectedName, result.getName());
    // Assert value not mapped - not included in selected fields
    assertEquals(0, result.getIq());
    assertNull(result.getCustomField());
  }

  @Test
  public void applyDtoToEntity_RelationShipTest_RelationsMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      StudentDto.class, ImmutableSet.of("name", "iq"));

    Set<String> relations = ImmutableSet.of("friend");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    assertEquals(dtoToMap.getFriend().getName(), result.getFriend().getName());
    assertEquals(dtoToMap.getFriend().getIq(), result.getFriend().getIq());
  }

  @Test
  public void applyDtoToEntity_CollectionRelation_RelationsMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();
    dtoToMap.getClassMates().addAll(Arrays.asList(createDTO(), createDTO(), createDTO()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      StudentDto.class, ImmutableSet.of("name", "iq"));

    Set<String> relations = ImmutableSet.of("classMates");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    for (int i = 0; i < result.getClassMates().size(); i++) {
      assertEquals(result.getClassMates().get(i).getName(), dtoToMap.getClassMates().get(i).getName());
      assertEquals(result.getClassMates().get(i).getIq(), dtoToMap.getClassMates().get(i).getIq());
    }
  }

  @Test
  public void applyDtoToEntity_ResolversTest_FieldResolversMapping() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      StudentDto.class, ImmutableSet.of("customField"));

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, new HashSet<>());

    // DTOs complex object (String) -> Entity (ComplexObject.name)
    assertEquals(dtoToMap.getCustomField(), result.getCustomField().getName());
  }

  @Test
  public void applyDtoToEntity_NothingSelected_NothingMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    mapper.applyDtoToEntity(dtoToMap, result, new HashMap<>(), new HashSet<>());

    assertNull(result.getName());
    assertNull(result.getCustomField());
    assertNull(result.getFriend());
    assertEquals(0, result.getIq());
  }

  private static StudentDto createDTO() {
    return StudentDto
      .builder()
      .name("new Name")
      .iq(2700)
      .customField("customField")
      .friend(StudentDto.builder().name("best friend").iq(10000).build())
      .classMates(new ArrayList<>())
      .build();
  }

  private static Student createEntity() {
    return Student.builder()
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
