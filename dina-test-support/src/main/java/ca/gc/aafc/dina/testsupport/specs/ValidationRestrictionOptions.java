package ca.gc.aafc.dina.testsupport.specs;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;

@Builder
@Getter
public class ValidationRestrictionOptions {

 public static final ValidationRestrictionOptions FULL_RESTRICTIONS = ValidationRestrictionOptions.builder()
   .allowAdditionalFields(false)
   .allowableMissingFields(Collections.emptySet())
   .build();

 private final boolean allowAdditionalFields;
 private final Set<String> allowableMissingFields;

}
