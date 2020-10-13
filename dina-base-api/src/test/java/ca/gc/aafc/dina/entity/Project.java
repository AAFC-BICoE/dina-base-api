package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import java.time.OffsetDateTime;
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
}
