package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;

import org.hibernate.annotations.NaturalId;

import ca.gc.aafc.dina.service.OnCreate;
import ca.gc.aafc.dina.service.OnUpdate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexObject implements DinaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NaturalId
  @Null(groups = OnCreate.class)
  @NotNull(groups = OnUpdate.class)
  private UUID uuid;

  private String name;

  private String createdBy;

  private OffsetDateTime createdOn;

}