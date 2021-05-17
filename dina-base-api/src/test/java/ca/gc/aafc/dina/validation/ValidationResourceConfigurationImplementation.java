package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.DinaUserConfig;
import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.dto.ChainTemplateDto;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import ca.gc.aafc.dina.service.DinaService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Set;

@Component
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

  @Inject
  DinaUserConfig.DepartmentDinaService departmentDinaService;

  @Override
  public DinaService<? extends DinaEntity> getServiceForType(String type) {
    return departmentDinaService;
  }

  @Override
  public Set<Class<?>> getTypes() {
    return Set.of(
      DepartmentDto.class,
      EmployeeDto.class,
      ChainDto.class,
      ChainTemplateDto.class);
  }
}
