package ca.gc.aafc.dina.client;

import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import ca.gc.aafc.dina.client.token.AccessTokenManager;

/**
 * OkHttp3 Authenticator that supports OpenID Connect access token.
 *
 * Usage:
 *     OkHttpClient client = new OkHttpClient.Builder()
 *       .authenticator(new AccessTokenAuthenticator(new AccessTokenManager(openIdConfig)))
 *       .build();
 *
 */
public class AccessTokenAuthenticator implements Authenticator {

  private final AccessTokenManager accessTokenManager;

  public AccessTokenAuthenticator(AccessTokenManager accessTokenManager) {
    this.accessTokenManager = accessTokenManager;
  }

  @Override
  public Request authenticate(Route route, Response response) {
    final String accessToken;
    try {
      accessToken = accessTokenManager.getAccessToken();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (!isRequestWithAccessToken(response) || accessToken == null) {
      return null;
    }

    return newRequestWithBearerToken(response.request(), accessToken);
  }

  private boolean isRequestWithAccessToken(Response response) {
    String header = response.request().header("Authorization");
    return header != null && header.startsWith("Bearer");
  }

  private Request newRequestWithBearerToken(Request request, String accessToken) {
    return request.newBuilder()
      .header("Authorization", "Bearer " + accessToken)
      .build();
  }
}
