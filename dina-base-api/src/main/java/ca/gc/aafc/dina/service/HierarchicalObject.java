package ca.gc.aafc.dina.service;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HierarchicalObject {

  @JsonIgnore
  private Integer id;
  private UUID uuid;
  private String name;
  private Integer rank;
}
