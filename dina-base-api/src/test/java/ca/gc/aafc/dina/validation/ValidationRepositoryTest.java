package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.repository.validation.ValidationRepository;
import ca.gc.aafc.dina.repository.validation.ValidationResourceConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import javax.validation.Validation;
import java.util.Set;

class ValidationConfigurationInitTest {

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

    @Override
    public Validator getValidatorForType(String type) {
      return new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return EmployeeDto.class;
    }

    @Override
    public Class<?> getEntityClassForType(String type) {
      return Employee.class;
    }

    @Override
    public Set<String> getTypes() {
      return null;
    }
  }

  static class BlankTypes implements ValidationResourceConfiguration {

    @Override
    public Validator getValidatorForType(String type) {
      return new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return EmployeeDto.class;
    }

    @Override
    public Class<?> getEntityClassForType(String type) {
      return Employee.class;
    }

    @Override
    public Set<String> getTypes() {
      return Set.of();
    }
  }

  static class MissingValidator implements ValidationResourceConfiguration {

    @Override
    public Validator getValidatorForType(String type) {
      return null;
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return EmployeeDto.class;
    }

    @Override
    public Class<?> getEntityClassForType(String type) {
      return Employee.class;
    }

    @Override
    public Set<String> getTypes() {
      return Set.of("employee");
    }
  }

  static class MissingResourceClass implements ValidationResourceConfiguration {

    @Override
    public Validator getValidatorForType(String type) {
      return new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return null;
    }

    @Override
    public Class<?> getEntityClassForType(String type) {
      return Employee.class;
    }

    @Override
    public Set<String> getTypes() {
      return Set.of("employee");
    }
  }

  static class MissingEntityClass implements ValidationResourceConfiguration {

    @Override
    public Validator getValidatorForType(String type) {
      return new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Override
    public Class<?> getResourceClassForType(String type) {
      return EmployeeDto.class;
    }

    @Override
    public Class<?> getEntityClassForType(String type) {
      return null;
    }

    @Override
    public Set<String> getTypes() {
      return Set.of("employee");
    }
  }

}

