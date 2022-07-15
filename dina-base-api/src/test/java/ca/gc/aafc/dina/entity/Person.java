package ca.gc.aafc.dina.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(name = "string-array", typeClass = StringArrayType.class)
public class Person implements DinaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NaturalId
  private UUID uuid;

  private Integer room;

  private String name;

  @Column(name = "group_name")
  private String group;

  @Column(name = "nick_names")
  @Type(type = "string-array")
  private String[] nickNames;

  private String createdBy;

  private OffsetDateTime createdOn;

  @OneToOne()
  @JoinColumn(name = "department_id")
  private Department department;

  @OneToMany
  @JoinColumn(name = "department_list_fk")
  private List<Department> departments;
}
