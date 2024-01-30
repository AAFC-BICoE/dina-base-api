package ca.gc.aafc.dina.client.token;

import ca.gc.aafc.dina.client.config.OpenIdConnectConfig;

import java.io.IOException;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Encapsulate the logic to acquire and refresh tokens.
 */
@Log4j2
public class AccessTokenManager {

  private static final int BUFFER_IN_SEC = 10;

  private final AccessTokenApiCall accessTokenApiCall;
  private final OpenIdConnectConfig config;

  private String accessToken;
  // in seconds
  private int expiresIn;
  private String refreshToken;
  private Instant tokenInstant;

  public AccessTokenManager(OpenIdConnectConfig openIdConnectConfig) {
    Retrofit retrofit = new Retrofit.Builder()
      .baseUrl(openIdConnectConfig.getOpenIdConnectBaseUrl())
      .addConverterFactory(JacksonConverterFactory.create())
      .build();

    accessTokenApiCall = retrofit.create(AccessTokenApiCall.class);
    this.config = openIdConnectConfig;
  }

  public synchronized String getAccessToken() throws IOException {

    // check if we already have a token
    if (accessToken == null) {
      acquireAccessToken();
      return accessToken;
    }

    boolean isAlmostExpired = Instant.now().isAfter(tokenInstant.plusSeconds(expiresIn - BUFFER_IN_SEC));
    if(!isAlmostExpired) {
      return accessToken;
    } else {
      if(!refreshAccessToken()) {
        acquireAccessToken();
      }
    }

    return accessToken;
  }

  private boolean acquireAccessToken() throws IOException {
    log.debug("Acquire token");
    Call<AccessToken> accessTokenCall = accessTokenApiCall.callAccessTokenEndpoint(
      AccessTokenRequest.newPasswordBased(config).toFieldMap());
    Response<AccessToken> accessTokenResponse = accessTokenCall.execute();

    if(!accessTokenResponse.isSuccessful()) {
      accessToken = null;
      return false;
    }

    AccessToken token = accessTokenResponse.body();
    accessToken = token.getAccessToken();
    expiresIn = token.getExpiresIn();
    refreshToken = token.getRefreshToken();
    tokenInstant = Instant.now();
    return true;
  }

  private boolean refreshAccessToken() throws IOException {
    log.debug("Refresh token");

    Call<AccessToken> accessTokenCall = accessTokenApiCall.callAccessTokenEndpoint(
      AccessTokenRequest.newRefreshTokenBased(config.getClientId(), refreshToken)
        .toFieldMap());

    Response<AccessToken> accessTokenResponse = accessTokenCall.execute();
    log.debug("Refreshing token successful: {}", accessTokenResponse::isSuccessful);

    if(!accessTokenResponse.isSuccessful()) {
      return false;
    }

    AccessToken token = accessTokenResponse.body();
    accessToken = token.getAccessToken();
    expiresIn = token.getExpiresIn();
    refreshToken = token.getRefreshToken();
    tokenInstant = Instant.now();
    return true;
  }
}
