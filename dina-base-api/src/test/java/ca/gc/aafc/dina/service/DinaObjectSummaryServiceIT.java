package ca.gc.aafc.dina.service;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.BasePostgresItContext;
import ca.gc.aafc.dina.jpa.DinaObjectSummary;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.repository.DinaRepositoryIT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

@Transactional
public class DinaObjectSummaryServiceIT extends BasePostgresItContext {

  @Inject
  private DinaRepositoryIT.DinaPersonService personService;

  @Inject
  private DinaObjectSummaryService summaryService;

  @Test
  public void findDinaObjectSummaryByUUID_onUniqueEntry_dinaObjectSummaryLoadedByUUID() {
    Person person1 = personService.create(Person.builder().group("abc").build());
    personService.create(Person.builder().group("bcd").build());
    personService.flush();

    DinaObjectSummary
      as = summaryService.findDinaObjectSummaryByUUID(Person.class, person1.getUuid());
    assertEquals(person1.getGroup(), as.getGroup());
  }

  @Test
  public void findDinaObjectSummaryByUUID_onNotFound_nullReturned() {
    assertNull(summaryService.findDinaObjectSummaryByUUID(Person.class, UUID.randomUUID()));
  }

}
