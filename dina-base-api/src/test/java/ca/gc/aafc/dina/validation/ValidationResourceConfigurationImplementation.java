package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.dto.ChainTemplateDto;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Chain;
import ca.gc.aafc.dina.entity.ChainTemplate;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Set;

@Component
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

  private final HashMap<Class<?>, DinaRepository<?, ? extends DinaEntity>> dinaServiceHashMap = new HashMap<>();

  public ValidationResourceConfigurationImplementation(
    @NonNull DinaRepository<DepartmentDto, Department> departmentRepo,
    @NonNull DinaRepository<EmployeeDto, Employee> employeeRepo,
    @NonNull DinaRepository<ChainDto, Chain> chainDinaRepository,
    @NonNull DinaRepository<ChainTemplateDto, ChainTemplate> templateDinaRepository
    ) {
    dinaServiceHashMap.put(DepartmentDto.class, departmentRepo);
    dinaServiceHashMap.put(EmployeeDto.class, employeeRepo);
    dinaServiceHashMap.put(ChainDto.class, chainDinaRepository);
    dinaServiceHashMap.put(ChainTemplateDto.class, templateDinaRepository);
  }

  @Override
  public DinaRepository<?, ? extends DinaEntity> getRepoForType(Class<?> type) {
    return dinaServiceHashMap.get(type);
  }

  @Override
  public Set<Class<?>> getTypes() {
    return dinaServiceHashMap.keySet();
  }
}
