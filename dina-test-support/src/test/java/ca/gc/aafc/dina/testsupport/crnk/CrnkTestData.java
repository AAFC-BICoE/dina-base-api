package ca.gc.aafc.dina.testsupport.crnk;

import java.util.UUID;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Data;

@Data
@JsonApiResource(type = "crnk-test-data")
public class CrnkTestData {

  @JsonApiId
  private UUID id;
  private String note;

}