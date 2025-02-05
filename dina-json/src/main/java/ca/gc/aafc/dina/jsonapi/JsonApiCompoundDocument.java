package ca.gc.aafc.dina.jsonapi;

import java.util.List;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a single json:api compound document (document with an included section).
 */
@Jacksonized
@SuperBuilder
@Getter
public class JsonApiCompoundDocument extends JsonApiDocument {

  private List<ResourceObject> included;

}
