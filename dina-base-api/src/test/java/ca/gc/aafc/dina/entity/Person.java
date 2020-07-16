package ca.gc.aafc.dina.entity;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.annotations.NaturalId;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Person implements DinaEntity {

  @Id
  @GeneratedValue
  private Integer id;

  @NaturalId
  private UUID uuid;

  private String name;

  @Column(name = "group_name")
  private String group;

  private String[] nickNames;

  @OneToOne()
  @JoinColumn(name = "department_id")
  private Department department;

  @OneToMany
  @JoinColumn(name = "department_list_fk")
  private List<Department> departments;
}
