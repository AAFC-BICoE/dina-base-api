package ca.gc.aafc.dina.validation;

import java.util.Map;
import static java.util.Map.entry;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.DinaUserConfig;
import ca.gc.aafc.dina.DinaUserConfig.DepartmentDinaService;
import ca.gc.aafc.dina.DinaUserConfig.EmployeeDinaService;
import ca.gc.aafc.dina.DinaUserConfig.VocabularyDinaService;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import ca.gc.aafc.dina.service.DefaultDinaService;

@Component
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

  @Inject
  private DepartmentDinaService departmentDinaService;

  @Inject
  private EmployeeDinaService employeeDinaService;

  private static final Map<String, Class> typeToEntityClassMap = Map.ofEntries(
    entry("department", Department.class),
    entry("employee", Employee.class)
  );

  private static final Map<String, Class> typeToResourceClassMap = Map.ofEntries(
    entry("department", DepartmentDto.class),
    entry("employee", EmployeeDto.class)
  );  
  @Override
  public DefaultDinaService getServiceForType(String type) {
    Map<String, DefaultDinaService<? extends DinaEntity>> typeToServiceMap = Map.ofEntries(
      entry("department", departmentDinaService),
      entry("employee", employeeDinaService)
    );
    return typeToServiceMap.get(type);
  }

  @Override
  public Set<String> getTypes() {
    return typeToEntityClassMap.keySet();
  }

  @Override
  public Class getEntityClassForType(String type) {
    return typeToEntityClassMap.get(type);
  }

  @Override
  public Class getResourceClassForType(String type) {
    return typeToResourceClassMap.get(type);
  }
  
}
