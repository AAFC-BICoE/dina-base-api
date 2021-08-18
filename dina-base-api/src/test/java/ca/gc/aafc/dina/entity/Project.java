package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class Project implements DinaEntity {
  @Id
  @GeneratedValue
  private Integer id;
  @NaturalId
  private UUID uuid;
  private String name;
  private OffsetDateTime createdOn;
  private String createdBy;
  //Internal Relation
  @OneToOne
  @JoinColumn(name = "task_id")
  private Task task;
  // External Relation
  private UUID acMetaDataCreator;
  // External Relation
  private UUID originalAuthor;
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private List<ComplexObject> nameTranslations;
  @Transient
  private List<UUID> authors;
  @Transient
  private List<Person> hiddenRelation;
}
