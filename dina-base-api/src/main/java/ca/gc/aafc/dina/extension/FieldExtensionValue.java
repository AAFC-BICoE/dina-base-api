package ca.gc.aafc.dina.extension;

import lombok.Builder;
import lombok.Data;

import org.javers.core.metamodel.annotation.Value;

/**
 * Represents the usage of a field extension with its associated value.
 *
 */
@Data
@Builder
@Value
public class FieldExtensionValue {
  // used to report error on validation
  public static final String FIELD_KEY_NAME = "extFieldKey";
  public static final String VALUE_KEY_NAME = "value";

  private String extKey;
  private String extFieldKey;
  private String value;

}
