package ca.gc.aafc.dina.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {

  private String name;

  private int iq;

  // Relation to test
  private Student friend;

  // Custom Resolved Field to test
  private ComplexObject customField;

  // Many to - Relation to test
  private List<Student> classMates;

}
