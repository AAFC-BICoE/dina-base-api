package ca.gc.aafc.dina.client;

import java.io.IOException;
import okhttp3.Request;

import ca.gc.aafc.dina.client.token.AccessTokenManager;

/**
 * OkHttp Request builder that will use a AccessTokenManager to create a Request
 * with the Authorization header properly set.
 */
public class TokenBasedRequestBuilder {

  private final AccessTokenManager accessTokenManager;

  public TokenBasedRequestBuilder(AccessTokenManager accessTokenManager) {
    this.accessTokenManager = accessTokenManager;
  }

  public Request.Builder newBuilder() throws IOException {
    return new Request.Builder().header("Authorization", "Bearer " + accessTokenManager.getAccessToken());
  }

}
