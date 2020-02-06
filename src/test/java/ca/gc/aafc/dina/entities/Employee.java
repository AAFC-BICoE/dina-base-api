package ca.gc.aafc.dina.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Data
@Entity
public class Employee {

  @Id
  @GeneratedValue
  private Integer id;

  @Column(unique = true)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  private Department department;

}