package ca.gc.aafc.dina.entity;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Size;

/**
 * Generic representation of an address (physical address).
 * No fields are mandatory at this level.
 */
@Data
@Builder
public class Address {

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
