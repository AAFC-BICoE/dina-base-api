package ca.gc.aafc.dina.dto;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class HierarchicalObject implements Serializable {

  @JsonIgnore
  private Integer id;
  private UUID uuid;
  private String name;
  private Integer rank;
  private String type;
}
