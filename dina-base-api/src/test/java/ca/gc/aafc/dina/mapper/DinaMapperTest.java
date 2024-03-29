package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.ComplexObject;
import ca.gc.aafc.dina.entity.Task;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

public class DinaMapperTest {

  private static final DinaMapper<StudentDto, Student> mapper = new DinaMapper<>(StudentDto.class);

  @Test
  public void simpleToDto_BaseAttributesTest_SelectedFieldsMapped() {
    Student friend = createEntity();
    Student entity = createEntity();
    entity.setFriend(friend);
    entity.getClassMates().addAll(Arrays.asList(createEntity(), createEntity(), createEntity()));

    StudentDto dto = mapper.toDto(entity);

    assertEquals(entity.getName(), dto.getName());
    assertEquals(entity.getIq(), dto.getIq());
    assertEquals(entity.getNickNames(), dto.getNickNames());
  }

  @Test
  public void simpleToDto_RelationShipTest_RelationsMapped() {
    Student friend = createEntity();
    Student entity = createEntity();
    Task unmarked = Task.builder().powerLevel(9000).build();
    entity.setFriend(friend);
    entity.setUnmarkedRelation(List.of(unmarked));
    entity.getClassMates().addAll(Arrays.asList(createEntity(), createEntity(), createEntity()));

    StudentDto dto = mapper.toDto(entity);

    // Assert non collection relation mapped
    assertEquals(friend.getName(), dto.getFriend().getName());
    assertEquals(friend.getIq(), dto.getFriend().getIq());
    assertEquals(friend.getNickNames(), dto.getFriend().getNickNames());

    // Assert collection relation mapped
    for (int i = 0; i < entity.getClassMates().size(); i++) {
      Student expectedClassMate = entity.getClassMates().get(i);
      StudentDto resultClassMate = dto.getClassMates().get(i);
      assertEquals(expectedClassMate.getName(), resultClassMate.getName());
      assertEquals(expectedClassMate.getIq(), resultClassMate.getIq());
      assertEquals(expectedClassMate.getNickNames(), resultClassMate.getNickNames());
    }
    // Assert custom fields mapped
    assertStudentCustomFields(entity, dto);
    // Assert unmarked related entity mapped
    assertEquals(unmarked.getPowerLevel(), dto.getUnmarkedRelation().get(0).getPowerLevel());
  }

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
  public void toDto_UnmarkedRelationShipTest_RelationsMapped() {
    Task unmarked = Task.builder().powerLevel(9000).build();
    Student friend = Student.builder().name("Friend").unmarkedRelation(List.of(unmarked)).build();

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Task.class,
      Set.of("powerLevel"));
    Set<String> relations = Set.of("unmarkedRelation");

    StudentDto dto = mapper.toDto(friend, selectedFieldPerClass, relations);

