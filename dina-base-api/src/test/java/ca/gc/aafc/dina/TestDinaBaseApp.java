package ca.gc.aafc.dina;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.SmartValidator;

import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.security.KeycloakClaimParser;
import ca.gc.aafc.dina.security.auth.GroupAuthorizationService;
import ca.gc.aafc.dina.security.oauth2.DinaAuthenticationToken;
import ca.gc.aafc.dina.service.DefaultDinaServiceTest.DinaServiceTestImplementation;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Small test application running on dina-base-api
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Department.class)
public class TestDinaBaseApp {

  @Inject
  private BuildProperties dinaTestBuildProperties;

  @Inject
  private GroupAuthorizationService groupAuthService;

//  @Bean
//  public JsonApiConfiguration jsonApiConfiguration() {
//    return new JsonApiConfiguration()
//      .withPluralizedTypeRendered(false)
//      .withPageMetaAutomaticallyCreated(false)
//      .withObjectMapperCustomizer(objectMapper -> {
//        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//        objectMapper.registerModule(new JavaTimeModule());
//      });
//  }

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
    DinaAuthenticationToken mockToken
  ) {
    // Mock the needed fields on the keycloak token:
    Mockito.when(mockToken.getName()).thenReturn("test-user");
    Mockito.when(mockToken.getUser()).thenReturn(toDinaUser(keycloakGroupClaim));
  }

  private static DinaAuthenticatedUser toDinaUser(List<String> keycloakGroupClaim) {
    return new DinaAuthenticatedUser("test-user",
      null,
      UUID.randomUUID().toString(),
      KeycloakClaimParser.parseGroupClaims(keycloakGroupClaim),
      Set.of(), false);
  }
}
