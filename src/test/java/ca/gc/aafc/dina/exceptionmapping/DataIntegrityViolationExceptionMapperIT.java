package ca.gc.aafc.dina.exceptionmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.EmployeeDto;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.error.ErrorResponse;
import io.crnk.core.repository.ResourceRepository;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class DataIntegrityViolationExceptionMapperIT {
  
  @Inject
  private DataIntegrityViolationExceptionMapper exceptionMapper;
  
  @Inject
  private ResourceRepository<EmployeeDto, Serializable> employeeRepository;
  
  @Test
  public void createEmployee_whenUniqueConstraintIsViolated_mapperCreatesReadableErrorMessages() {
    // Create 2 Employees violating the unique constraint (name)
    EmployeeDto emp1 = new EmployeeDto();
    emp1.setName("Mat");
    
    EmployeeDto emp2 = new EmployeeDto();
    emp2.setName("Mat");
    
    this.employeeRepository.create(emp1);
    try {
      // Attempt creating a second Employee with the same Name.
      this.employeeRepository.create(emp2);
    } catch(DataIntegrityViolationException exception) {
      ErrorResponse errorResponse = this.exceptionMapper.toErrorResponse(exception);
      
      // Assert correct http status.
      assertEquals(422, errorResponse.getHttpStatus());
      
      List<ErrorData> errors = errorResponse.getErrors().stream().collect(Collectors.toList());
      
      // 1 error should be given.
      assertEquals(1, errors.size());
      
      // Assert correct error message, status and title (Data integrity violation)
      assertEquals("could not execute statement; SQL", errors.get(0).getDetail().substring(0, 32));
      assertEquals("422", errors.get(0).getStatus());
      assertEquals("Data integrity violation", errors.get(0).getTitle());
      
      return;
    }
    
    // This test method should end at the "return" in the catch block.
    fail("DataIntegrityViolationException not thrown");
  }
  
}
