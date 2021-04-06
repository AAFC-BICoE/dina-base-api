package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;

import org.hibernate.annotations.NaturalId;

import ca.gc.aafc.dina.service.OnCreate;
import ca.gc.aafc.dina.service.OnUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department implements DinaEntity {

  @Id
  @GeneratedValue
  private Integer id;

  @NaturalId
  @Null(groups = OnCreate.class)
  @NotNull(groups = OnUpdate.class)
  private UUID uuid;

  @Size(min = 1, max = 50)
  private String name;
  
  @ManyToOne
  private DepartmentType departmentType;

  @NotNull
  private String location;

  @ManyToOne
  @JoinColumn(name = "department_head_id")
  private Person departmentHead;

  @Builder.Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "department", targetEntity = Employee.class)
  private List<Employee> employees = new ArrayList<>();

  private String createdBy;

  private OffsetDateTime createdOn;

  public String toString() {
    return super.toString();
  }

  @PrePersist
  public void initUuid() {
    this.uuid = UUID.randomUUID();
  }

}