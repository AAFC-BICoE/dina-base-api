package ca.gc.aafc.dina.entities;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
@Entity
public class Department {

  @Id
  @GeneratedValue
  private Integer id;

  @Size(min = 1, max = 50)
  @NotNull
  private String name;

  @NotNull
  private String location;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "department", targetEntity = Employee.class)
  private List<Employee> employees;

}