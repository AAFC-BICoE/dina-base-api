package ca.gc.aafc.dina.entity;

import ca.gc.aafc.dina.entity.ManagedAttribute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class TestManagedAttribute implements ManagedAttribute {
  
  @Id
  @GeneratedValue
  private Integer id;
  private UUID uuid;
  private String name;
  private String key;
  private ManagedAttributeType managedAttributeType;
  private String[] acceptedValues;
  private String createdBy;
  private OffsetDateTime createdOn;
}
