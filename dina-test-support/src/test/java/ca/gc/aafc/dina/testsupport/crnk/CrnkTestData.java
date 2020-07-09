package ca.gc.aafc.dina.testsupport.crnk;

import java.util.UUID;

import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonApiResource(type = "crnk-test-data")
public class CrnkTestData {

  @JsonApiId
  private UUID id;
  private String note;

}