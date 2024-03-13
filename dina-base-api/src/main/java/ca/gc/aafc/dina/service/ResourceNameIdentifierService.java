package ca.gc.aafc.dina.service;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.BaseDAO;

/**
 * Service that get an identifier (UUID) by name (text given to an entity). The name is assigned by the user.
 * This class assumes the following:
 *  - the DinaEntity has variables name and group
 *  - the DinaEntity has unique name per group
 */
@Service
public class ResourceNameIdentifierService {

  private final BaseDAO baseDAO;

  public ResourceNameIdentifierService(BaseDAO baseDAO) {
    this.baseDAO = baseDAO;
  }

  public UUID findByName(Class<? extends DinaEntity> table, String name, String group) {
    String sql = "SELECT t.uuid FROM " + table.getName() + " t WHERE group=:group AND name=:name";
    return baseDAO.findOneByQuery(UUID.class, sql, List.of(Pair.of("name", name),
      Pair.of("group", group)));
  }

}
