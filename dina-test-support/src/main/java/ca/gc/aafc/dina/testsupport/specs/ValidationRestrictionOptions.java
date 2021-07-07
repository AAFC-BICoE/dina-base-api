package ca.gc.aafc.dina.testsupport.specs;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;

/**
 * Options used to specify if additional fields are allowed and to specify which fields may remain missing.
 * Used with open api {@link RestrictiveFieldValidator}
 */
@Builder
@Getter
public class ValidationRestrictionOptions {

  /**
   * Allows no additional fields and no fields may be missing.
   */
  public static final ValidationRestrictionOptions FULL_RESTRICTIONS = ValidationRestrictionOptions.builder()
    .allowAdditionalFields(false)
    .allowableMissingFields(Collections.emptySet())
    .build();

  private final boolean allowAdditionalFields;
  private final Set<String> allowableMissingFields;

}
