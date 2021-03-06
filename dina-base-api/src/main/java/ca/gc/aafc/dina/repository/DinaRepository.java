package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.DinaMappingLayer;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.repository.auditlog.AuditSnapshotRepository;
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.repository.meta.AttributeMetaInfoProvider;
import ca.gc.aafc.dina.repository.meta.DinaMetaInfo;
import ca.gc.aafc.dina.security.DinaAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.engine.http.HttpRequestContextAware;
import io.crnk.core.engine.http.HttpRequestContextProvider;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.MetaRepository;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;
import io.crnk.core.resource.meta.MetaInformation;
import io.crnk.core.resource.meta.PagedMetaInformation;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * JSONAPI repository that interfaces using DTOs, and uses JPA entities internally. Sparse fields
 * sets are handled by the underlying Crnk ResourceRepository.
 *
 * @param <D> - Dto type
 * @param <E> - Entity type
 */
public class DinaRepository<D, E extends DinaEntity>
  implements ResourceRepository<D, Serializable>, MetaRepository<D>, HttpRequestContextAware {

  /* Forces CRNK to not display any top-level links. */
  private static final NoLinkInformation NO_LINK_INFORMATION = new NoLinkInformation();
  private static final long DEFAULT_LIMIT = 100;
  public static final String PERMISSION_META_HEADER_KEY = "include-dina-permission";

  @Getter
  private final Class<D> resourceClass;
  private final Class<E> entityClass;

  private final DinaService<E> dinaService;
  private final DinaAuthorizationService authorizationService;
  private final Optional<AuditService> auditService;

  private final DinaMappingLayer<D, E> mappingLayer;
  private final DinaFilterResolver filterResolver;

  private final List<Map<String, String>> externalMetaMap;

  private final BuildProperties buildProperties;
  private final DinaMappingRegistry registry;
  private final boolean hasFieldAdapters;
  private HttpRequestContextProvider httpRequestContextProvider;

  public DinaRepository(
    @NonNull DinaService<E> dinaService,
    @NonNull DinaAuthorizationService authorizationService,
    @NonNull Optional<AuditService> auditService,
    @NonNull DinaMapper<D, E> dinaMapper,
    @NonNull Class<D> resourceClass,
    @NonNull Class<E> entityClass,
    DinaFilterResolver filterResolver,
    ExternalResourceProvider externalResourceProvider,
    @NonNull BuildProperties buildProperties
  ) {
    this.dinaService = dinaService;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.resourceClass = resourceClass;
    this.entityClass = entityClass;
    this.filterResolver = Objects.requireNonNullElseGet(
      filterResolver, () -> new DinaFilterResolver(null));
    this.buildProperties = buildProperties;
    if (externalResourceProvider != null) {
      this.externalMetaMap =
        DinaMetaInfo.parseExternalTypes(resourceClass, externalResourceProvider);
    } else {
      this.externalMetaMap = null;
    }
    this.registry = new DinaMappingRegistry(resourceClass);
    this.mappingLayer = new DinaMappingLayer<>(resourceClass, dinaMapper, dinaService, this.registry);
    this.hasFieldAdapters = CollectionUtils.isNotEmpty(registry.getFieldAdaptersPerClass().keySet());
  }

  /**
   * Returns a resource with a given id. Relations that are not included in the query spec are
   * mapped in a shallow form. Relations included in the query spec are eager loaded.
   *
   * @param querySpec - query spec of the request
   * @return - list of resources
   */
  @Transactional(readOnly = true)
  @Override
  public D findOne(Serializable id, QuerySpec querySpec) {
    querySpec.setLimit(1L);
    ResourceList<D> resourceList = findAll(Collections.singletonList(id), querySpec);

    if (resourceList.size() == 0) {
      auditService.ifPresent(service -> { // Past Deleted records with audit logs throw Gone.
        final String resourceType = querySpec.getResourceType();
        final AuditService.AuditInstance auditInstance = AuditService.AuditInstance.builder()
          .id(id.toString()).type(resourceType).build();
        if (service.hasTerminalSnapshot(auditInstance)) {
          throw new GoneException(
            "GONE",
            "The Resource has been deleted but audit records remain, see the links.about section",
            AuditSnapshotRepository.generateUrlLink(resourceType, id.toString()));
        }
      });
      throw new ResourceNotFoundException(
        resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }

    return resourceList.get(0);
  }

  /**
   * Returns a list of resources from a given query spec. Relations that are not included in the
   * query spec are mapped in a shallow form. Relations included in the query spec are eager
   * loaded.
   *
   * @param querySpec - query spec of the request
   * @return - list of resources
   */
  @Transactional(readOnly = true)
  @Override
  public ResourceList<D> findAll(QuerySpec querySpec) {
    return findAll(null, querySpec);
  }

  /**
   * Returns a list of resources from a given collection of ids and a query spec. Relations that are
   * not included in the query spec are mapped in a shallow form. Relations included in the query
   * spec are eager loaded.
   *
   * @param ids       - ids to query
   * @param querySpec - query spec of the request
   * @return - list of resources
   */
  @Transactional(readOnly = true)
  @Override
  public ResourceList<D> findAll(Collection<Serializable> ids, QuerySpec querySpec) {
    final QuerySpec spec = resolveFilterAdapters(querySpec);
    String idName = findIdFieldName(resourceClass);

    List<E> entities = fetchEntities(ids, spec, idName);
    List<D> dList = mappingLayer.mapEntitiesToDto(spec, entities);

    handleMetaPermissionsResponse(entities, dList);

    Long resourceCount = dinaService.getResourceCount( entityClass,
      (criteriaBuilder, root, em) -> filterResolver.buildPredicates(spec, criteriaBuilder, root, ids, idName, em));

    DefaultPagedMetaInformation metaInformation = new DefaultPagedMetaInformation();
    metaInformation.setTotalResourceCount(resourceCount);
    return new DefaultResourceList<>(dList, metaInformation, NO_LINK_INFORMATION);
  }

  private void handleMetaPermissionsResponse(List<E> entities, List<D> dList) {
    if (permissionsNotRequested()) {
      return;
    }

    @SuppressWarnings("unchecked") // we checked
    final List<AttributeMetaInfoProvider> providers = (List<AttributeMetaInfoProvider>) dList;
    entities.forEach(e -> {
      // Return permissions for the entity
      Set<String> permissions = authorizationService.getPermissionsForObject(e);
      // but apply response to the DTO.
      providers.stream().filter(d -> d.getUuid().equals(e.getUuid())).findFirst()
        .ifPresent(provider -> provider.setMeta(
          AttributeMetaInfoProvider.DinaJsonMetaInfo.builder().permissions(permissions).build())
      );
    });
  }

  private boolean permissionsNotRequested() {
    if (!AttributeMetaInfoProvider.class.isAssignableFrom(resourceClass)
      || !httpRequestContextProvider.hasThreadRequestContext()) {
      return true;
    }

    return httpRequestContextProvider.getRequestContext().getRequestHeader(PERMISSION_META_HEADER_KEY) == null;
  }

  /**
   * Convenience method to resolve the filters of a given query spec for {@link
   * ca.gc.aafc.dina.mapper.DinaFieldAdapter}'s. A QuerySpec will only be processed if a resources entity
   * graph contains any field adapters.
   *
   * @param querySpec - QuerySpec with filters to resolve
   * @return A new QuerySpec with the resolved filters, or the original query spec.
   */
  private QuerySpec resolveFilterAdapters(QuerySpec querySpec) {
    if (hasFieldAdapters) {
      QuerySpec spec = querySpec.clone();
      spec.setFilters(
        DinaFilterResolver.resolveFilterAdapters(resourceClass, querySpec.getFilters(), registry));
      return spec;
    }
    return querySpec;
  }

  private List<E> fetchEntities(Collection<Serializable> ids, QuerySpec querySpec, String idName) {
    return dinaService.findAll(
      entityClass,
      (criteriaBuilder, root, em) -> {
        DinaFilterResolver.leftJoinRelations(root, querySpec, registry);
        return filterResolver.buildPredicates(querySpec, criteriaBuilder, root, ids, idName, em);
      },
      (cb, root) -> DinaFilterResolver.getOrders(querySpec, cb, root),
      Math.toIntExact(querySpec.getOffset()),
      Optional.ofNullable(querySpec.getLimit()).orElse(DEFAULT_LIMIT).intValue());
  }

  @Transactional
  @Override
  public <S extends D> S save(S resource) {
    Object id = PropertyUtils.getProperty(resource, findIdFieldName(resourceClass));

    E entity = dinaService.findOne(id, entityClass);

    authorizationService.authorizeUpdate(entity);

    if (entity == null) {
      throw new ResourceNotFoundException(
        resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }

    mappingLayer.mapToEntity(resource, entity);
    dinaService.update(entity);
    auditService.ifPresent(service -> service.audit(resource));
    return resource;
  }

  @Transactional
  @Override
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public <S extends D> S create(S resource) {
    E entity = entityClass.getConstructor().newInstance();

    mappingLayer.mapToEntity(resource, entity);

    authorizationService.authorizeCreate(entity);
    dinaService.create(entity);

    D dto = findOne(
      (Serializable) PropertyUtils.getProperty(entity, findIdFieldName(resourceClass)),
      new QuerySpec(resourceClass));
    auditService.ifPresent(service -> service.audit(dto));
    return (S) dto;
  }

  @Transactional
  @Override
  public void delete(Serializable id) {
    E entity = dinaService.findOne(id, entityClass);
    if (entity == null) {
      throw new ResourceNotFoundException(
        resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }
    authorizationService.authorizeDelete(entity);
    dinaService.delete(entity);

    auditService.ifPresent(service -> service.auditDeleteEvent(mappingLayer.toDtoSimpleMapping(
      entity)));
  }

  @Override
  public DinaMetaInfo getMetaInformation(
    Collection<D> collection, QuerySpec querySpec, MetaInformation metaInformation
  ) {
    DinaMetaInfo metaInfo = new DinaMetaInfo();
    // Set External types
    metaInfo.setExternal(externalMetaMap);
    // Set resource counts
    if (metaInformation instanceof PagedMetaInformation) {
      PagedMetaInformation pagedMetaInformation = (PagedMetaInformation) metaInformation;
      if (pagedMetaInformation.getTotalResourceCount() != null) {
        metaInfo.setTotalResourceCount(pagedMetaInformation.getTotalResourceCount());
      }
    } else {
      metaInfo.setTotalResourceCount((long) collection.size());
    }
    metaInfo.setModuleVersion(buildProperties.getVersion());
    return metaInfo;
  }

  @SneakyThrows
  public void validate(D resource) {
    E entity = entityClass.getConstructor().newInstance();
    mappingLayer.applySimpleMappingToEntity(resource, entity);

    // validation group should probably be set here
    dinaService.validateConstraints(entity, null);
    dinaService.validateBusinessRules(entity);
  }

  /**
   * Returns the id field name for a given class.
   *
   * @param clazz - class to find the id field name for
   * @return - id field name for a given class.
   */
  private String findIdFieldName(Class<?> clazz) {
    return this.registry.findJsonIdFieldName(clazz);
  }

  @Override
  public void setHttpRequestContextProvider(HttpRequestContextProvider httpRequestContextProvider) {
    this.httpRequestContextProvider = httpRequestContextProvider;
  }
}
