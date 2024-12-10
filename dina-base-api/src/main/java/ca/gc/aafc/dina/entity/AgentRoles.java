package ca.gc.aafc.dina.entity;

import java.util.List;
import java.util.UUID;
import javax.validation.constraints.Size;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
