package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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