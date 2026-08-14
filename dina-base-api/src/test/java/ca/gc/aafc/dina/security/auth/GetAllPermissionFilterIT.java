package ca.gc.aafc.dina.security.auth;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.mapper.PersonMapper;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.security.DinaRole;
import ca.gc.aafc.dina.security.oauth2.DinaAuthenticationToken;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The goal of this test it to make sur unauthorized resources are filtered out by DinaRepository
 */
@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, GetAllPermissionFilterIT.GetAllPermissionFilterConfig.class},
  properties = "keycloak.enabled: true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class GetAllPermissionFilterIT {

  @Inject
  private DinaRepositoryV2<PersonDTO, Person> dinaRepository;

  @Inject
  private DefaultDinaService<Person> personService;

  @Inject
  private BaseDAO baseDAO;

  private static final String GROUP_1 = "group 1";

  @BeforeEach
  public void beforeEach() {
    setupMockToken(GROUP_1, DinaRole.USER);
  }

  @Test
  public void getAll_unauthorizedGroup_notReturned() {
    PersonDTO dto = PersonDTO.builder().uuid(UUID.randomUUID()).group(GROUP_1).name("name").build();
    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(dto));

    PersonDTO result = dinaRepository.create(docToCreate, null).getDto();
    assertNotNull(result.getUuid());

    Person p = Person.builder().group("not" + GROUP_1)
      .uuid(UUID.randomUUID())
      .build();
    personService.create(p);

    var queryResult = dinaRepository.getAll("");
    assertEquals(1, queryResult.totalCount());

    p.setGroup(GROUP_1);
    personService.update(p);
    queryResult = dinaRepository.getAll("");
    assertEquals(2, queryResult.totalCount());
  }

  private static void setupMockToken(String group, DinaRole role) {
    DinaAuthenticationToken mockToken = Mockito.mock(
      DinaAuthenticationToken.class,
      Answers.RETURNS_DEEP_STUBS);
    TestDinaBaseApp.mockToken(List.of("/" + group + "/" + role), mockToken);

    SecurityContextHolder.getContext().setAuthentication(mockToken);
  }

  @TestConfiguration
  @EntityScan(basePackageClasses = DinaAdminCUDAuthTest.class)
  static class GetAllPermissionFilterConfig {

    // we can't use the repo from ItemTestConfig since we need GroupAuthorizationService
    @Bean
    @Primary
    public DinaRepositoryV2<PersonDTO, Person> testRepo(
      Optional<AuditService> auditService,
      GroupWithReadAuthorizationService authorizationService,
      BuildProperties buildProperties,
      DefaultDinaService<Person> defaultService, ObjectMapper objMapper
    ) {
      return new DinaRepositoryV2<>(
        defaultService,
        authorizationService,
        auditService,
        PersonMapper.INSTANCE,
        PersonDTO.class,
        Person.class,
        buildProperties, objMapper);
    }
  }
}
