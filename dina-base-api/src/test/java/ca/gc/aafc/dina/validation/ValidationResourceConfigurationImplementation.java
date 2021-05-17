package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.DinaUserConfig;
import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.dto.ChainTemplateDto;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Chain;
import ca.gc.aafc.dina.entity.ChainTemplate;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import ca.gc.aafc.dina.service.DinaService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

@Component
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

  @Inject
  DinaUserConfig.DepartmentDinaService departmentDinaService;

  private static final Map<String, Class<? extends DinaEntity>> typeToEntityClassMap = Map.ofEntries(
    entry("department", Department.class),
    entry("employee", Employee.class),
    entry("chain", Chain.class),
    entry("chainTemplate", ChainTemplate.class)
  );

  private static final Map<String, Class<?>> typeToResourceClassMap = Map.ofEntries(
    entry("department", DepartmentDto.class),
    entry("employee", EmployeeDto.class),
    entry("chain", ChainDto.class),
    entry("chainTemplate", ChainTemplateDto.class)
  );

  @Override
  public Set<String> getTypes() {
    return typeToEntityClassMap.keySet();
  }

  @Override
  public Class<? extends DinaEntity> getEntityClassForType(String type) {
    return typeToEntityClassMap.get(type);
  }

  @Override
  public DinaService<? extends DinaEntity> getServiceForType(String type) {
    return departmentDinaService;
  }

  @Override
  public Class<?> getResourceClassForType(String type) {
    return typeToResourceClassMap.get(type);
  }

}
