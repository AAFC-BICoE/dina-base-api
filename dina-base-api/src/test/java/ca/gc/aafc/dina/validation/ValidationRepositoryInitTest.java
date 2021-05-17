package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.DinaUserConfig;
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.repository.validation.ValidationRepository;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import ca.gc.aafc.dina.service.DinaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.inject.Inject;
import java.util.Set;

@SpringBootTest(classes = TestDinaBaseApp.class,
  properties = {"dev-user.enabled: true", "keycloak.enabled: false", "dina.validationEndpoint.enabled: true"})
class ValidationRepositoryInitTest {

  private final static ObjectMapper mapper = new ObjectMapper();

  @Test
  void init_WithInvalidConfig() {
    Assertions.assertThrows(
      IllegalStateException.class,
      (() -> new ValidationRepository(new NullTypes(), mapper)));
    Assertions.assertThrows(
      IllegalStateException.class,
      (() -> new ValidationRepository(new BlankTypes(), mapper)));
    Assertions.assertThrows(
      IllegalStateException.class,
      (() -> new ValidationRepository(new MissingValidator(), mapper)));
    Assertions.assertThrows(
      IllegalStateException.class,
      (() -> new ValidationRepository(new MissingResourceClass(), mapper)));
    Assertions.assertThrows(
      IllegalStateException.class,
      (() -> new ValidationRepository(new MissingEntityClass(), mapper)));
  }

  static class NullTypes implements ValidationResourceConfiguration {
    @Inject
    DinaUserConfig.DepartmentDinaService departmentDinaService;

    @Override
    public DinaService<? extends DinaEntity> getServiceForType(String type) {
      return departmentDinaService;
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return EmployeeDto.class;
    }

    @Override
    public Class<? extends DinaEntity> getEntityClassForType(String type) {
      return Employee.class;
    }

    @Override
    public Set<String> getTypes() {
      return null;
    }
  }

  static class BlankTypes implements ValidationResourceConfiguration {

    @Inject
    DinaUserConfig.DepartmentDinaService departmentDinaService;

    @Override
    public DinaService<? extends DinaEntity> getServiceForType(String type) {
      return departmentDinaService;
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return EmployeeDto.class;
    }

    @Override
    public Class<? extends DinaEntity> getEntityClassForType(String type) {
      return Employee.class;
    }

    @Override
    public Set<String> getTypes() {
      return Set.of();
    }
  }

  static class MissingValidator implements ValidationResourceConfiguration {

    @Override
    public DinaService<? extends DinaEntity> getServiceForType(String type) {
      return null;
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return EmployeeDto.class;
    }

    @Override
    public Class<? extends DinaEntity> getEntityClassForType(String type) {
      return Employee.class;
    }

    @Override
    public Set<String> getTypes() {
      return Set.of("employee");
    }
  }

  static class MissingResourceClass implements ValidationResourceConfiguration {

    @Inject
    DinaUserConfig.DepartmentDinaService departmentDinaService;

    @Override
    public DinaService<? extends DinaEntity> getServiceForType(String type) {
      return departmentDinaService;
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return null;
    }

    @Override
    public Class<? extends DinaEntity> getEntityClassForType(String type) {
      return Employee.class;
    }

    @Override
    public Set<String> getTypes() {
      return Set.of("employee");
    }
  }

  static class MissingEntityClass implements ValidationResourceConfiguration {

    @Override
    public DinaService<? extends DinaEntity> getServiceForType(String type) {
      return null;
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return EmployeeDto.class;
    }

    @Override
    public Class<? extends DinaEntity> getEntityClassForType(String type) {
      return null;
    }

    @Override
    public Set<String> getTypes() {
      return Set.of("employee");
    }
  }

}

