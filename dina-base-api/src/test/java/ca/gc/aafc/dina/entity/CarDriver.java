package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarDriver implements DinaEntity {

  @Id
  private Integer id;

  @NaturalId
  private UUID uuid;

  private String createdBy;
  private OffsetDateTime createdOn;

  @OneToOne
  private JsonbCar car;
}