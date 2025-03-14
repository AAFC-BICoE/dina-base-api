package ca.gc.aafc.dina.jsonapi;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a bulk request with only resource identifier (GET and DELETE).
 *
 */
@Jacksonized
@SuperBuilder
@Getter
public class JsonApiBulkResourceIdentifierDocument {

  @Singular("addData")
  private List<JsonApiDocument.ResourceIdentifier> data;
  private Map<String, Object> meta;

}