    assertEquals(unmarked.getPowerLevel(), dto.getUnmarkedRelation().get(0).getPowerLevel());
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

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(Student.class, Set.of());

    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, new HashSet<>());

    // Entity (ComplexObject.name) DTOs complex object (String)
    assertEquals(entity.getCustomField().getName(), dto.getCustomField());
  }

  @Test
  public void toDto_NestedResolver_ResolversMapped() {
    Student entityToMap = createEntity();
    entityToMap.setFriend(createEntity());
    entityToMap.getClassMates()
      .addAll(Arrays.asList(createEntity(), createEntity(), createEntity()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      Student.class, Set.of("name"),
      NestedResolverRelation.class, Set.of("customField"));
    Set<String> relations = Set.of("relationWithResolver", "friend", "classMates");

    StudentDto result = mapper.toDto(entityToMap, selectedFieldPerClass, relations);

    assertStudentCustomFields(entityToMap, result);
  }

  @Test
  public void toDto_NothingSelected_NothingMapped() {
    Student entity = createEntity();
    StudentDto dto = mapper.toDto(entity, Map.of(), Set.of());

    assertNull(dto.getName());
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

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(StudentDto.class, Set.of());

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, new HashSet<>());

    // DTOs complex object (String) -> Entity (ComplexObject.name)
    assertEquals(dtoToMap.getCustomField(), result.getCustomField().getName());
  }

  @Test
  public void applyDtoToEntity_NestedResolver_ResolversMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();
    dtoToMap.getClassMates().addAll(Arrays.asList(createDTO(), createDTO(), createDTO()));

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      StudentDto.class, Set.of("name"),
      NestedResolverRelationDTO.class, Set.of("customField"));
    Set<String> relations = Set.of("relationWithResolver", "friend", "classMates");

    mapper.applyDtoToEntity(dtoToMap, result, selectedFieldPerClass, relations);

    assertStudentCustomFields(result, dtoToMap);
  }

  @Test
  public void applyDtoToEntity_UnmarkedRelationShipTest_RelationsMapped() {
    TaskDTO unmarked = TaskDTO.builder().powerLevel(9000).build();
    StudentDto friend = StudentDto.builder().name("Friend").unmarkedRelation(List.of(unmarked)).build();

    Map<Class<?>, Set<String>> selectedFieldPerClass = Map.of(
      TaskDTO.class,
      Set.of("powerLevel"));
    Set<String> relations = Set.of("unmarkedRelation");

    Student result = new Student();
    mapper.applyDtoToEntity(friend, result, selectedFieldPerClass, relations);

    assertEquals(unmarked.getPowerLevel(), result.getUnmarkedRelation().get(0).getPowerLevel());
  }

  @Test
  public void applyDtoToEntity_NothingSelected_NothingMapped() {
    Student result = new Student();
    StudentDto dtoToMap = createDTO();

    mapper.applyDtoToEntity(dtoToMap, result, Map.of(), new HashSet<>());

    assertNull(result.getName());
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
  @CustomFieldAdapter(adapters = CustomFieldAdapterImp.class)
  public static final class StudentDto {

    private String name;

    private int iq;

    private String[] nickNames;

    // Relation to test
    @JsonApiRelation
    private StudentDto friend;

    @IgnoreDinaMapping(reason = "Custom resolved field to test")
    private String customField;

    // Relation with Custom Resolved Field to test
    @JsonApiRelation
    private NestedResolverRelationDTO relationWithResolver;

    // Many to - Relation to test
    @JsonApiRelation
    private List<StudentDto> classMates;

    // Relation with no related entity
    @JsonApiRelation
    private NoRelatedEntityDTO noRelatedEntityDTO;

    // unmarked maps to Related entity
    private List<TaskDTO> unmarkedRelation;

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
    private ComplexObject customField;

    // Relation with Custom Resolved Field to test
    private NestedResolverRelation relationWithResolver;

    // Many to - Relation to test
    private List<Student> classMates;

    private List<Task> unmarkedRelation;

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class NestedResolverRelation {

    // Custom Resolved Field to test
    private ComplexObject name;

    /**
     * Regular field but with the a name matching a custom resolved field on the parent
     */
    private int customField;

  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @RelatedEntity(NestedResolverRelation.class)
  @CustomFieldAdapter(adapters = NestedCustomFieldAdapterImp.class)
  public static final class NestedResolverRelationDTO {

    // Custom Resolved Field to test
    @IgnoreDinaMapping(reason = "Custom resolved field to test")
    private String name;

    /**
     * Regular field but with the a name matching a custom resolved field on the parent
     */
    private int customField;

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

  static class CustomFieldAdapterImp implements DinaFieldAdapter<StudentDto, Student, String, ComplexObject> {

    public CustomFieldAdapterImp() {
    }

    @Override
    public String toDTO(ComplexObject complexObject) {
      return complexObject == null ? "" : complexObject.getName();
    }

    @Override
    public ComplexObject toEntity(String s) {
      return s == null ? null : ComplexObject.builder().name(s).build();
    }

    @Override
    public Consumer<ComplexObject> entityApplyMethod(Student entityRef) {
      return entityRef::setCustomField;
    }

    @Override
    public Consumer<String> dtoApplyMethod(StudentDto dtoRef) {
      return dtoRef::setCustomField;
    }

    @Override
    public Supplier<ComplexObject> entitySupplyMethod(Student entityRef) {
      return entityRef::getCustomField;
    }

    @Override
    public Supplier<String> dtoSupplyMethod(StudentDto dtoRef) {
      return dtoRef::getCustomField;
    }
  }

  static class NestedCustomFieldAdapterImp
    implements DinaFieldAdapter<NestedResolverRelationDTO, NestedResolverRelation, String, ComplexObject> {

    public NestedCustomFieldAdapterImp() {
    }

    @Override
    public String toDTO(ComplexObject complexObject) {
      return complexObject == null ? "" : complexObject.getName();
    }

    @Override
    public ComplexObject toEntity(String s) {
      return s == null ? null : ComplexObject.builder().name(s).build();
    }

    @Override
    public Consumer<ComplexObject> entityApplyMethod(NestedResolverRelation entityRef) {
      return entityRef::setName;
    }

    @Override
    public Consumer<String> dtoApplyMethod(NestedResolverRelationDTO dtoRef) {
      return dtoRef::setName;
    }

    @Override
    public Supplier<ComplexObject> entitySupplyMethod(NestedResolverRelation entityRef) {
      return entityRef::getName;
    }

    @Override
    public Supplier<String> dtoSupplyMethod(NestedResolverRelationDTO dtoRef) {
      return dtoRef::getName;
    }
  }

}
