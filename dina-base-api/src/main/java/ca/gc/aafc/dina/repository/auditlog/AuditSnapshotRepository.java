package ca.gc.aafc.dina.repository.auditlog;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import ca.gc.aafc.dina.dto.AuditSnapshotDto;
import ca.gc.aafc.dina.repository.NoLinkInformation;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.AuditService.AuditInstance;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ReadOnlyResourceRepositoryBase;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;

@Repository
@ConditionalOnProperty(value = "dina.auditing.enabled", havingValue = "true")
public class AuditSnapshotRepository extends ReadOnlyResourceRepositoryBase<AuditSnapshotDto, Long> {

  private static final long DEFAULT_SKIP = 0L;
  private static final long DEFAULT_LIMIT = 100L;
  private static final String INSTANCE_FILTER_VALUE = "instanceId";
  private static final String AUTHOR_FILTER_VALUE = "author";

  private final AuditService service;

  public AuditSnapshotRepository(AuditService service) {
    super(AuditSnapshotDto.class);
    this.service = service;
  }

  @Override
  public ResourceList<AuditSnapshotDto> findAll(QuerySpec qs) {
    int limit = Optional.ofNullable(qs.getLimit()).orElse(DEFAULT_LIMIT).intValue();
    int skip = Optional.ofNullable(qs.getOffset()).orElse(DEFAULT_SKIP).intValue();

    Map<String, String> filters = getFilterMap(qs);
    String authorFilter = filters.get(AUTHOR_FILTER_VALUE);
    String instanceFilter = filters.get(INSTANCE_FILTER_VALUE);

    AuditInstance instance = AuditInstance.fromString(instanceFilter).orElse(null);

    List<AuditSnapshotDto> dtos = service.findAll(instance, authorFilter, limit, skip)
        .stream().map(AuditSnapshotRepository::toDto).collect(Collectors.toList());

    Long count = service.getResouceCount(authorFilter, instance);
    DefaultPagedMetaInformation meta = new DefaultPagedMetaInformation();
    meta.setTotalResourceCount(count);

    return new DefaultResourceList<>(dtos, meta, new NoLinkInformation());
  }

  @Override
  public AuditSnapshotDto findOne(Long id, QuerySpec querySpec) {
    // Disable findOne.
    throw new MethodNotAllowedException("method not allowed");
  }

  /** Converts Javers snapshot to our DTO format. */
  private static AuditSnapshotDto toDto(CdoSnapshot original) {
    // Get the snapshot state as a map:
    Map<String, Object> state = new HashMap<>();
    original.getState().forEachProperty((key, val) -> state.put(key, val));

    // Get the commit date as OffsetDateTime:
    OffsetDateTime commitDateTime = original.getCommitMetadata().getCommitDate().atOffset(OffsetDateTime.now().getOffset());

    return AuditSnapshotDto.builder()
        .id(original.getGlobalId().value() + "/" + commitDateTime)   
        .instanceId(original.getGlobalId().value())
        .state(state)
        .changedProperties(original.getChanged())
        .snapshotType(original.getType().toString())
        .version(original.getVersion())
        .author(original.getCommitMetadata().getAuthor())
        .commitDateTime(commitDateTime)
        .build();
  }

  /**
   * Converts Crnk's filters into a String/String Map.
   */
  private static Map<String, String> getFilterMap(QuerySpec qs) {
    Map<String, String> map = new HashMap<>();
    qs.getFilters().forEach(
        it -> map.put(
        it.getPath().toString(),
        it.getValue().toString()
      )
    );
    return map;
  }

  public static String generateUrlLink(String resourceType, String id) {
    if (StringUtils.isBlank(resourceType) || StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("A url can not be generated without a type and id");
    }
    return "/" + AuditSnapshotDto.TYPE_NAME + "?filter[" + INSTANCE_FILTER_VALUE + "]=" + resourceType + "/" + id;
  }
}
