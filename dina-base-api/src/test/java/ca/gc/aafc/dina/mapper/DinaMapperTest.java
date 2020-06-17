package ca.gc.aafc.dina.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.entity.ComplexObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DinaMapperTest {

  private static DinaMapper<StudentDto, Student> mapper = new DinaMapper<>(
    StudentDto.class,
    Student.class
  );

  @Test
  public void toDto_BaseAttributesTest_SelectedFieldsMapped() {
    Student entity = createEntity();

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      Student.class, ImmutableSet.of("name","nickNames"));
    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, new HashSet<>());

    assertEquals(entity.getName(), dto.getName());
    assertEquals(entity.getNickNames(), dto.getNickNames());
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
  public void toDto_CollectionRelation_RelationsMapped() {
    Student entityToMap = createEntity();
    entityToMap.getClassMates().addAll(Arrays.asList(createEntity(), createEntity(), createEntity()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(Student.class, ImmutableSet.of("name", "iq"));

    Set<String> relations = ImmutableSet.of("classMates");

    StudentDto result = mapper.toDto(entityToMap, selectedFieldPerClass, relations);

    for (int i = 0; i < entityToMap.getClassMates().size(); i++) {
      assertEquals(entityToMap.getClassMates().get(i).getName(), result.getClassMates().get(i).getName());
      assertEquals(entityToMap.getClassMates().get(i).getIq(), result.getClassMates().get(i).getIq());
    }
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
    assertNull(dto.getNickNames());
    assertEquals(0, dto.getIq());
  }

  @Test
  public void toDto_AllSelectedButNull_NullsMap() {
    Student entity = createEntity();
    entity.setName(null);
    entity.setFriend(null);
    entity.setNickNames(null);
    entity.setClassMates(null);

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      Student.class, ImmutableSet.of("name", "iq", "nickNames"));
    Set<String> relations = ImmutableSet.of("classMates", "friend");

    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, relations);

    assertNull(dto.getName());
    assertNull(dto.getFriend());
    assertNull(dto.getNickNames());
    assertNull(dto.getClassMates());
  }

  @Test
  public void applyDtoToEntity_BaseAttributesTest_SelectedFieldsMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    String expectedName = "expected name";
    dtoToMap.setName(expectedName);

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      StudentDto.class, ImmutableSet.of("name", "nickNames"));

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, new HashSet<>());

    assertEquals(expectedName, result.getName());
    assertEquals(dtoToMap.getNickNames(), result.getNickNames());
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
  public void applyDtoToEntity_NestedResolver_ResolverIgnored() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();
    dtoToMap.getClassMates().addAll(Arrays.asList(createDTO(), createDTO(), createDTO()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      StudentDto.class, ImmutableSet.of("customField"));
    Set<String> relations = ImmutableSet.of("friend", "classMates");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    assertNull(result.getFriend().getCustomField());
    assertNotNull(result.getClassMates());
    result.getClassMates().forEach(cm -> assertNull(cm.getCustomField()));
  }

  @Test
  public void applyDtoToEntity_NothingSelected_NothingMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    mapper.applyDtoToEntity(dtoToMap, result, new HashMap<>(), new HashSet<>());

    assertNull(result.getName());
    assertNull(result.getCustomField());
    assertNull(result.getFriend());
    assertNull(result.getNickNames());
    assertEquals(0, result.getIq());
  }

  @Test
  public void applyDtoToEntity_AllSelectedButNull_NullsMap() {
    Student result = createEntity();

    StudentDto dtoToMap = createDTO();
    dtoToMap.setName(null);
    dtoToMap.setFriend(null);
    dtoToMap.setNickNames(null);
    dtoToMap.setClassMates(null);

    Map<Class<?>, Set<String>> selectedFieldPerClass = ImmutableMap.of(
      StudentDto.class, ImmutableSet.of("name", "iq", "nickNames"));
    Set<String> relations = ImmutableSet.of("classMates", "friend");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    assertNull(result.getName());
    assertNull(result.getFriend());
    assertNull(result.getNickNames());
    assertNull(result.getClassMates());
  }

    @Test
  public void mapperInit_IncorrectResolverReturnTypes_ThrowsIllegalState() {
    assertThrows(
      IllegalStateException.class,
      ()-> new DinaMapper<>(IncorrectFieldResolversReturnType.class, Student.class));
  }

  @Test
  public void mapperInit_IncorrectResolverParaMeterCount_ThrowsIllegalState() {
    assertThrows(
      IllegalStateException.class,
      ()-> new DinaMapper<>(IncorrectFieldResolversParaCount.class, Student.class));
  }

  @Test
  public void mapperInit_IncorrectResolverParaMeterType_ThrowsIllegalState() {
    assertThrows(
      IllegalStateException.class,
      ()-> new DinaMapper<>(IncorrectResolversParaType.class, Student.class));
  }

  private static StudentDto createDTO() {
    StudentDto friend = StudentDto
      .builder()
      .customField("hello")
      .name(RandomStringUtils.random(5, true, false))
      .iq(RandomUtils.nextInt(5, 1000))
      .build();
    return StudentDto
      .builder()
      .nickNames(Arrays.asList("d","z","q").toArray(new String[0]))
      .name(RandomStringUtils.random(5, true, false))
      .iq(RandomUtils.nextInt(5, 1000))
      .customField(RandomStringUtils.random(5, true, false))
      .friend(friend)
      .classMates(new ArrayList<>())
      .build();
  }

  private static Student createEntity() {
    ComplexObject customField = ComplexObject
      .builder()
      .name(RandomStringUtils.random(5, true, false))
      .build();
    return Student.builder()
      .name(RandomStringUtils.random(5, true, false))
      .nickNames(Arrays.asList("a","b","c").toArray(new String[0]))
      .iq(RandomUtils.nextInt(5, 1000))
      .customField(customField)
      .classMates(new ArrayList<>())
      .build();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class StudentDto {

    private String name;

    private int iq;

    private String[] nickNames;

    // Relation to test
    private StudentDto friend;

    // Custom Resolved Field to test
    private String customField;

    // Many to - Relation to test
    private List<StudentDto> classMates;

    @CustomFieldResolver(field = "customField")
    public String customFieldToDto(Student entity) {
      return entity.getCustomField() == null ? "" : entity.getCustomField().getName();
    }

    @CustomFieldResolver(field = "customField")
    public ComplexObject customFieldToEntity(StudentDto entity) {
      return entity.getCustomField() == null ? null
          : ComplexObject.builder().name(entity.getCustomField()).build();
    }

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class Student {

    private String name;

    private int iq;

    private String[] nickNames;

    // Relation to test
    private Student friend;

    // Custom Resolved Field to test
    private ComplexObject customField;

    // Many to - Relation to test
    private List<Student> classMates;

  }

  /**
   * Class used to test invalid custom resolvers.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class IncorrectFieldResolversReturnType{

    private String customField;

    @CustomFieldResolver(field = "customField")
    public ComplexObject customFieldToDto(Student entity) {
      return null;
    }
  }

  
  /**
   * Class used to test invalid custom resolvers.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class IncorrectFieldResolversParaCount {

    private String customField;

    @CustomFieldResolver(field = "customField")
    public String customFieldToDto(Student entity, StudentDto dto) {
      return null;
    }
  }

  /**
   * Class used to test invalid custom resolvers.
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class IncorrectResolversParaType {

    private String customField;

    @CustomFieldResolver(field = "customField")
    public String customFieldToDto(String entity) {
      return null;
    }
  }
}
