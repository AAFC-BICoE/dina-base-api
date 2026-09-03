package ca.gc.aafc.dina.entity;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * Generic representation of an agent with one or multiple roles.
 * The agent must have at least 1 role.
 */
@Data
@SuperBuilder
public class AgentRoles implements Serializable {
  
  @NotNull
  private UUID agent;

  @NotEmpty
  private List<@NotBlank String> roles;

  @Size(max = 1000)
  private String remarks;

}
