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
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import javax.validation.Validation;
import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

@Component
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

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
  public Class<?> getResourceClassForType(String type) {
    return typeToResourceClassMap.get(type);
  }

  // If there is no custom validator, return default Validator
  @Override
  public Validator getValidatorForType(String type) {
    return new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());
  }

}
