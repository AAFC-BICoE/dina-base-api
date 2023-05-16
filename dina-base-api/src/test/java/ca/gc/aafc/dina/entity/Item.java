package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Generic Item for testing purpose
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item implements DinaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @NaturalId
  private UUID uuid;

  @Column(name = "group_name")
  private String group;

  private String createdBy;
  private OffsetDateTime createdOn;

  private Boolean publiclyReleasable;

}