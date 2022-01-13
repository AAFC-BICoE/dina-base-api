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
import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.NaturalId;

import ca.gc.aafc.dina.validation.ISOPartialDate;

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
  @NotNull
  private UUID uuid;

  @Size(min = 1, max = 50)
  private String name;

  @ManyToOne
  private DepartmentType departmentType;

  @NotNull(message = "{test.key.location}")
  private String location;

  @ManyToOne
  @JoinColumn(name = "department_head_id")
  private Person departmentHead;

  @Builder.Default
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "department", targetEntity = Employee.class)
  private List<Employee> employees = new ArrayList<>();

  private String createdBy;

  private OffsetDateTime createdOn;

  @ISOPartialDate
  private String establishedOn;

  // Simulates a possible jsonb field
  @Valid
  @Transient
  private DepartmentDetails departmentDetails;

  @Valid
  @Transient
  @Builder.Default
  private List<DepartmentAlias> aliases = new ArrayList<>();

  public String toString() {
    return super.toString();
  }

  @Data
  @AllArgsConstructor
  public static class DepartmentDetails {
    @Max(10)
    private String note;
  }

  @Data
  @AllArgsConstructor
  public static class DepartmentAlias {
    @Size(max = 100)
    @NotBlank
    private String name;
  }

}
