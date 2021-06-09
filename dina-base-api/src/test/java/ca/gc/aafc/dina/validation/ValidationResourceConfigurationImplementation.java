package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.dto.ChainTemplateDto;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Chain;
import ca.gc.aafc.dina.entity.ChainTemplate;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import ca.gc.aafc.dina.repository.validation.ValidationResourceHandler;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

  private final List<ValidationResourceHandler<?>> list;

  public ValidationResourceConfigurationImplementation(
    @NonNull DinaRepository<DepartmentDto, Department> departmentRepo,
    @NonNull DinaRepository<EmployeeDto, Employee> employeeRepo,
    @NonNull DinaRepository<ChainDto, Chain> chainDinaRepository,
    @NonNull DinaRepository<ChainTemplateDto, ChainTemplate> templateDinaRepository
  ) {
    list = List.of(
      new ValidationResourceHandler<>(DepartmentDto.class, departmentRepo),
      new ValidationResourceHandler<>(EmployeeDto.class, employeeRepo),
      new ValidationResourceHandler<>(ChainDto.class, chainDinaRepository),
      new ValidationResourceHandler<>(ChainTemplateDto.class, templateDinaRepository));
  }

  @Override
  public List<ValidationResourceHandler<?>> getValidationHandlers() {
    return list;
  }
}
