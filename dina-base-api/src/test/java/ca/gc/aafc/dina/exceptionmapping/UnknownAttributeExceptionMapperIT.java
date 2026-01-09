package ca.gc.aafc.dina.exceptionmapping;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import com.toedter.spring.hateoas.jsonapi.JsonApiError;
import com.toedter.spring.hateoas.jsonapi.JsonApiErrors;

import ca.gc.aafc.dina.BasePostgresItContext;
import ca.gc.aafc.dina.config.PersonTestConfig;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.exception.UnknownAttributeException;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import javax.transaction.Transactional;

@Transactional
@Import(PersonTestConfig.class)
public class UnknownAttributeExceptionMapperIT extends BasePostgresItContext {

  private final JsonApiExceptionControllerAdvice exceptionControllerAdvice = new JsonApiExceptionControllerAdvice();

  @Inject
  private DinaRepositoryV2<PersonDTO, Person> dinaRepository;

  @Test
  public void findAll_FilterWithFIQL_FiltersOnNonExistantfield_ErrorMessages() {
    try {
      dinaRepository.getAll("fiql=nonExistant==anyValue");
    } catch (UnknownAttributeException exception) {

      JsonApiErrors
        apiErrors = exceptionControllerAdvice.handleUnknownAttributeException(exception).getBody();
      // Assert correct http status.
      assertEquals("400", apiErrors.getErrors().getFirst().getCode());

      // Get the errors sorted by detail. The default error order is not consistent.
      List<JsonApiError> errors = apiErrors.getErrors()
        .stream()
        .sorted(Comparator.comparing(JsonApiError::getDetail))
        .toList();

      assertEquals(1, errors.size());
      // Assert correct error message, status and title
      assertEquals("nonExistant : unknown attribute", errors.getFirst().getDetail());
      assertEquals("400", errors.getFirst().getCode());
      assertEquals("Bad Request", errors.getFirst().getTitle());
    }
  }

  @Test
  public void findAll_sortWithNonExistantField_mapperCreatesReadableErrorMessages() {
    try {
      dinaRepository.getAll(QueryComponent.builder().sorts(List.of("nonExistant")).build());
    } catch (UnknownAttributeException exception) {
      JsonApiErrors
        apiErrors = exceptionControllerAdvice.handleUnknownAttributeException(exception).getBody();

      // Assert correct http status.
      assertEquals("400", apiErrors.getErrors().getFirst().getCode());

      // Get the errors sorted by detail. The default error order is not consistent.
      List<JsonApiError> errors = apiErrors.getErrors()
        .stream()
        .sorted(Comparator.comparing(JsonApiError::getDetail))
        .toList();

      assertEquals(1, errors.size());

      // Assert correct error message, status and title
      assertEquals("Unable to locate Attribute with the the given name [nonExistant] on this ManagedType [ca.gc.aafc.dina.entity.Person]", errors.getFirst().getDetail());
      assertEquals("400", errors.getFirst().getCode());
      assertEquals("Bad Request", errors.getFirst().getTitle());
    }
  }
}
