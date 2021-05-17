package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.DinaUserConfig;
import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.dto.ChainTemplateDto;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import ca.gc.aafc.dina.service.DinaService;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Set;

@Component
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

  private final HashMap<Class<?>, DinaService<? extends DinaEntity>> dinaServiceHashMap = new HashMap<>();

  public ValidationResourceConfigurationImplementation(@NonNull DinaUserConfig.DepartmentDinaService departmentDinaService) {
    dinaServiceHashMap.put(DepartmentDto.class, departmentDinaService);
    dinaServiceHashMap.put(EmployeeDto.class, departmentDinaService);
    dinaServiceHashMap.put(ChainDto.class, departmentDinaService);
    dinaServiceHashMap.put(ChainTemplateDto.class, departmentDinaService);
  }

  @Override
  public DinaService<? extends DinaEntity> getServiceForType(Class<?> type) {
    return dinaServiceHashMap.get(type);
  }

  @Override
  public Set<Class<?>> getTypes() {
    return dinaServiceHashMap.keySet();
  }
}
