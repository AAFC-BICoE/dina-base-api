package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Chain Template is the definition of a workflow. It is the blueprint to how a workflow is
 * designed. This could also be known as the workflow type, such as SANGER or shotgun.
 *
 * When creating a brand new workflow instance, you need to select what type of workflow you are
 * creating. This entity is the type end users would select.
 */
@Entity
@Table(name = "ChainTemplates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChainTemplate implements DinaEntity {

  @Getter(onMethod = @__({
    @Id,
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    }))
  private Integer id;

  @Getter(onMethod = @__({
    @NotNull,
    @NaturalId
    }))
  private UUID uuid;

  private String createdBy;

  @Column(insertable = false, updatable = false)
  private OffsetDateTime createdOn;

  @Getter(onMethod = @__({
    @Column(name = "groupname")
  }))
  private String group;

  @NotNull
  @Size(max = 50)
  private String name;

}
