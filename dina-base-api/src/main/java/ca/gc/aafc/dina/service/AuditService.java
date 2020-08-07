package ca.gc.aafc.dina.service;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@ConditionalOnProperty(value = "dina.auditing.enabled", havingValue = "true")
public class AuditService {

  private final Javers javers;
  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final Optional<DinaAuthenticatedUser> user;

  /**
   * Returns a list of Audit snapshots filtered by a given instance and author.
   * Author and instance can be null for un-filtered results.
   * 
   * @param instance - instance to filter may be null
   * @param author   - author to filter may be null
   * @param limit    - limit of results
   * @param skip     - amount of results to skip
   * @return list of Audit snapshots
   */
  public List<CdoSnapshot> findAll(AuditInstance instance, String author, int limit, int skip) {
    return AuditService.findAll(this.javers, instance, author, limit, skip);
  }

  /**
   * Get the total resource count by a given Author and Audit Instance. Author and
   * instance can be null for un-filtered counts.
   * 
   * @param author   - author to filter
   * @param instance - instance to filter
   * @return the total resource count
   */
  public Long getResouceCount(String author, AuditInstance instance) {
    return AuditService.getResouceCount(this.jdbcTemplate, author, instance);
  }

  /**
   * Commit a snapshot for a given object, A dina authenticated user will be set
   * as the commit author. a dina user bean must be present for a snap shot to be
   * generated.
   * 
   * @param obj - domain object state to persist
   */
  public void audit(@NonNull Object obj) {
    user.ifPresentOrElse(u -> this.javers.commit(u.getUsername(), obj), () -> logNoUser());
  }

  /**
   * Commit a shallow delete snapshot for a given object, A dina authenticated
   * user will be set as the commit author. a dina user bean must be present for a
   * snap shot to be generated.
   * 
   * @param obj - domain object state to persist
   */
  public void auditDeleteEvent(@NonNull Object obj) {
    user.ifPresentOrElse(
      u -> this.javers.commitShallowDelete(u.getUsername(), obj),
      () -> logNoUser());
  }

  /**
   * Returns a list of Audit snapshots using a given Javers Facade filtered by a
   * given instance and author. Author and instance can be null for un-filtered
   * results.
   * 
   * @param javers   - Facade to query
   * @param instance - instance to filter may be null
   * @param author   - author to filter may be null
   * @param limit    - limit of results
   * @param skip     - amount of results to skip
   * @return list of Audit snapshots
   */
  public static List<CdoSnapshot> findAll(Javers javers, AuditInstance instance, String author, int limit, int skip) {
    QueryBuilder queryBuilder;

    if (instance != null) {
      queryBuilder = QueryBuilder.byInstanceId(instance.getId(), instance.getType());
    } else {
      queryBuilder = QueryBuilder.anyDomainObject();
    }

    if (StringUtils.isNotBlank(author)) {
      queryBuilder.byAuthor(author);
    }

    queryBuilder.limit(limit);
    queryBuilder.skip(skip);

    return javers.findSnapshots(queryBuilder.build());
  }

  /**
   * Get the total resource count by a given Author and/or Audit Instance. Author
   * and instance can be null for un-filtered counts.
   * 
   * @param jdbc     - NamedParameterJdbcTemplate for the query
   * @param author   - author to filter
   * @param instance - instance filter to apply
   * @return the total resource count
   */
  public static Long getResouceCount(@NonNull NamedParameterJdbcTemplate jdbc, String author, AuditInstance instance) {

    String id = null;
    String type = null;

    if (instance != null) {
      id = instance.getId();
      type = instance.getType();
    }

    SqlParameterSource parameters = new MapSqlParameterSource()
      .addValue("author", author)
      .addValue("id", id)
      .addValue("type", type);

    String sql = getResouceCountSql(author, id);
    return jdbc.queryForObject(sql, parameters, Long.class);
  }

  /**
   * Returns the needed SQL String to return a resouce count for a specific author
   * and id. Author and id can be null for un-filtered counts.
   * 
   * @param author - author filter to apply
   * @param id     - id filter to apply
   * @return SQL String to return a resouce count
   */
  private static String getResouceCountSql(String author, String id) {
    String baseSql = "select count(*) from jv_snapshot s join jv_commit c on s.commit_fk = c.commit_pk where 1=1 %s %s ;";
    String sql = String.format(
      baseSql,
      StringUtils.isNotBlank(author) ? "and c.author = :author" : "",
      StringUtils.isNotBlank(id)
        ? "and global_id_fk = (select global_id_pk from jv_global_id where local_id = :id and type_name = :type)"
        : "");
    return sql;
  }

  private static void logNoUser() {
    log.warn("No Dina Authenticated user, Auditing has been skipped!");
  }

  @Builder
  @Data
  public static final class AuditInstance {

    @NonNull
    private final String type;
    @NonNull
    private final String id;

    /**
     * Returns an Optional AuditInstance from a string representation or empty if the
     * string is blank. Expected string format is {type}/{id}.
     * 
     * @param instanceString - string to parse
     * @throws IllegalArgumentException if the string has an invalid format.
     * @return Optional AuditInstance or empty for blank strings
     */
    public static Optional<AuditInstance> fromString(String instanceString) {
      if (StringUtils.isBlank(instanceString)) {
        return Optional.empty();
      }

      String[] split = instanceString.split("/");
      if (split.length != 2) {
        throw new IllegalArgumentException(
          "Invalid ID must be formatted as {type}/{id}: " + instanceString);
      }
      return Optional.of(AuditInstance.builder().type(split[0]).id(split[1]).build());
    }

  }

}
