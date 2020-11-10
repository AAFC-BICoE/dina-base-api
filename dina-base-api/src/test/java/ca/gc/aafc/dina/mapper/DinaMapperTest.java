package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.ComplexObject;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

public class DinaMapperTest {

  private static final DinaMapper<StudentDto, Student> mapper = new DinaMapper<>(StudentDto.class);

  @Test
  public void toDto_BaseAttributesTest_SelectedFieldsMapped() {
    Student entity = createEntity();

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Student.class,
      Set.of("name", "nickNames"));
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

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Student.class,
      Set.of("name", "iq"));
    Set<String> relations = Set.of("friend");

    StudentDto dto = mapper.toDto(friend, selectedFieldPerClass, relations);

    assertEquals(friend.getFriend().getName(), dto.getFriend().getName());
    assertEquals(friend.getFriend().getIq(), dto.getFriend().getIq());
  }

  @Test
  public void toDto_CollectionRelation_RelationsMapped() {
    Student entityToMap = createEntity();
    entityToMap.getClassMates()
      .addAll(Arrays.asList(createEntity(), createEntity(), createEntity()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Student.class,
      Set.of("name", "iq"));

    Set<String> relations = Set.of("classMates");

    StudentDto result = mapper.toDto(entityToMap, selectedFieldPerClass, relations);

    for (int i = 0; i < entityToMap.getClassMates().size(); i++) {
      assertEquals(
        entityToMap.getClassMates().get(i).getName(),
        result.getClassMates().get(i).getName());
      assertEquals(
        entityToMap.getClassMates().get(i).getIq(),
        result.getClassMates().get(i).getIq());
    }
  }

  @Test
  public void toDto_ResolversTest_FieldResolversMapping() {
    Student entity = createEntity();

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Student.class,
      Set.of("customField", "oneSidedDto"));

    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, new HashSet<>());

    // Entity (ComplexObject.name) DTOs complex object (String)
    assertEquals(entity.getCustomField().getName(), dto.getCustomField());
    // One sided custom resolver mapping
    assertEquals(entity.iq, dto.getOneSidedDto());
  }

  @Test
  public void toDto_NestedResolver_ResolversMapped() {
    Student entityToMap = createEntity();
    entityToMap.setFriend(createEntity());
    entityToMap.getClassMates()
      .addAll(Arrays.asList(createEntity(), createEntity(), createEntity()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Student.class,
      Set.of("name", "customField"),
      NestedResolverRelation.class,
      Set.of("name", "customField"));
    Set<String> relations = Set.of("relationWithResolver", "friend", "classMates");

    StudentDto result = mapper.toDto(entityToMap, selectedFieldPerClass, relations);

    assertStudentCustomFields(entityToMap, result);
  }

  @Test
  public void toDto_NothingSelected_NothingMapped() {
    Student entity = createEntity();
    StudentDto dto = mapper.toDto(entity, Map.of(), Set.of());

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

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Student.class,
      Set.of("name", "iq", "nickNames"));
    Set<String> relations = Set.of("classMates", "friend");

    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, relations);

    assertNull(dto.getName());
    assertNull(dto.getFriend());
    assertNull(dto.getNickNames());
    assertNull(dto.getClassMates());
  }

  @Test
  public void toDto_CircularRelation_CircularRelationMapped() {
    Student entity = createEntity();
    Student friend = createEntity();

    entity.setFriend(friend);
    entity.getClassMates().add(friend);
    friend.setFriend(entity);
    friend.getClassMates().add(entity);

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Student.class,
      Set.of("name", "iq"));
    Set<String> relations = Set.of("friend", "classMates");

    StudentDto result = mapper.toDto(entity, selectedFieldPerClass, relations);
    assertEquals(friend.getName(), result.getFriend().getName());
    assertEquals(entity.getName(), result.getFriend().getFriend().getName());

    StudentDto resultClassmate = result.getClassMates().get(0);
    assertEquals(friend.getName(), resultClassmate.getName());
    assertEquals(entity.getName(), resultClassmate.getFriend().getName());
  }

  @Test
  public void toDto_FromHibernateProxy_FieldsMapped() {
    /* Mockable abstract class that works as a Hibernate-proxied Student entity. */
    abstract class MockStudentProxy extends Student implements HibernateProxy {
      private static final long serialVersionUID = 1L;
    }

    Student entity = createEntity();

    MockStudentProxy mockProxy = Mockito.mock(MockStudentProxy.class, RETURNS_DEEP_STUBS);
    Mockito.when(mockProxy.getHibernateLazyInitializer().getImplementation()).thenReturn(entity);
    StudentDto dto = mapper.toDto(
      mockProxy,
      Map.of(Student.class, Set.of("name", "iq")),
      Set.of()
    );

    assertEquals(entity.getName(), dto.getName());
    assertEquals(entity.getIq(), dto.getIq());
  }

  @Test
  public void applyDtoToEntity_BaseAttributesTest_SelectedFieldsMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    String expectedName = "expected name";
    dtoToMap.setName(expectedName);

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      StudentDto.class, Set.of("name", "nickNames"));

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

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      StudentDto.class, Set.of("name", "iq"));

    Set<String> relations = Set.of("friend");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    assertEquals(dtoToMap.getFriend().getName(), result.getFriend().getName());
    assertEquals(dtoToMap.getFriend().getIq(), result.getFriend().getIq());
  }

  @Test
  public void applyDtoToEntity_CollectionRelation_RelationsMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();
    dtoToMap.getClassMates().addAll(Arrays.asList(createDTO(), createDTO(), createDTO()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      StudentDto.class, Set.of("name", "iq"));

    Set<String> relations = Set.of("classMates");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    for (int i = 0; i < result.getClassMates().size(); i++) {
      assertEquals(
        result.getClassMates().get(i).getName(),
        dtoToMap.getClassMates().get(i).getName());
      assertEquals(result.getClassMates().get(i).getIq(), dtoToMap.getClassMates().get(i).getIq());
    }
  }

  @Test
  public void applyDtoToEntity_ResolversTest_FieldResolversMapping() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      StudentDto.class, Set.of("customField", "oneSided"));

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, new HashSet<>());

    // DTOs complex object (String) -> Entity (ComplexObject.name)
    assertEquals(dtoToMap.getCustomField(), result.getCustomField().getName());
    // One sided custom resolver mapping
    assertEquals(dtoToMap.getName(), result.getOneSided());
  }

  @Test
  public void applyDtoToEntity_NestedResolver_ResolversMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();
    dtoToMap.getClassMates().addAll(Arrays.asList(createDTO(), createDTO(), createDTO()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      StudentDto.class, Set.of("name", "customField"),
      NestedResolverRelationDTO.class, Set.of("name", "customField"));
    Set<String> relations = Set.of("relationWithResolver", "friend", "classMates");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    assertStudentCustomFields(result, dtoToMap);
  }

  @Test
  public void applyDtoToEntity_NothingSelected_NothingMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    mapper.applyDtoToEntity(dtoToMap, result, Map.of(), new HashSet<>());

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

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      StudentDto.class, Set.of("name", "iq", "nickNames"));
    Set<String> relations = Set.of("classMates", "friend");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    assertNull(result.getName());
    assertNull(result.getFriend());
    assertNull(result.getNickNames());
    assertNull(result.getClassMates());
  }

  @Test
  public void applyDtoToEntity_CircularRelation_CircularRelationMapped() {
    Student result = new Student();

    StudentDto dtoToMap = createDTO();
    StudentDto relationToMap = createDTO();

    dtoToMap.setFriend(relationToMap);
    dtoToMap.getClassMates().add(relationToMap);
    relationToMap.setFriend(dtoToMap);
    relationToMap.getClassMates().add(dtoToMap);

    Map<Class<?>, Set<String>> selectedFieldPerClass =
      Map.of(StudentDto.class, Set.of("name", "iq"));
    Set<String> relations = Set.of("friend", "classMates");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);
    assertEquals(relationToMap.getName(), result.getFriend().getName());
    assertEquals(dtoToMap.getName(), result.getFriend().getFriend().getName());

    Student resultClassmate = result.getClassMates().get(0);
    assertEquals(relationToMap.getName(), resultClassmate.getName());
    assertEquals(dtoToMap.getName(), resultClassmate.getFriend().getName());
  }

  @Test
  public void mapperInit_IncorrectResolverReturnTypes_ThrowsIllegalArgumentException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> new DinaMapper<>(ResolverWithBadReturnType.class));
  }

  @Test
  public void mapperInit_IncorrectResolverParaMeterType_ThrowsIllegalArgumentException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> new DinaMapper<>(ResolverWithBadParameter.class));
  }

  @Test
  public void mapperInit_IncorrectResolverParaMeterCount_ThrowsIllegalArgumentException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> new DinaMapper<>(ResolverWithBadParameterCount.class));
  }

  private static StudentDto createDTO() {
    NestedResolverRelationDTO relationWithResolver = NestedResolverRelationDTO.builder()
      .name(RandomStringUtils.random(5, true, false))
      .customField(RandomUtils.nextInt(5, 1000))
      .build();
    StudentDto friend = StudentDto
      .builder()
      .customField("hello")
      .name(RandomStringUtils.random(5, true, false))
      .iq(RandomUtils.nextInt(5, 1000))
      .build();
    return StudentDto
      .builder()
      .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
      .name(RandomStringUtils.random(5, true, false))
      .iq(RandomUtils.nextInt(5, 1000))
      .customField(RandomStringUtils.random(5, true, false))
      .relationWithResolver(relationWithResolver)
      .friend(friend)
      .classMates(new ArrayList<>())
      .build();
  }

  private static Student createEntity() {
    ComplexObject customField = ComplexObject
      .builder()
      .name(RandomStringUtils.random(5, true, false))
      .build();
    NestedResolverRelation relationWithResolver = NestedResolverRelation.builder()
      .name(customField)
      .customField(RandomUtils.nextInt(5, 1000))
      .build();
    return Student.builder()
      .name(RandomStringUtils.random(5, true, false))
      .nickNames(Arrays.asList("a", "b", "c").toArray(new String[0]))
      .iq(RandomUtils.nextInt(5, 1000))
      .customField(customField)
      .relationWithResolver(relationWithResolver)
      .classMates(new ArrayList<>())
      .build();
  }

  private static void assertStudentCustomFields(Student entity, StudentDto dto) {
    String expectedRelationCustomField = dto.getFriend().getCustomField();
    String resultRelationCustomField = entity.getFriend().getCustomField().getName();
    assertEquals(expectedRelationCustomField, resultRelationCustomField);

    assertNotNull(entity.getClassMates());
    for (int i = 0; i < dto.getClassMates().size(); i++) {
      String expectedCustomField = dto.getClassMates().get(i).getCustomField();
      String resultCustomField = entity.getClassMates().get(i).getCustomField().getName();
      assertEquals(expectedCustomField, resultCustomField);
    }

    NestedResolverRelationDTO expectedRelation = dto.getRelationWithResolver();
    NestedResolverRelation resultRelation = entity.getRelationWithResolver();
    assertEquals(expectedRelation.getName(), resultRelation.getName().getName());
    assertEquals(expectedRelation.getCustomField(), resultRelation.getCustomField());
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(Student.class)
  public static final class StudentDto {

    private String name;

    private int iq;

    private String[] nickNames;

    // Relation to test
    @JsonApiRelation
    private StudentDto friend;

    // Custom Resolved Field to test
    @CustomFieldResolver(setterMethod = "customFieldToDto")
    private String customField;

    // Custom resolved field on one side only
    @CustomFieldResolver(setterMethod = "oneSidedSetter")
    private int oneSidedDto;

    // Relation with Custom Resolved Field to test
    @JsonApiRelation
    private NestedResolverRelationDTO relationWithResolver;

    // Many to - Relation to test
    @JsonApiRelation
    private List<StudentDto> classMates;

    // Relation with no related entity
    @JsonApiRelation
    private NoRelatedEntityDTO noRelatedEntityDTO;

    public String customFieldToDto(Student entity) {
      return entity.getCustomField() == null ? "" : entity.getCustomField().getName();
    }

    public int oneSidedSetter(Student entity) {
      return entity.iq;
    }

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Student {

    private String name;

    private int iq;

    private String[] nickNames;

    // Relation to test
    private Student friend;

    // Custom Resolved Field to test
    @CustomFieldResolver(setterMethod = "customFieldToEntity")
    private ComplexObject customField;

    // Custom resolved field on one side only
    @CustomFieldResolver(setterMethod = "oneSidedSetter")
    private String oneSided;

    // Relation with Custom Resolved Field to test
    private NestedResolverRelation relationWithResolver;

    // Many to - Relation to test
    private List<Student> classMates;

    public ComplexObject customFieldToEntity(StudentDto dto) {
      return dto.getCustomField() == null ? null
        : ComplexObject.builder().name(dto.getCustomField()).build();
    }

    public String oneSidedSetter(StudentDto dto) {
      return dto.getName();
    }

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class NestedResolverRelation {

    // Custom Resolved Field to test
    @CustomFieldResolver(setterMethod = "nameToEntity")
    private ComplexObject name;

    /**
     * Regular field but with the a name matching a custom resolved field on the parent
     */
    private int customField;

    public ComplexObject nameToEntity(NestedResolverRelationDTO dto) {
      return dto.getName() == null ? null
        : ComplexObject.builder().name(dto.getName()).build();
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(NestedResolverRelation.class)
  public static final class NestedResolverRelationDTO {

    // Custom Resolved Field to test
    @CustomFieldResolver(setterMethod = "nameToDto")
    private String name;

    /**
     * Regular field but with the a name matching a custom resolved field on the parent
     */
    private int customField;

    public String nameToDto(NestedResolverRelation entity) {
      return entity.getName() == null ? "" : entity.getName().getName();
    }

  }

  /**
   * Relation without a related entity
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class NoRelatedEntityDTO {

    private String customField;

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(NestedResolverRelation.class)
  public static final class ResolverWithBadReturnType {

    @CustomFieldResolver(setterMethod = "nameToDto")
    private String name;

    public int nameToDto(NestedResolverRelation entity) {
      return 0;
    }

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(NestedResolverRelation.class)
  public static final class ResolverWithBadParameter {

    @CustomFieldResolver(setterMethod = "nameToDto")
    private String name;

    public String nameToDto(int entity) {
      return "0";
    }

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(NestedResolverRelation.class)
  public static final class ResolverWithBadParameterCount {

    @CustomFieldResolver(setterMethod = "nameToDto")
    private String name;

    public String nameToDto(NestedResolverRelation entity, NestedResolverRelation dto) {
      return "0";
    }

  }
}
