package ca.gc.aafc.dina.service;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import ca.gc.aafc.dina.entity.AuthorizationSummary;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;

/**
 * Service that can load authorization summary (information required to perform authorization) without loading the entire entity.
 * Only available for DinaEntity.
 */
@Service
public class AuthorizationSummaryService {

  private final BaseDAO baseDAO;

  public AuthorizationSummaryService(BaseDAO baseDAO) {
    this.baseDAO = baseDAO;
  }

  public AuthorizationSummary findAuthorizationSummaryByUUID(Class<? extends DinaEntity> table, UUID uuid) {
    String sql = "SELECT new "+ AuthorizationSummary.class.getCanonicalName() + "(t.uuid, t.group, t.createdBy) FROM " + table.getName() + " t WHERE uuid=:uuid";
    return baseDAO.findOneByQuery(AuthorizationSummary.class, sql, List.of(Pair.of("uuid", uuid)));
  }

}
