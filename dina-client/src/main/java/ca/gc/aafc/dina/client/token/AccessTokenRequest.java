package ca.gc.aafc.dina.client.token;

import java.util.Map;

import ca.gc.aafc.dina.client.config.OpenIdConnectConfig;

public class AccessTokenRequest {

  private enum GrantType {PASSWORD, REFRESH_TOKEN}

  private final String clientId;
  private final GrantType grantType;

  private final String username;
  private final String password;

  private final String refreshToken;

  public static AccessTokenRequest newPasswordBased(String clientId, String username, String password) {
    return new AccessTokenRequest(GrantType.PASSWORD, clientId, username, password, null);
  }

  public static AccessTokenRequest newPasswordBased(OpenIdConnectConfig config) {
    return new AccessTokenRequest(GrantType.PASSWORD, config.getClientId(), config.getUsername(), config.getPassword(), null);
  }

  public static AccessTokenRequest newRefreshTokenBased(String clientId, String refreshToken) {
    return new AccessTokenRequest(GrantType.REFRESH_TOKEN, clientId, null, null, refreshToken);
  }

  private AccessTokenRequest(GrantType grantType, String clientId, String username, String password, String refreshToken) {
    this.grantType = grantType;
    this.clientId = clientId;
    this.username = username;
    this.password = password;
    this.refreshToken = refreshToken;
  }

  public Map<String, Object> toFieldMap() {
    if (grantType == GrantType.PASSWORD) {
      return Map.of(
        "client_id", clientId,
        "grant_type", grantType.name().toLowerCase(),
        "username", username,
        "password", password);
    } else if (grantType == GrantType.REFRESH_TOKEN) {
      return Map.of(
        "client_id", clientId,
        "grant_type", grantType.name().toLowerCase(),
        "refresh_token", refreshToken);
    }
    return null;
  }

}
