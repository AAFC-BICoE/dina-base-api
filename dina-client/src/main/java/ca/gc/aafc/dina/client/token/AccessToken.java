package ca.gc.aafc.dina.client.token;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Represents an access token response from an OpenId Connect endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class AccessToken {

  private String clientId;
  private String tokenType;
  private String accessToken;

  private String refreshToken;
  private int expiresIn;

}
