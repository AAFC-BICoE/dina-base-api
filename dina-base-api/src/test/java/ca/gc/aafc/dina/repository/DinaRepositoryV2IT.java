package ca.gc.aafc.dina.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.mapper.PersonMapper;
import ca.gc.aafc.dina.security.auth.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import javax.transaction.Transactional;

@Transactional
@SpringBootTest(classes = {TestDinaBaseApp.class, DinaRepositoryV2IT.RepoV2TestConfig.class})
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class DinaRepositoryV2IT {

  @Inject
  private DinaRepositoryV2<PersonDTO, Person> repositoryV2;

  @Inject
  private DinaRepositoryIT.DinaPersonService personService;

  @Test
  public void onGetAll_noError() {
    repositoryV2.getAll("");
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
  public void onCreateJsonApiModelBuilder_noException() {

    personService.create(Person.builder()
      .name("abc defg")
      .room(18)
      .build());

    QueryComponent qc = QueryComponent.builder()
      .pageLimit(1)
      .build();

    JsonApiModelBuilder builder =
      repositoryV2.createJsonApiModelBuilder(repositoryV2.getAll(qc));
    assertNotNull(builder.build());
  }

  @Test
  public void onCreateUpdateDelete_noException() throws ResourceNotFoundException {

    PersonDTO personDto = PersonDTO.builder()
      .name("Bob")
      .build();

    JsonApiDocument doc = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(personDto));
    UUID assignedId = repositoryV2.create(doc, null);

    Map<String, Object> attributes = new HashMap<>();

    attributes.put("name", "abc");
    attributes.put("room", 21);
    // convert to string to mimic how we would get it with json:api
    attributes.put("createdOn", OffsetDateTime.now().toString());

    JsonApiDocument document = JsonApiDocument.builder()
      .data(JsonApiDocument.ResourceObject.builder()
        .id(assignedId)
        .attributes(attributes)
        .build())
      .build();
    repositoryV2.update(document);

    JsonApiDto<PersonDTO> getOneDto = repositoryV2.getOne(assignedId, null);
    assertEquals("abc", getOneDto.getDto().getName());

    repositoryV2.delete(assignedId);
  }

  @TestConfiguration
  static class RepoV2TestConfig {

    @Bean
    public DinaRepositoryV2<PersonDTO, Person> personRepositoryV2(DinaService<Person> dinaService,
                                                                  BuildProperties buildProperties,
                                                                  ObjectMapper objMapper) {
      return new DinaRepositoryV2<>(dinaService, new AllowAllAuthorizationService(),
        Optional.empty(), PersonMapper.INSTANCE, PersonDTO.class, Person.class,
        buildProperties, objMapper);
    }

  }

}
