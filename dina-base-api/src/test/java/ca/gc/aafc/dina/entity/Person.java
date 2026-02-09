package ca.gc.aafc.dina.entity;

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Person implements DinaEntityIdentifiableByName {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NaturalId
  private UUID uuid;

  private Integer room;

  @Size(max = 50)
  private String name;

  @Column(name = "group_name")
  private String group;

  @Column(name = "nick_names")
  //@Type(type = "string-array")
  private String[] nickNames;

  // Simulates a json:api optional field
  @Transient
  private String expensiveToCompute;

  // Simulates a calculated field
  @Transient
  private String augmentedData;

  private String createdBy;

  private OffsetDateTime createdOn;

  @OneToOne
  @JoinColumn(name = "department_id")
  private Department department;

  // one person could be the head backup of multiple departments but a department only has 1 head backup
  @OneToMany
  @JoinColumn(name = "department_head_backup_id")
  private List<Department> departmentsHeadBackup;
}
