package ca.gc.aafc.dina.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

  @Id
  @GeneratedValue
  private Integer id;

  @Column(unique = true)
  private String name;

  private String job;

  @ManyToOne(fetch = FetchType.LAZY)
  private Department department;

  public String toString() {
    return super.toString();
  }

}