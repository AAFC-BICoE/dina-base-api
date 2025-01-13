package ca.gc.aafc.dina.entity;

import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @NaturalId
  private UUID uuid;
  private String name;
  private OffsetDateTime createdOn;
  private String createdBy;
  private String alias;

  //Internal to-one relationship
  @OneToOne
  @JoinColumn(name = "task_id")
  private Task task;

  //Internal to-many relationship
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "task_history",
    joinColumns = {@JoinColumn(name = "project_id")},
    inverseJoinColumns = { @JoinColumn(name = "task_id")}
  )
  private List<Task> taskHistory;

  // External Relation
  private UUID acMetaDataCreator;

  // External Relation
  private UUID originalAuthor;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private List<ComplexObject> nameTranslations;

  @Transient
  private List<UUID> authors;

  // list of people but not exposed as relationship
  @Transient
  private List<Person> randomPeople;

}
