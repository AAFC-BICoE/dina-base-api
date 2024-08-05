package ca.gc.aafc.dina.client.token;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Retrofit based API call to an OpenId Connect endpoint to get or refresh tokens.
 */
public interface AccessTokenApiCall {

  @FormUrlEncoded
  @POST("token")
  Call<AccessToken> callAccessTokenEndpoint(@FieldMap Map<String, Object> accessTokenRequest);

}
