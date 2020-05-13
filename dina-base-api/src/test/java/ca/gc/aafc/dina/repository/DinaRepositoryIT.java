package ca.gc.aafc.dina.repository;

import static org.junit.jupiter.api.Assertions.fail;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jpa.DinaService;
import lombok.NonNull;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class DinaRepositoryIT {

  @Inject
  private DinaRepository<PersonDTO, Person> dinaRepository;

  @Test
  public void create_ValidResource_ResourceCreated() {
    dinaRepository.create(null);
    fail();
  }

  public static class DinaPersonService extends DinaService<Person> {

    public DinaPersonService(@NonNull BaseDAO baseDAO) {
      super(baseDAO);
      // TODO Auto-generated constructor stub
    }

    @Override
    protected Person preCreate(Person entity) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    protected Person preUpdate(Person entity) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    protected void preDelete(Person entity) {
      // TODO Auto-generated method stub

    }

  }
}
