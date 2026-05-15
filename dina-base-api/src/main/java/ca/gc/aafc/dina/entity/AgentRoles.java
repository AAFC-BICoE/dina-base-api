package ca.gc.aafc.dina.entity;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * Generic representation of an agent with one or multiple roles.
 * The agent must have at least 1 role.
 */
@Data
@Builder
public class AgentRoles {
  
  @NotNull
  private UUID agent;

  @NotEmpty
  private List<@NotBlank String> roles;

  @Size(max = 1000)
  private String remarks;

}
