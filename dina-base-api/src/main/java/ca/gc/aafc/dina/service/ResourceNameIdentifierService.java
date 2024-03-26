package ca.gc.aafc.dina.service;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import ca.gc.aafc.dina.config.ResourceNameIdentifierConfig;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.IdentifiableByName;
import ca.gc.aafc.dina.jpa.BaseDAO;

/**
 * Service that get an identifier (UUID) by name (text given to an entity). The name is assigned by the user.
 * This class assumes the DinaEntity has unique name per group.
 */
@Service
@ConditionalOnBean(ResourceNameIdentifierConfig.class)
public class ResourceNameIdentifierService {

  private final BaseDAO baseDAO;
  private final ResourceNameIdentifierConfig config;

  public ResourceNameIdentifierService(BaseDAO baseDAO, ResourceNameIdentifierConfig config) {
    this.baseDAO = baseDAO;
    this.config = config;
  }

  /**
   * Find the UUID identifier by name (textual name given by the user).
   * Uniqueness of the name within the group is assumed.
   * @param entityClass
   * @param name textual name given by the user to the entity
   * @param group group where the uniqueness of the name is assumed
   * @return uuid or null if not found
   */
  public <T extends DinaEntity & IdentifiableByName> UUID findByName(Class<T> entityClass, String name, String group) {

    ResourceNameIdentifierConfig.ResourceNameConfig
      resourceNameConfig = config.getResourceNameConfig(entityClass).orElse(ResourceNameIdentifierConfig.DEFAULT_CONFIG);

    StringBuilder sb = new StringBuilder("SELECT t.uuid FROM " );
    sb.append(entityClass.getName());
    sb.append(" t WHERE ");
    sb.append(resourceNameConfig.groupColumn());
    sb.append("=:group");
    sb.append(" AND ");
    sb.append(resourceNameConfig.nameColumn());
    sb.append("=:name");

    return baseDAO.findOneByQuery(UUID.class, sb.toString(), List.of(Pair.of("name", name),
      Pair.of("group", group)));
  }

  public <T extends DinaEntity & IdentifiableByName> List<NameUUIDPair> findAllByNames(Class<T> entityClass, List<String> names, String group) {

    ResourceNameIdentifierConfig.ResourceNameConfig
      resourceNameConfig = config.getResourceNameConfig(entityClass).orElse(ResourceNameIdentifierConfig.DEFAULT_CONFIG);

    StringBuilder sb = new StringBuilder("SELECT new ");
    sb.append(NameUUIDPair.class.getCanonicalName());
    sb.append(" (t.");
    sb.append(resourceNameConfig.nameColumn());
    sb.append(", t.uuid) FROM " );
    sb.append(entityClass.getName());
    sb.append(" t WHERE ");
    sb.append(resourceNameConfig.groupColumn());
    sb.append("=:group");
    sb.append(" AND ");
    sb.append(resourceNameConfig.nameColumn());
    sb.append(" IN (:names)");

    return baseDAO.findAllByQuery(NameUUIDPair.class, sb.toString(), List.of(Pair.of("names", names),
      Pair.of("group", group)));
  }
}
