package ca.gc.aafc.dina.entity;

import java.io.Serializable;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import jakarta.validation.constraints.Size;

/**
 * Generic representation of an address (physical address).
 * No fields are mandatory at this level.
 */
@Data
@SuperBuilder
@Jacksonized
public class Address implements Serializable {

  @Size(max = 150)
  private String addressLine1;

  @Size(max = 150)
  private String addressLine2;

  @Size(max = 150)
  private String city;

  @Size(max = 150)
  private String provinceState;

  @Size(max = 50)
  private String zipCode;

  @Size(max = 50)
  private String country;

}
