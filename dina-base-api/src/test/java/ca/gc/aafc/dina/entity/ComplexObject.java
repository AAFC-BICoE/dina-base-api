package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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
  @GeneratedValue
  private Integer id;

  private String name;

  private String createdBy;

  private OffsetDateTime createdOn;

}