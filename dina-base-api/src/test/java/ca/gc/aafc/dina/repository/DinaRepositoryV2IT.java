package ca.gc.aafc.dina.repository;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toedter.spring.hateoas.jsonapi.JsonApiConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.exception.ResourceGoneException;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.exception.ResourcesGoneException;
import ca.gc.aafc.dina.exception.ResourcesNotFoundException;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkResourceIdentifierDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.mapper.PersonMapper;
import ca.gc.aafc.dina.security.auth.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.factories.TestableEntityFactory;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import static ca.gc.aafc.dina.repository.DinaRepository.IT_OM_TYPE_REF;
import static com.toedter.spring.hateoas.jsonapi.MediaTypes.JSON_API_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import lombok.NonNull;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, DinaRepositoryV2IT.RepoV2TestConfig.class},
  properties = "dina.auditing.enabled = true")
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaRepositoryV2IT {

  @Autowired
  private DinaRepositoryV2<PersonDTO, Person> repositoryV2;

  @Inject
  private DinaRepositoryIT.DinaPersonService personService;

  @Inject
  private DinaRepositoryV2IT.RepoV2TestConfig.DinaDepartmentService departmentService;

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
  public void onGetAll_noError() {
    repositoryV2.getAll("");
  }

  @Test
  public void findOne_onIncludePermissions_permissionMetaIncluded() throws ResourceGoneException, ResourceNotFoundException {
    Person person = personService.create(Person.builder()
      .name(TestableEntityFactory.generateRandomNameLettersOnly(11))
      .room(38)
      .build());

    JsonApiDto<PersonDTO> dto = repositoryV2.getOne(person.getUuid(), null, true);

    assertNotNull(dto.getMeta());
    assertNotNull(dto.getMeta().getPermissionsProvider());
  }

  @Test
  public void findOne_onSparseFieldSet_fieldsIncluded() throws Exception {

    Department department = departmentService.create(Department.builder()
      .uuid(UUID.randomUUID())
      .name(RandomStringUtils.randomAlphabetic(4))
      .location(RandomStringUtils.randomAlphabetic(4))
      .build());

    Person person = personService.create(Person.builder()
      .name(TestableEntityFactory.generateRandomNameLettersOnly(11))
      .room(39)
      .department(department)
      .build());

    var getResponse = mockMvc.perform(
        get("/" + RepoV2TestConfig.PATH + "/" + person.getUuid() +
          "?fields[person]=name&fields[department]=name&include=department")
          .contentType(JSON_API_VALUE)
      )
      .andExpect(status().isOk())
      .andReturn();

    assertTrue(getResponse.getResponse().getContentAsString().contains("name"));
    assertFalse(getResponse.getResponse().getContentAsString().contains("room"));

    // we are asking for fields[department]=name so location should not be included
    assertFalse(getResponse.getResponse().getContentAsString().contains("location"));
  }

  @Test
  public void findOne_onOptionalFields_fieldsIncluded() throws Exception {

    Person person = personService.create(Person.builder()
      .name(TestableEntityFactory.generateRandomNameLettersOnly(11))
      .room(39)
      .build());

    var getResponse = mockMvc.perform(
        get("/" + RepoV2TestConfig.PATH + "/" + person.getUuid() +
          "?optfields[person]=expensiveToCompute")
          .contentType(JSON_API_VALUE)
      )
      .andExpect(status().isOk())
      .andReturn();

    assertTrue(getResponse.getResponse().getContentAsString().contains(DinaRepositoryIT.EXPENSIVE_VALUE_TO_COMPUTE));
  }

  @Test
  public void findAll_SortingByName_ReturnsSortedCaseSensitiveOrNot() {

    List<String> shuffledNames = Arrays.asList("b", "a", "d", "C");
    List<Integer> matchingRooms = Arrays.asList(6, 2, 1, 11);

    List<String> namesCaseInsensitive = Arrays.asList("a", "b", "C", "d");
    List<String> reversed = new ArrayList<>(namesCaseInsensitive);
    Collections.reverse(reversed);
    List<String> namesCaseSensitive = Arrays.asList("C", "a", "b", "d");
    List<String> byRoom = Arrays.asList("d", "a", "b", "C");

    for (int i=0; i < shuffledNames.size(); i++) {
      personService.create(Person.builder()
        .name(shuffledNames.get(i))
        .room(matchingRooms.get(i))
        .build());
    }

    QueryComponent qc = QueryComponent.builder()
      .sorts(List.of("name"))
      .build();

    //ASC, case insensitive by default
    DinaRepositoryV2.PagedResource<JsonApiDto<PersonDTO>> resultList = repositoryV2.getAll(qc);
    for (int i = 0; i < namesCaseInsensitive.size(); i++) {
      assertEquals(namesCaseInsensitive.get(i), resultList.resourceList().get(i).getDto().getName());
    }

    //test DESC
    qc = QueryComponent.builder()
      .sorts(List.of("-name"))
      .build();
    resultList = repositoryV2.getAll(qc);
    for (int i = 0; i < reversed.size(); i++) {
      assertEquals(reversed.get(i), resultList.resourceList().get(i).getDto().getName());
    }

    // sorting on non text field
    qc = QueryComponent.builder()
      .sorts(List.of("room"))
      .includes(Set.of("department"))
      .pageLimit(1)
      .build();

    resultList = repositoryV2.getAll(qc);
    assertEquals(byRoom.getFirst(), resultList.resourceList().getFirst().getDto().getName());
    assertEquals(byRoom.size(), resultList.totalCount());
  }

  @Test
  public void findAll_fiqlFilter_returnExpectedRecord() {

    for (int i = 0; i < 5; i++) {
      personService.create(Person.builder()
        .name("fiql test name " + i)
        .room(i)
        .build());
    }

    personService.create(Person.builder()
      .name("test name A")
      .room(6)
      .build());

    // fiql
    QueryComponent qc = QueryComponent.builder().fiql("(name==fiql test name 2),(name==fiql test name 4)")
      .sorts(List.of("-name"))
      .build();
    DinaRepositoryV2.PagedResource<JsonApiDto<PersonDTO>> resultList = repositoryV2.getAll(qc);
    assertEquals(2, resultList.resourceList().size());
    assertEquals("fiql test name 4", resultList.resourceList().getFirst().getDto().getName());

    qc = QueryComponent.builder().fiql("name==fiql*")
      .sorts(List.of("-name"))
      .build();
    resultList = repositoryV2.getAll(qc);
    assertEquals(5, resultList.resourceList().size());
    assertEquals(5, resultList.totalCount());
    assertEquals("fiql test name 4", resultList.resourceList().getFirst().getDto().getName());
  }

  @Test
  public void onCreateUpdateDelete_noException() throws ResourceNotFoundException,
      ResourceGoneException {

    PersonDTO personDto = PersonDTO.builder()
      .name("Bob")
      .group("g1")
      .build();

    JsonApiDocument doc = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(personDto));

    var created = repositoryV2.handleCreate(doc, null);
    UUID assignedId = JsonApiModelAssistant.extractUUIDFromRepresentationModelLink(created);

    Map<String, Object> attributes = new HashMap<>();
    attributes.put("name", "abc");
    attributes.put("room", 21);
    attributes.put("group", "g2");
    // convert to string to mimic how we would get it with json:api
    attributes.put("createdOn", OffsetDateTime.now().toString());

    JsonApiDocument document = JsonApiDocument.builder()
      .data(JsonApiDocument.ResourceObject.builder()
        .id(assignedId)
        .attributes(attributes)
        .build())
      .build();
    repositoryV2.handleUpdate(document, assignedId);

    JsonApiDto<PersonDTO> getOneDto = repositoryV2.getOne(assignedId, null);
    assertEquals("abc", getOneDto.getDto().getName());

    assertEquals("g1", getOneDto.getDto().getGroup());

    var handleFindOneResponse = repositoryV2.handleFindOne(assignedId, null);
    assertEquals(HttpStatus.OK, handleFindOneResponse.getStatusCode());

    repositoryV2.handleDelete(assignedId);
  }

  @Test
  public void onBulk_noException() throws Exception {
    PersonDTO personDto1 = PersonDTO.builder()
      .name("Bob 1 Bulk")
      .build();

    PersonDTO personDto2 = PersonDTO.builder()
      .name("Bob 2 Bulk")
      .build();

    JsonApiDocument doc1 = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(personDto1));

    JsonApiDocument doc2 = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(personDto2));

    JsonApiBulkDocument bulkCreateDocument = JsonApiBulkDocument.builder()
      .data(List.of(doc1.getData(), doc2.getData())).build();

    // bulk create the 2 documents
    var createResponse = mockMvc.perform(
        post("/" + RepoV2TestConfig.PATH)
          .contentType(DinaRepositoryV2.JSON_API_BULK)
          .content(objMapper.writeValueAsString(bulkCreateDocument))
      )
      .andExpect(status().isOk())
      .andReturn();

    JsonApiBulkDocument createdDocs = objMapper.readValue(createResponse.getResponse().getContentAsString(),
      JsonApiBulkDocument.class);

    assertEquals(2, createdDocs.getData().size());
    List<UUID> createdUUIDs = createdDocs.getData().stream().map(
      JsonApiDocument.ResourceObject::getId).toList();

    var bulkUpdateDocument = JsonApiBulkDocument.builder();
    byte i = 1;
    for (UUID uuid : createdUUIDs) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put("name", "updated " + i);
      bulkUpdateDocument.addData(JsonApiDocument.ResourceObject.builder()
        .id(uuid)
        .attributes(attributes)
        .build());
    }

    // bulk update the 2 documents
    var updateResponse = mockMvc.perform(
        patch("/" + RepoV2TestConfig.PATH)
          .contentType(DinaRepositoryV2.JSON_API_BULK)
          .content(objMapper.writeValueAsString(bulkUpdateDocument.build()))
      )
      .andExpect(status().isOk())
      .andReturn();

    JsonApiBulkDocument updatedDocs = objMapper.readValue(updateResponse.getResponse().getContentAsString(),
      JsonApiBulkDocument.class);
    assertEquals(2, updatedDocs.getData().size());

    var bulkLoadDocument = JsonApiBulkResourceIdentifierDocument.builder();
    for (UUID uuid : createdUUIDs) {
      bulkLoadDocument.addData(JsonApiDocument.ResourceIdentifier.builder()
        .type(PersonDTO.TYPE_NAME)
        .id(uuid)
        .build());
    }

    var bulkLoadResponse = mockMvc.perform(
        post("/" + RepoV2TestConfig.PATH + "/" + DinaRepositoryV2.JSON_API_BULK_LOAD_PATH)
          .contentType(DinaRepositoryV2.JSON_API_BULK)
          .content(objMapper.writeValueAsString(bulkLoadDocument.build()))
      )
      .andExpect(status().isOk())
      .andReturn();

    var bulkDeleteResponse = mockMvc.perform(
        delete("/" + RepoV2TestConfig.PATH)
          .contentType(DinaRepositoryV2.JSON_API_BULK)
          .content(objMapper.writeValueAsString(bulkLoadDocument.build()))
      )
      .andExpect(status().isNoContent())
      .andReturn();
  }

  @Test
  public void onBulkLoadNonExisting_NotFoundError() throws Exception {

    var bulkLoadDocument = JsonApiBulkResourceIdentifierDocument.builder();
    bulkLoadDocument.addData(JsonApiDocument.ResourceIdentifier.builder()
      .type(PersonDTO.TYPE_NAME)
      .id(UUID.randomUUID())
      .build());

    var response = mockMvc.perform(
        post("/" + RepoV2TestConfig.PATH + "/" + DinaRepositoryV2.JSON_API_BULK_LOAD_PATH)
          .contentType(DinaRepositoryV2.JSON_API_BULK)
          .content(objMapper.writeValueAsString(bulkLoadDocument.build())))
      .andExpect(status().isNotFound())
      .andReturn();

    Map<String, Object> loadedDocs = objMapper.readValue(response.getResponse().getContentAsString(),
      IT_OM_TYPE_REF);
    assertNotNull(loadedDocs.get("errors"));
  }

  @Test
  public void onBulkLoadDeleted_GoneError() throws Exception {

    PersonDTO personDto1 = PersonDTO.builder()
      .name("Bob test onBulkLoadDeleted_GoneError")
      .build();
    JsonApiDocument doc1 = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(personDto1));

    var created = repositoryV2.handleCreate(doc1, null);
    UUID assignedId = JsonApiModelAssistant.extractUUIDFromRepresentationModelLink(created);

    repositoryV2.handleDelete(assignedId);

    var bulkLoadDocument = JsonApiBulkResourceIdentifierDocument.builder();
    bulkLoadDocument.addData(JsonApiDocument.ResourceIdentifier.builder()
      .type(PersonDTO.TYPE_NAME)
      .id(assignedId)
      .build());

    var response = mockMvc.perform(
        post("/" + RepoV2TestConfig.PATH + "/" + DinaRepositoryV2.JSON_API_BULK_LOAD_PATH)
          .contentType(DinaRepositoryV2.JSON_API_BULK)
          .content(objMapper.writeValueAsString(bulkLoadDocument.build())))
      .andExpect(status().isGone())
      .andReturn();

    Map<String, Object> loadedDocs = objMapper.readValue(response.getResponse().getContentAsString(),
      IT_OM_TYPE_REF);
    assertNotNull(loadedDocs.get("errors"));
  }

  @Test
  public void create_unsafePayload_ExceptionThrown() {
    PersonDTO personDto1 = PersonDTO.builder()
      .name("abc<iframe src=javascript:alert(32311)>")
      .build();
    JsonApiDocument doc1 = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(personDto1));
    assertThrows(IllegalArgumentException.class, () -> repositoryV2.create(doc1, null));
  }

  @TestConfiguration
  public static class RepoV2TestConfig {

    public static final String PATH = PersonDTO.TYPE_NAME + "v2";

    @Bean
    public JsonApiConfiguration jsonApiConfiguration() {
      return new JsonApiConfiguration()
        .withPluralizedTypeRendered(false)
        .withPageMetaAutomaticallyCreated(false)
        .withObjectMapperCustomizer(objectMapper -> {
          objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
          objectMapper.registerModule(new JavaTimeModule());
        });
    }

    @Service
    public static class DinaDepartmentService extends DefaultDinaService<Department> {

      public DinaDepartmentService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
        super(baseDAO, sv);
      }

      @Override
      protected void preCreate(Department entity) {
        entity.setUuid(UUID.randomUUID());
      }
    }

    @RestController
    @RequestMapping(produces = JSON_API_VALUE)
    static class PersonDinaTestRepositoryV2 extends DinaRepositoryV2<PersonDTO, Person> {
      public PersonDinaTestRepositoryV2(
        DinaService<Person> dinaService,
        BuildProperties buildProperties,
        Optional<AuditService> auditService,
        ObjectMapper objMapper
      ) {
        super(dinaService, new AllowAllAuthorizationService(),
          auditService, PersonMapper.INSTANCE, PersonDTO.class, Person.class,
          buildProperties, objMapper);
      }

      @Override
      protected Link generateLinkToResource(PersonDTO dto) {
        try {
          return linkTo(
            methodOn(PersonDinaTestRepositoryV2.class).onFindOne(dto.getUuid(), null)).withSelfRel();
        } catch (ResourceNotFoundException | ResourceGoneException e) {
          throw new RuntimeException(e);
        }
      }

      @GetMapping(PATH + "/{id}")
      public ResponseEntity<RepresentationModel<?>> onFindOne(@PathVariable UUID id,
                                                              HttpServletRequest req)
        throws ResourceNotFoundException, ResourceGoneException {
        return handleFindOne(id, req);
      }

      @GetMapping(PATH)
      public ResponseEntity<RepresentationModel<?>> onFindAll(HttpServletRequest req) {
        return handleFindAll(req);
      }

      @PostMapping(path = PATH, consumes = JSON_API_BULK)
      @Transactional
      public ResponseEntity<RepresentationModel<?>> onBulkCreate(
        @RequestBody JsonApiBulkDocument jsonApiBulkDocument) {
        return handleBulkCreate(jsonApiBulkDocument, null);
      }

      @PostMapping(path = PATH + "/" + JSON_API_BULK_LOAD_PATH, consumes = JSON_API_BULK)
      public ResponseEntity<RepresentationModel<?>> onBulkLoad(
        @RequestBody JsonApiBulkResourceIdentifierDocument jsonApiBulkDocument, HttpServletRequest req)
        throws ResourcesNotFoundException, ResourcesGoneException {
        return handleBulkLoad(jsonApiBulkDocument, req);
      }

      @PostMapping(path = PATH)
      @Transactional
      public ResponseEntity<RepresentationModel<?>> onCreate(
        @RequestBody JsonApiDocument postedDocument) {

        return handleCreate(postedDocument, null);
      }

      @PatchMapping(PATH + "/{id}")
      @Transactional
      public ResponseEntity<RepresentationModel<?>> onUpdate(
        @RequestBody JsonApiDocument partialPatchDto,
        @PathVariable UUID id) throws ResourceNotFoundException, ResourceGoneException {
        return handleUpdate(partialPatchDto, id);
      }

      @PatchMapping(path = PATH, produces = JSON_API_BULK)
      @Transactional
      public ResponseEntity<RepresentationModel<?>> onBulkUpdate(
        @RequestBody JsonApiBulkDocument jsonApiBulkDocument)
        throws ResourceNotFoundException, ResourceGoneException {
        return handleBulkUpdate(jsonApiBulkDocument);
      }

      @DeleteMapping(path = PATH, produces = JSON_API_BULK)
      @Transactional
      public ResponseEntity<RepresentationModel<?>> onBulkDelete(@RequestBody
                                                                 JsonApiBulkResourceIdentifierDocument jsonApiBulkDocument)
        throws ResourceNotFoundException, ResourceGoneException {
        return handleBulkDelete(jsonApiBulkDocument);
      }

      @DeleteMapping(PATH + "/{id}")
      @Transactional
      public ResponseEntity<RepresentationModel<?>> onDelete(@PathVariable UUID id)
        throws ResourceNotFoundException, ResourceGoneException {
        return handleDelete(id);
      }
    }
  }
}
