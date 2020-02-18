package ca.gc.aafc.dina;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entities.Department;
import ca.gc.aafc.dina.entities.Employee;
import ca.gc.aafc.dina.jpa.JpaDtoMapper;
import ca.gc.aafc.dina.jpa.filter.RsqlFilterHandler;
import ca.gc.aafc.dina.jpa.filter.SimpleFilterHandler;
import ca.gc.aafc.dina.jpa.meta.JpaTotalMetaInformationProvider;
import ca.gc.aafc.dina.jpa.repository.JpaDtoRepository;
import ca.gc.aafc.dina.jpa.repository.JpaRelationshipRepository;
import ca.gc.aafc.dina.jpa.repository.JpaResourceRepository;
import ca.gc.aafc.dina.jpa.repository.SelectionHandler;
import lombok.NonNull;

/**
 * Small test application running on dina-base-api
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Department.class)
public class TestConfiguration {

  @Inject
  private JpaTotalMetaInformationProvider metaInformationProvider;

  @Inject
  private SimpleFilterHandler simpleFilterHandler;

  @Inject
  private RsqlFilterHandler rsqlFilterHandler;

  @Bean
  public JpaDtoMapper jpaDtoMapper(@NonNull SelectionHandler selectionHandler) {
    Map<Class<?>, Class<?>> jpaEntities = new HashMap<>();

    jpaEntities.put(DepartmentDto.class, Department.class);
    jpaEntities.put(EmployeeDto.class, Employee.class);

    return new JpaDtoMapper(jpaEntities, selectionHandler);
  }

  @Bean
  public JpaResourceRepository<DepartmentDto> departmentRepository(JpaDtoRepository dtoRepository) {
    return new JpaResourceRepository<DepartmentDto>(
      DepartmentDto.class,
      dtoRepository,
      Arrays.asList(simpleFilterHandler, rsqlFilterHandler),
      metaInformationProvider
    );
  }

  @Bean
  public JpaResourceRepository<EmployeeDto> employeeRepository(JpaDtoRepository dtoRepository) {
    return new JpaResourceRepository<EmployeeDto>(
      EmployeeDto.class,
      dtoRepository,
      Arrays.asList(simpleFilterHandler, rsqlFilterHandler),
      metaInformationProvider
    );
  }

  @Bean
  public JpaRelationshipRepository<DepartmentDto, EmployeeDto> departmentToEmployeeRepository(JpaDtoRepository dtoRepository) {
    return new JpaRelationshipRepository<>(
      DepartmentDto.class,
      EmployeeDto.class,
      dtoRepository,
      Arrays.asList(
        simpleFilterHandler, 
        rsqlFilterHandler
      ),
      metaInformationProvider
    );
  }

  @Bean
  public JpaRelationshipRepository<EmployeeDto, DepartmentDto> employeeToDepartmentRepository(JpaDtoRepository dtoRepository) {
    return new JpaRelationshipRepository<>(
      EmployeeDto.class,
      DepartmentDto.class,
      dtoRepository,
      Arrays.asList(
        simpleFilterHandler, 
        rsqlFilterHandler
      ),
      metaInformationProvider
    );
  }

}
