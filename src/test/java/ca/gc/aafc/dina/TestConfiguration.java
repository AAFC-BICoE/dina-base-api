package ca.gc.aafc.dina;

import java.io.Serializable;
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
import ca.gc.aafc.dina.jpa.meta.JpaTotalMetaInformationProvider;
import ca.gc.aafc.dina.jpa.repository.JpaDtoRepository;
import ca.gc.aafc.dina.jpa.repository.JpaResourceRepository;
import io.crnk.core.repository.ResourceRepository;

/**
 * Small test application running on dina-base-api
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Department.class)
public class TestConfiguration {

  @Inject
  private JpaTotalMetaInformationProvider metaInformationProvider;

  @Bean
  public JpaDtoMapper jpaDtoMapper() {
    Map<Class<?>, Class<?>> jpaEntities = new HashMap<>();

    jpaEntities.put(DepartmentDto.class, Department.class);
    jpaEntities.put(EmployeeDto.class, Employee.class);

    return new JpaDtoMapper(jpaEntities);
  }

  @Bean
  public ResourceRepository<DepartmentDto, Serializable> departmentRepository(JpaDtoRepository dtoRepository) {
    return new JpaResourceRepository<DepartmentDto>(
      DepartmentDto.class,
      dtoRepository,
      Arrays.asList(),
      metaInformationProvider
    );
  }

  @Bean
  public ResourceRepository<EmployeeDto, Serializable> employeeRepository(JpaDtoRepository dtoRepository) {
    return new JpaResourceRepository<EmployeeDto>(
      EmployeeDto.class,
      dtoRepository,
      Arrays.asList(),
      metaInformationProvider
    );
  }

}
