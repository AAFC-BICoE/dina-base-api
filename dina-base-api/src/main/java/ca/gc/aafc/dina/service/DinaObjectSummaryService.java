package ca.gc.aafc.dina.service;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import ca.gc.aafc.dina.jpa.DinaObjectSummary;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;

/**
 * Service that can load summary objects without loading the entire entity.
 * Only available for DinaEntity since some specific fields are required.
 */
@Service
public class DinaObjectSummaryService {

  private final BaseDAO baseDAO;

  public DinaObjectSummaryService(BaseDAO baseDAO) {
    this.baseDAO = baseDAO;
  }

  public DinaObjectSummary findDinaObjectSummaryByUUID(Class<? extends DinaEntity> table, UUID uuid) {
    String sql = "SELECT new " + DinaObjectSummary.class.getCanonicalName() +
      "(t.uuid, t.group, t.createdBy) FROM " + table.getName() + " t WHERE uuid=:uuid";
    return baseDAO.findOneByQuery(DinaObjectSummary.class, sql, List.of(Pair.of("uuid", uuid)));
  }

}
