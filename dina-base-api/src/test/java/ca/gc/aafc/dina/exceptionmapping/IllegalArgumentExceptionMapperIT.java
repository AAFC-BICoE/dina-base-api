package ca.gc.aafc.dina.exceptionmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.repository.DinaRepository;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.queryspec.SortSpec;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
public class IllegalArgumentExceptionMapperIT {

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Inject
  private IllegalArgumentExceptionMapper illegalArgumentExceptionMapper;
  
  @Test
  public void findAll_FilterWithRSQL_FiltersOnNonExistantRSQLfield_mapperCreatesReadableErrorMessages() {
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(PathSpec.of("rsql").filter(FilterOperator.EQ, "nonExistant==" + "anyValue"));

    try {
      dinaRepository.findAll(null, querySpec);
    } catch (IllegalArgumentException exception) {

      ErrorResponse errorResponse = illegalArgumentExceptionMapper.toErrorResponse(exception);

      // Assert correct http status.
      assertEquals(400, errorResponse.getHttpStatus());
            
      // Get the errors sorted by detail. The default error order is not consistent.
      List<ErrorData> errors = errorResponse.getErrors()
      .stream()
      .sorted((a, b) -> a.getDetail().compareTo(b.getDetail()))
      .collect(Collectors.toList());

      assertEquals(1, errors.size());

      // Assert correct error message, status and title (@NotNull location error)
      assertEquals("Unknown property: nonExistant from entity ca.gc.aafc.dina.entity.Person", errors.get(0).getDetail());
      assertEquals("400", errors.get(0).getStatus());
      assertEquals("BAD_REQUEST", errors.get(0).getTitle());
    }
  }

  @Test
  public void findAll_sortWithNonExistantField_mapperCreatesReadableErrorMessages() {
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setSort(Collections.singletonList(
      new SortSpec(Collections.singletonList("nonExistant"), Direction.ASC)));
    try {
      dinaRepository.findAll(null, querySpec);
    } catch (IllegalArgumentException exception) {

      ErrorResponse errorResponse = illegalArgumentExceptionMapper.toErrorResponse(exception);

      // Assert correct http status.
      assertEquals(400, errorResponse.getHttpStatus());
            
      // Get the errors sorted by detail. The default error order is not consistent.
      List<ErrorData> errors = errorResponse.getErrors()
      .stream()
      .sorted((a, b) -> a.getDetail().compareTo(b.getDetail()))
      .collect(Collectors.toList());

      assertEquals(1, errors.size());

      // Assert correct error message, status and title (@NotNull location error)
      assertEquals("Unable to locate Attribute  with the the given name [nonExistant] on this ManagedType [ca.gc.aafc.dina.entity.Person]", errors.get(0).getDetail());
      assertEquals("400", errors.get(0).getStatus());
      assertEquals("BAD_REQUEST", errors.get(0).getTitle());
    }
  }
}
