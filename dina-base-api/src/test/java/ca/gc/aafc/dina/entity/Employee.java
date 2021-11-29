package ca.gc.aafc.dina.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.NaturalId;

import ca.gc.aafc.dina.validation.ISOPartialDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee implements DinaEntity {

  @Id
  @GeneratedValue
  private Integer id;

  @NaturalId
  @NotNull
  private UUID uuid;

  @Column(unique = true)
  private String name;

  @Size(min = 1, max = 50)
  private String job;

  @ISOPartialDate
  private String employedOn;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "custom_field_id")
  private ComplexObject customField;

  @ManyToOne(fetch = FetchType.LAZY)
  private Department department;

  @Override
  public String getCreatedBy() {
    return null;
  }

  @Override
  public OffsetDateTime getCreatedOn() {
    return null;
  }
}