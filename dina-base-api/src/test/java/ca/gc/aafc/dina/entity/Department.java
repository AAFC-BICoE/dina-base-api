package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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

  @ManyToOne
  @JoinColumn(name = "department_owner_id")
  private Person departmentOwner;

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

  // Simulates a possible jsonb field
  @Valid
  @Transient
  private List<DepartmentAlias> aliases;

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
