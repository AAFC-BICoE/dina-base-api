package ca.gc.aafc.dina.exceptionmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;

import ca.gc.aafc.dina.entity.Department;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.repository.ResourceRepository;

@Transactional
@SpringBootTest(classes = TestDinaBaseApp.class)
public class ConstraintViolationExceptionMapperIT {

  @Inject
  private ConstraintViolationExceptionMapper constraintViolationExceptionMapper;
  
  @Inject
  private ResourceRepository<DepartmentDto, Serializable> departmentRepository;
  
  @Test
  public void persistDepartment_whenNameIsTooLongAndLocationIsNull_mapperCreatesReadableErrorMessages() {
    // Make a String with over 50 characters (department name length limit is 50).
    String stringWith51Chars = "01234567890123456789012345678901234567890123456789a";
    assertEquals(51, stringWith51Chars.length());
    
    // Create the department
    DepartmentDto testDepartment = new DepartmentDto();
    testDepartment.setName(stringWith51Chars);
    testDepartment.setUuid(UUID.randomUUID());

    try {
      // Attempt the create.
      this.departmentRepository.create(testDepartment);
    } catch (ConstraintViolationException exception) {
      // This test expects the ConstraintViolationException to be thrown, it will fail otherwise.
      // Generate the error response here:
      ErrorResponse errorResponse = constraintViolationExceptionMapper.toErrorResponse(exception);
      
      // Assert correct http status.
      assertEquals(422, errorResponse.getHttpStatus());
      
      // Get the errors sorted by detail. The default error order is not consistent.
      List<ErrorData> errors = errorResponse.getErrors()
          .stream()
          .sorted((a, b) -> a.getDetail().compareTo(b.getDetail()))
          .collect(Collectors.toList());
      
      // 2 errors should be given.
      assertEquals(2, errors.size());
      
      // Assert correct error message, status and title (@NotNull location error)
      assertEquals("location cannot be null.", errors.get(0).getDetail());
      assertEquals("422", errors.get(0).getStatus());
      assertEquals("Constraint violation", errors.get(0).getTitle());
      assertEquals("location", errors.get(0).getSourcePointer());
      
      // Assert correct error message, status and title (@Size name length error)
      assertEquals("name size must be between 1 and 50", errors.get(1).getDetail());
      assertEquals("422", errors.get(1).getStatus());
      assertEquals("Constraint violation", errors.get(1).getTitle());
      assertEquals("name", errors.get(1).getSourcePointer());

      return;
    }
    
    // This test method should end at the "return" in the catch block.
    fail("ConstraintViolationException not thrown");
  }

  @Test
  public void persistDepartment_whenNestedStructure_errorDataIsAccurate() {
    // Create the department
    DepartmentDto testDepartment = new DepartmentDto();
    testDepartment.setName("Dep1");
    testDepartment.setLocation("Loc1");
    testDepartment.setUuid(UUID.randomUUID());
    testDepartment.setDepartmentDetails(new Department.DepartmentDetails("noteWithMoreThan10Chars"));

    try {
      // Attempt the create.
      this.departmentRepository.create(testDepartment);
    } catch (ConstraintViolationException exception) {
      // This test expects the ConstraintViolationException to be thrown, it will fail otherwise.
      // Generate the error response here:
      ErrorResponse errorResponse = constraintViolationExceptionMapper.toErrorResponse(exception);

      // Assert correct http status.
      assertEquals(422, errorResponse.getHttpStatus());

      // Get the errors sorted by detail. The default error order is not consistent.
      List<ErrorData> errors = errorResponse.getErrors()
          .stream()
          .sorted((a, b) -> a.getDetail().compareTo(b.getDetail()))
          .collect(Collectors.toList());

      assertEquals(1, errors.size());

      assertEquals("422", errors.get(0).getStatus());
      assertEquals("departmentDetails.note must be less than or equal to 10", errors.get(0).getDetail());
      assertEquals("departmentDetails/note", errors.get(0).getSourcePointer());

      return;
    }

    // This test method should end at the "return" in the catch block.
    fail("ConstraintViolationException not thrown");
  }
}
