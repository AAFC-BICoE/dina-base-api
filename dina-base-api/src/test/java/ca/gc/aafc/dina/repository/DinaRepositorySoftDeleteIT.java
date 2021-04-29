package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Person;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.QuerySpec;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.UUID;

@SpringBootTest(classes = TestDinaBaseApp.class, properties = "dina.auditing.enabled = true")
public class DinaRepositorySoftDeleteIT {

  @Inject
  private DinaRepository<PersonDTO, Person> repository;

  @Test
  void findOne_DeletedResourcePresentInAuditLogs_ThrowsGoneException() {
    UUID id = repository.create(createPersonDto()).getUuid();
    repository.delete(id);
    Assertions.assertThrows(GoneException.class, () -> repository.findOne(id, newQuerySpec()));
  }

  @Test
  void findOne_DeletedResourceNOTPresentInAuditLogs_ThrowsResourceNotFoundException() {
    Assertions.assertThrows(
      ResourceNotFoundException.class,
      () -> repository.findOne(UUID.randomUUID(), newQuerySpec()));
  }

  private static PersonDTO createPersonDto() {
    return PersonDTO.builder()
      .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
      .name(RandomStringUtils.random(4)).build();
  }

  private static QuerySpec newQuerySpec() {
    return new QuerySpec(PersonDTO.class);
  }

}
