package ca.gc.aafc.dina.entity;

import ca.gc.aafc.dina.service.OnCreate;
import ca.gc.aafc.dina.service.OnUpdate;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The chain entity is an instance of a workflow definition (aka. Chain Template). It also contains
 * meta data about the chain. A chain needs to have a chain template which describes the workflow
 * being performed, and a group which has access to the workflow instance.
 *
 */
@Entity
@Table(name = "Chains")
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public class Chain implements DinaEntity {

  @Getter(onMethod = @__({
    @Id,
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    }))
  private Integer id;

  @Getter(onMethod = @__({
    @NotNull(groups = OnUpdate.class),
    @NaturalId,
    @Null(groups = OnCreate.class)
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

  @Getter(onMethod = @__({
    @NotNull,
    @ManyToOne(fetch = FetchType.LAZY),
    @JoinColumn(name = "chaintemplateid")
    }))
  private ChainTemplate chainTemplate;

  @NotNull
  private UUID agent;

}
