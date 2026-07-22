package ca.gc.aafc.dina.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.config.PersonTestConfig;
import ca.gc.aafc.dina.dto.PermissionCheckDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.security.WithMockKeycloakUser;

import static com.toedter.spring.hateoas.jsonapi.MediaTypes.JSON_API_VALUE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.inject.Inject;
import java.util.Map;

@SpringBootTest(classes = {TestDinaBaseApp.class, DinaRepositoryV2IT.RepoV2TestConfig.class},
  properties = "keycloak.enabled: true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
@Import(PersonTestConfig.class)
public class PermissionCheckRepositoryIT {

  @Inject
  private ObjectMapper objMapper;

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  @WithMockKeycloakUser(groupRole = {"CNC:SUPER_USER", "CNC:USER", "CNC:GUEST"})
  public void onPermissionCheck_permissionsReturned() throws Exception {

    JsonApiDocument doc = JsonApiDocuments.createJsonApiDocument(
      null, PersonDTO.TYPE_NAME,
      Map.of()
    );

    var response = mockMvc.perform(
        post("/" + PermissionCheckDto.TYPE_NAME)
          .contentType(JSON_API_VALUE).
          content(objMapper.writeValueAsString(doc)))
      .andExpect(status().isCreated())
      .andReturn();

    JsonApiDocument returnedDoc =
      objMapper.readValue(response.getResponse().getContentAsString(), JsonApiDocument.class);

    assertNotNull(returnedDoc.getAttributes());
  }
}
