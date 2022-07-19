package ca.gc.aafc.dina.entity.ma;

import ca.gc.aafc.dina.entity.DinaEntity;
import lombok.Builder;
import lombok.Data;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class TestManagedAttributeUsage implements DinaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  private UUID uuid;

  private Map<String, String> managedAttributes;

  private String createdBy;
  private OffsetDateTime createdOn;
}