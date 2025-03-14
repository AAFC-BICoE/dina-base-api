package ca.gc.aafc.dina.jsonapi;

import java.util.List;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a json:api document for bulk extension that is not an official extension.
 */
@Jacksonized
@SuperBuilder
@Getter
public class JsonApiBulkDocument {

  @Singular("addData")
  private List<JsonApiDocument.ResourceObject> data;
}
