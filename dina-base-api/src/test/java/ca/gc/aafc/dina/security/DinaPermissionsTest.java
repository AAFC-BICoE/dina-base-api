package ca.gc.aafc.dina.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.DinaRepository;

@Transactional
@SpringBootTest(classes = TestConfiguration.class, properties = "keycloak.enabled: true")
public class DinaPermissionsTest {

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Inject
  private BaseDAO baseDAO;

  private static final String GROUP_1 = "group 1";

  @BeforeEach
  public void beforeEach() {
    KeycloakAuthenticationToken mockToken = Mockito.mock(
      KeycloakAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    mockToken(Arrays.asList("/group 1/staff", "/group 2/collection-manager"), mockToken);

    SecurityContextHolder.getContext().setAuthentication(mockToken);
  }

  @Test
  public void create() {
    PersonDTO dto = PersonDTO.builder().uuid(UUID.randomUUID()).group(GROUP_1).name("name").build();
    PersonDTO result =  dinaRepository.create(dto);
    assertNotNull(result.getUuid());
  }

  /**
   * Mocks a given token to return a agent identifier and list of given groups.
   *
   * @param groupClaims
   *                             - groups to return in claim
   * @param mockToken
   *                             - token to mock
   */
  private static void mockToken(List<String> groupClaims, KeycloakAuthenticationToken mockToken) {
    // Mock the needed fields on the keycloak token:
    Mockito.when(mockToken.getName()).thenReturn("test-user");
    mockClaim(mockToken, "agent-identifier", "a2cef694-10f1-42ec-b403-e0f8ae9d2ae6");
    mockClaim(mockToken, "groups", groupClaims);
  }

  /**
   * Mock a given tokens claims by returning a given value for the given claim
   * key.
   *
   * @param token
   *                - token holding claims
   * @param key
   *                - key of claim to mock
   * @param value
   *                - return value of the claim
   */
  private static void mockClaim(KeycloakAuthenticationToken token, String key, Object value) {
    Mockito.when(
        token.getAccount()
          .getKeycloakSecurityContext()
          .getToken()
          .getOtherClaims()
          .get(key))
      .thenReturn(value);
  }

}
