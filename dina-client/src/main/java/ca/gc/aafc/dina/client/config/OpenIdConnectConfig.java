package ca.gc.aafc.dina.client.config;

import lombok.Data;

@Data
public class OpenIdConnectConfig {

  /**
   * for Keycloak it should look like .../realms/dina/protocol/openid-connect/
   */
  private String openIdConnectBaseUrl;

  private String clientId;
  private String username;
  private String password;

}
