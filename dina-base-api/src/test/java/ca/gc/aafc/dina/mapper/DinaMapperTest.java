package ca.gc.aafc.dina.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.StudentDto;
import ca.gc.aafc.dina.entity.Student;

public class DinaMapperTest {

  private static DinaMapper<StudentDto, Student> mapper = new DinaMapper<>(StudentDto.class, Student.class);
  private static Student entity = Student.builder().name("Test Name").build();

  @Test
  public void toDto_BaseAttributesTest_AttributeMapped() {
    HashSet<String> selectedFields = new HashSet<>();
    selectedFields.add("name");

    Map<Class<?>, Set<String>> selectedFieldPerClass = new HashMap<>();
    selectedFieldPerClass.put(Student.class, selectedFields);

    StudentDto dto = mapper.toDto(entity, selectedFieldPerClass, new HashSet<>());

    assertEquals(entity.getName(), dto.getName());
  }

  @Test
  public void toDto_RelationShipTest_RelationsMapped() {
    Student friend = Student.builder().name("Friend").friend(entity).build();

    HashSet<String> selectedFields = new HashSet<>();
    selectedFields.add("name");

    Map<Class<?>, Set<String>> selectedFieldPerClass = new HashMap<>();
    selectedFieldPerClass.put(Student.class, selectedFields);
    selectedFieldPerClass.put(StudentDto.class, selectedFields);

    HashSet<String> relations = new HashSet<>();
    relations.add("friend");

    StudentDto dto = mapper.toDto(friend, selectedFieldPerClass, relations);

    assertEquals(entity.getName(), dto.getFriend().getName());
  }

}
