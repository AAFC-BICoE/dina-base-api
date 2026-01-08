package ca.gc.aafc.dina.exceptionmapping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.toedter.spring.hateoas.jsonapi.JsonApiError;
import com.toedter.spring.hateoas.jsonapi.JsonApiErrors;

import ca.gc.aafc.dina.BasePostgresItContext;
import ca.gc.aafc.dina.config.PersonTestConfig;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocuments;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.repository.DinaRepositoryV2IT;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;

@SpringBootTest(classes = {DinaRepositoryV2IT.RepoV2TestConfig.class, PersonTestConfig.class})
@Transactional
public class ConstraintViolationExceptionMapperIT extends BasePostgresItContext {

  private final JsonApiExceptionControllerAdvice exceptionControllerAdvice = new JsonApiExceptionControllerAdvice();

  @Inject
  private ConstraintViolationExceptionMapper constraintViolationExceptionMapper;

  @Autowired
  private DinaRepositoryV2<PersonDTO, Person> personRepository;
  
  @Test
  public void persistPerson_whenNameIsTooLong_mapperCreatesReadableErrorMessages() {
    // Make a String with over 50 characters (department name length limit is 50).
    String stringWith51Chars = "01234567890123456789012345678901234567890123456789a";
    assertEquals(51, stringWith51Chars.length());
    
    // Create the department
    PersonDTO testPerson = new PersonDTO();
    testPerson.setName(stringWith51Chars);
    testPerson.setUuid(UUID.randomUUID());

    JsonApiDocument docToCreate = JsonApiDocuments.createJsonApiDocument(null, PersonDTO.TYPE_NAME,
      JsonAPITestHelper.toAttributeMap(testPerson));

    try {
      // Attempt the create.
      this.personRepository.create(docToCreate, null);
    } catch (ConstraintViolationException exception) {
      // This test expects the ConstraintViolationException to be thrown, it will fail otherwise.
      // Generate the error response here:
      JsonApiErrors apiErrors = exceptionControllerAdvice.handleConstraintViolationException(exception).getBody();
      // Assert correct http status.
      assertEquals("422", apiErrors.getErrors().getFirst().getCode());
      
      // Get the errors sorted by detail. The default error order is not consistent.
      List<JsonApiError> errors = apiErrors.getErrors()
          .stream()
          .sorted(Comparator.comparing(JsonApiError::getDetail))
          .toList();

      // Assert correct error message, status and title
      assertEquals("name size must be between 0 and 50", errors.getFirst().getDetail());
      assertEquals("422", errors.getFirst().getCode());
      assertEquals("Constraint violation", errors.getFirst().getTitle());
      return;
    }
    
    // This test method should end at the "return" in the catch block.
    fail("ConstraintViolationException not thrown");
  }

//  @Test
//  public void persistDepartment_whenNestedObject_errorDataIsAccurate() {
//    // Create the department
//    DepartmentDto testDepartment = new DepartmentDto();
//    testDepartment.setName("Dep1");
//    testDepartment.setLocation("Loc1");
//    testDepartment.setUuid(UUID.randomUUID());
//    testDepartment.setDepartmentDetails(new Department.DepartmentDetails("noteWithMoreThan10Chars"));
//
//    try {
//      // Attempt the create.
//      this.departmentRepository.create(testDepartment);
//    } catch (ConstraintViolationException exception) {
//      // This test expects the ConstraintViolationException to be thrown, it will fail otherwise.
//      // Generate the error response here:
//      ErrorResponse errorResponse = constraintViolationExceptionMapper.toErrorResponse(exception);
//
//      // Assert correct http status.
//      assertEquals(422, errorResponse.getHttpStatus());
//
//      // Get the errors sorted by detail. The default error order is not consistent.
//      List<ErrorData> errors = errorResponse.getErrors()
//          .stream()
//          .sorted((a, b) -> a.getDetail().compareTo(b.getDetail()))
//          .toList();
//
//      assertEquals(1, errors.size());
//
//      assertEquals("422", errors.get(0).getStatus());
//      assertEquals("departmentDetails.note must be less than or equal to 10", errors.get(0).getDetail());
//      assertEquals("departmentDetails.note", errors.get(0).getSourcePointer());
//
//      return;
//    }
//
//    // This test method should end at the "return" in the catch block.
//    fail("ConstraintViolationException not thrown");
//  }
//
//  @Test
//  public void persistDepartment_whenNestedObjectArray_errorDataIsAccurate() {
//    // Create the department
//    DepartmentDto testDepartment = new DepartmentDto();
//    testDepartment.setName("Dep1");
//    testDepartment.setLocation("Loc1");
//    testDepartment.setUuid(UUID.randomUUID());
//    testDepartment.setAliases(
//      // Try to set a blank alias, which is not allowed:
//      List.of(new Department.DepartmentAlias(""))
//    );
//
//    try {
//      // Attempt the create.
//      this.departmentRepository.create(testDepartment);
//    } catch (ConstraintViolationException exception) {
//      // This test expects the ConstraintViolationException to be thrown, it will fail otherwise.
//      // Generate the error response here:
//      ErrorResponse errorResponse = constraintViolationExceptionMapper.toErrorResponse(exception);
//
//      // Assert correct http status.
//      assertEquals(422, errorResponse.getHttpStatus());
//
//      // Get the errors sorted by detail. The default error order is not consistent.
//      ErrorData[] errors = errorResponse.getErrors().toArray(ErrorData[]::new);
//      assertEquals(1, errors.length);
//
//      assertEquals("422", errors[0].getStatus());
//      assertEquals("aliases[0].name must not be blank", errors[0].getDetail());
//      assertEquals("aliases[0].name", errors[0].getSourcePointer());
//
//      return;
//    }
//
//    // This test method should end at the "return" in the catch block.
//    fail("ConstraintViolationException not thrown");
//  }
}
