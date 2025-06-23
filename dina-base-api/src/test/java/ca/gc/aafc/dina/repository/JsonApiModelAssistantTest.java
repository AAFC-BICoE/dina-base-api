package ca.gc.aafc.dina.repository;

import org.junit.jupiter.api.Test;

import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;

import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.PersonDTO;


import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonApiModelAssistantTest {

  @Test
  public void onCreateJsonApiModelBuilder_noException() {

    JsonApiModelAssistant<PersonDTO> assistant = new JsonApiModelAssistant<>("123");

    PersonDTO person = PersonDTO.builder()
      .name("abc defg")
      .room(18)
      .build();

    JsonApiDto.JsonApiDtoBuilder<PersonDTO> jsonApiDtoBuilder = JsonApiDto.builder();
    jsonApiDtoBuilder.dto(person);

    JsonApiModelBuilder builder =
      assistant.createJsonApiModelBuilder(jsonApiDtoBuilder.build());
    assertNotNull(builder.build());
  }
}
