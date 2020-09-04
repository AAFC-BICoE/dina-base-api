package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vocabulary implements DinaEntity {

  @Id
  @GeneratedValue
  private Integer id;

  @NaturalId
  private UUID uuid;

  private String name;

  private String createdBy;

  private OffsetDateTime createdOn;


}
