package ca.gc.aafc.dina.repository.auditlog;

import org.apache.commons.lang3.StringUtils;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;

import ca.gc.aafc.dina.dto.AuditSnapshotDto;
import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.JsonApiMeta;
import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.FilterExpression;
import ca.gc.aafc.dina.filter.FilterGroup;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.repository.JsonApiModelBuilderHelper;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.AuditService.AuditInstance;

import static com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder.jsonApiModel;
import static com.toedter.spring.hateoas.jsonapi.MediaTypes.JSON_API_VALUE;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

@RestController
@ConditionalOnProperty(value = "dina.auditing.enabled", havingValue = "true")
public class AuditSnapshotRepository {

  private static final String INSTANCE_FILTER_VALUE = "instanceId";
  private static final String AUTHOR_FILTER_VALUE = "author";

  private final AuditService service;
  private final BuildProperties buildProperties;

  public AuditSnapshotRepository(AuditService service, BuildProperties buildProperties) {
    this.service = service;
    this.buildProperties = buildProperties;
  }

  @GetMapping(path = AuditSnapshotDto.TYPE_NAME, produces = JSON_API_VALUE)
  public ResponseEntity<RepresentationModel<?>> findAll(HttpServletRequest req) {
    String queryString = req != null ? DinaRepositoryV2.decodeQueryString(req) : null;
    QueryComponent queryComponents = QueryStringParser.parse(queryString);

    int pageOffset = DinaRepositoryV2.toSafePageOffset(queryComponents.getPageOffset());
    int pageLimit = DinaRepositoryV2.toSafePageLimit(queryComponents.getPageLimit());

    Map<String, String> filters = getFilterMap(queryComponents);
    String authorFilter = filters.get(AUTHOR_FILTER_VALUE);
    String instanceFilter = filters.get(INSTANCE_FILTER_VALUE);

    AuditInstance instance = AuditInstance.fromString(instanceFilter).orElse(null);

    List<AuditSnapshotDto> dtos = service.findAll(instance, authorFilter, pageLimit, pageOffset)
      .stream().map(AuditSnapshotRepository::toDto).toList();

    Long count = service.getResouceCount(authorFilter, instance);

    JsonApiModelBuilder mainBuilder = jsonApiModel();
    List<RepresentationModel<?>> repModels = new ArrayList<>();
    Set<UUID> included = new HashSet<>();
    for (AuditSnapshotDto currResource : dtos) {
      JsonApiModelBuilder builder = JsonApiModelBuilderHelper.
        createJsonApiModelBuilder(JsonApiDto.builder().dto(currResource).build(), mainBuilder, included);
      repModels.add(builder.build());
    }

    // use custom metadata instead of PagedModel.PageMetadata so we can control
    // the content and key names
    JsonApiMeta.builder()
      .totalResourceCount(Math.toIntExact(count))
      .moduleVersion(buildProperties.getVersion())
      .build()
      .populateMeta(mainBuilder::meta);

    mainBuilder.model(CollectionModel.of(repModels));

    return ResponseEntity.ok(mainBuilder.build());
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
   * Converts QueryComponent filters into a String/String Map.
   */
  private static Map<String, String> getFilterMap(QueryComponent queryComponents) {
    Map<String, String> map = new HashMap<>();

    if (queryComponents == null || queryComponents.getFilters() == null) {
      return Map.of();
    }

    queryComponents.getFilterExpression().ifPresent( fe -> map.put(fe.attribute(), fe.value()));

    switch (queryComponents.getFilters()) {
      case FilterExpression fe -> map.put(fe.attribute(), fe.value());
      case FilterGroup fg -> {
        for (FilterComponent gfg : fg.getComponents()) {
          if (gfg instanceof FilterExpression gfe) {
            map.put(gfe.attribute(), gfe.value());
          }
        }
      }
      default ->
        throw new IllegalStateException("Unexpected value: " + queryComponents.getFilters());
    }
    return map;
  }

  public static String generateUrlLink(String resourceType, String id) {
    if (StringUtils.isBlank(resourceType) || StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("A url can not be generated without a type and id");
    }
    return "/" + AuditSnapshotDto.TYPE_NAME + "?filter[" + INSTANCE_FILTER_VALUE + "]=" + resourceType + "/" + id;
  }
}
