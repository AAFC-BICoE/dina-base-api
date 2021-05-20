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

/** When you need an Authenticated user bean */
@Configuration
public class DinaUserConfig {

  public static final String AUTH_USER_NAME = "username";

  @Bean
  public DinaAuthenticatedUser user() {
    return DinaAuthenticatedUser.builder().username(DinaUserConfig.AUTH_USER_NAME).build();
  }

  @Service
  class DepartmentDinaService extends DefaultDinaService<Department> {

    public DepartmentDinaService(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }

    @Override
    protected void preCreate(Department entity) {
      entity.setUuid(UUID.randomUUID());
    }
  }

  @Service
  class EmployeeDinaService extends DefaultDinaService<Employee> {

    public EmployeeDinaService(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }
  }

  @Service

  class VocabularyDinaService extends DefaultDinaService<Vocabulary> {

    public VocabularyDinaService(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
    }
  }
}
