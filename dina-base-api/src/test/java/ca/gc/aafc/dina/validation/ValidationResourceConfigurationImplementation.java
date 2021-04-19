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
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import ca.gc.aafc.dina.service.DefaultDinaService;

@Component
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

  @Inject
  private DepartmentDinaService departmentDinaService;

  // @Inject
  // private EmployeeDinaService employeeDinaService;

  // @Inject
  // private VocabularyDinaService vocabularyDinaService;

  // public Map<String, DefaultDinaService<? extends DinaEntity>> typeToReferenceMap = Map.ofEntries(
  //   entry("department", departmentDinaService),
  //   entry("employee", employeeDinaService),
  //   entry("vocabulary", vocabularyDinaService)
  // );

  @Override
  public DefaultDinaService getServiceForType(String type) {
    return departmentDinaService;
   //return typeToReferenceMap.get(type);
  }

  @Override
  public Set<String> getTypes() {
    return null;
    //return typeToReferenceMap.keySet();
  }

  @Override
  public Class getEntityClassForType(String type) {
    return Department.class;
  }

  @Override
  public Class getResourceClassForType(String type) {
    return DepartmentDto.class;
  }
  
}
