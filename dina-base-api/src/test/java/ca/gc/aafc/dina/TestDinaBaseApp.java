package ca.gc.aafc.dina;

import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.validation.SmartValidator;

import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.security.auth.GroupAuthorizationService;
import ca.gc.aafc.dina.service.DefaultDinaServiceTest.DinaServiceTestImplementation;

import java.util.List;
import javax.inject.Inject;

/**
 * Small test application running on dina-base-api
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Department.class)
@Import(ExternalResourceProviderImplementation.class)
public class TestDinaBaseApp {

  @Inject
  private BuildProperties dinaTestBuildProperties;

  @Inject
  private GroupAuthorizationService groupAuthService;


  @Bean
  public DinaServiceTestImplementation serviceUnderTest(BaseDAO baseDAO, SmartValidator sv) {
    return new DinaServiceTestImplementation(baseDAO, sv);
  }


  /**
   * Mocks a given token to return a agent identifier and list of given groups.
   *
   * @param keycloakGroupClaim - groups to return in claim
   * @param mockToken          - token to mock
   */
  public static void mockToken(
    List<String> keycloakGroupClaim,
    KeycloakAuthenticationToken mockToken
  ) {
    // Mock the needed fields on the keycloak token:
    Mockito.when(mockToken.getName()).thenReturn("test-user");
    mockClaim(mockToken, "agent-identifier", "a2cef694-10f1-42ec-b403-e0f8ae9d2ae6");
    mockClaim(mockToken, "groups", keycloakGroupClaim);
  }

  /**
   * Mock a given tokens claims by returning a given value for the given claim key.
   *
   * @param token - token holding claims
   * @param key   - key of claim to mock
   * @param value - return value of the claim
   */
  public static void mockClaim(KeycloakAuthenticationToken token, String key, Object value) {
    Mockito.when(
      token.getAccount()
        .getKeycloakSecurityContext()
        .getToken()
        .getOtherClaims()
        .get(key))
      .thenReturn(value);
  }

}
