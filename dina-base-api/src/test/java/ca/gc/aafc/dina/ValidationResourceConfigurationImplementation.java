package ca.gc.aafc.dina;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;

import org.springframework.boot.test.context.TestConfiguration;

import ca.gc.aafc.dina.DinaUserConfig.DepartmentDinaService;
import ca.gc.aafc.dina.DinaUserConfig.EmployeeDinaService;
import ca.gc.aafc.dina.DinaUserConfig.VocabularyDinaService;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import ca.gc.aafc.dina.service.DefaultDinaService;

@TestConfiguration
public class ValidationResourceConfigurationImplementation implements ValidationResourceConfiguration {

  @Inject
  private static DepartmentDinaService departmentDinaService;

  @Inject
  private static EmployeeDinaService employeeDinaService;

  @Inject
  private static VocabularyDinaService vocabularyDinaService;

  public static final Map<String, DefaultDinaService<? extends DinaEntity>> typeToReferenceMap = ImmutableMap.of(
    "department", departmentDinaService,
    "employee", employeeDinaService,
    "vocabulary", vocabularyDinaService
  );

  @Override
  public DefaultDinaService<? extends DinaEntity> getServiceForType(String type) {
    return typeToReferenceMap.get(type);
  }

  @Override
  public Set<String> getTypes() {
    return typeToReferenceMap.keySet();
  }
  
}
