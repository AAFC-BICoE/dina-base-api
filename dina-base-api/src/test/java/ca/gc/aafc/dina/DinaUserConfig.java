package ca.gc.aafc.dina;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.entity.Vocabulary;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import ca.gc.aafc.dina.service.DefaultDinaService;
import lombok.NonNull;
import org.springframework.validation.SmartValidator;

/** When you need an Authenticated user bean */
@Configuration
public class DinaUserConfig {

  public static final String AUTH_USER_NAME = "username";

  @Bean
  public DinaAuthenticatedUser user() {
    return DinaAuthenticatedUser.builder().username(DinaUserConfig.AUTH_USER_NAME).build();
  }

  @Service
  public class DepartmentDinaService extends DefaultDinaService<Department> {

    public DepartmentDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

    @Override
    protected void preCreate(Department entity) {
      entity.setUuid(UUID.randomUUID());
    }

    @Override
    public void validateBusinessRules(Department entity) {
      validateConstraints(entity,null);
    }
  }

  @Service
  public class EmployeeDinaService extends DefaultDinaService<Employee> {
    public EmployeeDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

    @Override
    protected void preCreate(Employee entity) {
      entity.setUuid(UUID.randomUUID());
    }

    @Override
    public void validateBusinessRules(Employee entity) {
      validateConstraints(entity,null);
    }
  }

  @Service
  public class VocabularyDinaService extends DefaultDinaService<Vocabulary> {

    public VocabularyDinaService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

  }
}
