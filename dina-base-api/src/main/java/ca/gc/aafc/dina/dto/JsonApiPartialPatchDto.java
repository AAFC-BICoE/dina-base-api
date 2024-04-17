package ca.gc.aafc.dina.dto;

import java.util.UUID;
import lombok.Getter;

import org.apache.commons.beanutils.LazyDynaBean;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.toedter.spring.hateoas.jsonapi.JsonApiId;

/**
 * Special DTO used to received partial PATCH in JSON:API.
 * It stores properties using a LazyDynaBean to make sure we can distinguish between null and
 * value not provided (which we should leave untouched in JSON:API).
 */
public class JsonApiPartialPatchDto extends LazyDynaBean {

  @JsonApiId
  @Getter
  private UUID id;

  @Override
  @JsonAnySetter
  public void set(String name, Object value) {
    super.set(name, value);
  }

}
