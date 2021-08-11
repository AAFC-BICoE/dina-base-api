package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.security.DinaAuthenticatedUser;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.Javers;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "dina.auditing.enabled", havingValue = "true")
public class AuditService {

  private final Javers javers;
  private final Optional<DinaAuthenticatedUser> user;
  private final JaversDataService javersDataService;

  public static final String ANONYMOUS = "anonymous";

  /**
   * Returns true if the given audit instance has a terminal snapshot associated with it. Terminal snapshots
   * represent delete operations.
   *
   * @param auditInstance The audit instance to check
   * @return true if the given audit instance has a terminal snapshot associated with it
   */
  public boolean hasTerminalSnapshot(@NonNull AuditInstance auditInstance) {
    return this.findAll(auditInstance, null, Integer.MAX_VALUE, 0)
      .stream()
      .anyMatch(CdoSnapshot::isTerminal);
  }

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
    return AuditService.getResouceCount(this.javersDataService, author, instance);
  }

  /**
   * Removes the snapshots for a given instance.
   *
   * @param instance - instance to remove
   */
  @Transactional
  public void removeSnapshots(@NonNull AuditInstance instance) {
    List<CdoSnapshot> snapshots = javers.findSnapshots(
      QueryBuilder.byInstanceId(instance.getId(), instance.getType()).build());
    javersDataService.removeSnapshots(snapshots.stream()
      .map(c -> c.getCommitId().valueAsNumber())
      .collect(Collectors.toList()), instance.getId(), instance.getType());
  }

  /**
   * Commit a snapshot for a given object, A dina authenticated user will be set
   * as the commit author. If a user is not present the author will be anonymous.
   *
   * @param obj - domain object state to persist
   */
  public void audit(@NonNull Object obj) {
    if (user.isPresent()) {
      this.javers.commit(user.get().getUsername(), obj);
    } else {
      this.javers.commit(ANONYMOUS, obj);
    }
  }

  /**
   * Commit a shallow delete snapshot for a given object, A dina authenticated
   * user will be set as the commit author. If a user is not present the author
   * will be anonymous.
   *
   * @param obj - domain object state to persist
   */
  public void auditDeleteEvent(@NonNull Object obj) {
    if (user.isPresent()) {
      this.javers.commitShallowDelete(user.get().getUsername(), obj);
    } else {
      this.javers.commitShallowDelete(ANONYMOUS, obj);
    }
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
   * @param dataService     - JaversDataService to execute query
   * @param author   - author to filter
   * @param instance - instance filter to apply
   * @return the total resource count
   */
  public static Long getResouceCount(
    @NonNull JaversDataService dataService,
    String author,
    AuditInstance instance
  ) {
    String id = null;
    String type = null;

    if (instance != null) {
      id = instance.getId();
      type = instance.getType();
    }

    return dataService.getResourceCount(id, type, author);
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
