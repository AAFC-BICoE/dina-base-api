package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.mapper.DinaFieldAdapterHandler;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.DinaMappingLayer;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.repository.meta.DinaMetaInfo;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaAuthorizationService;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.registry.ResourceRegistryAware;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.IncludeRelationSpec;
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
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.boot.info.BuildProperties;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JSONAPI repository that interfaces using DTOs, and uses JPA entities internally. Sparse fields
 * sets are handled by the underlying Crnk ResourceRepository.
 *
 * @param <D> - Dto type
 * @param <E> - Entity type
 */
@Transactional
public class DinaRepository<D, E extends DinaEntity>
  implements ResourceRepository<D, Serializable>, ResourceRegistryAware, MetaRepository<D> {

  /* Forces CRNK to not display any top-level links. */
  private static final NoLinkInformation NO_LINK_INFORMATION = new NoLinkInformation();

  @Getter
  private final Class<D> resourceClass;
  private final Class<E> entityClass;

  private final DinaService<E> dinaService;
  private final Optional<DinaAuthorizationService> authorizationService;
  private final Optional<AuditService> auditService;

  private final DinaMappingLayer<D, E> mappingLayer;
  private final DinaFilterResolver filterResolver;

  private final List<Map<String, String>> externalMetaMap;

  private final BuildProperties buildProperties;

  private static final long DEFAULT_LIMIT = 100;

  @Getter
  @Setter(onMethod_ = @Override)
  private ResourceRegistry resourceRegistry;
  private final DinaMappingRegistry registry;

  @Inject
  public DinaRepository(
    @NonNull DinaService<E> dinaService,
    @NonNull Optional<DinaAuthorizationService> authorizationService,
    @NonNull Optional<AuditService> auditService,
    @NonNull DinaMapper<D, E> dinaMapper,
    @NonNull Class<D> resourceClass,
    @NonNull Class<E> entityClass,
    @NonNull DinaFilterResolver filterResolver,
    ExternalResourceProvider externalResourceProvider,
    @NonNull BuildProperties buildProperties
  ) {
    this.dinaService = dinaService;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.resourceClass = resourceClass;
    this.entityClass = entityClass;
    this.filterResolver = filterResolver;
    this.buildProperties = buildProperties;
    if (externalResourceProvider != null) {
      this.externalMetaMap =
        DinaMetaInfo.parseExternalTypes(resourceClass, externalResourceProvider);
    } else {
      this.externalMetaMap = null;
    }
    this.registry = new DinaMappingRegistry(resourceClass);
    this.mappingLayer = new DinaMappingLayer<>(resourceClass, dinaMapper, dinaService, this.registry);
  }

  /**
   * Returns a resource with a given id. Relations that are not included in the query spec are
   * mapped in a shallow form. Relations included in the query spec are eager loaded.
   *
   * @param querySpec - query spec of the request
   * @return - list of resources
   */
  @Override
  public D findOne(Serializable id, QuerySpec querySpec) {
    querySpec.setLimit(1L);
    ResourceList<D> resourceList = findAll(Collections.singletonList(id), querySpec);

    if (resourceList.size() == 0) {
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
  @Override
  public ResourceList<D> findAll(Collection<Serializable> ids, QuerySpec querySpec) {
    QuerySpec newQuery = querySpec.clone();
    newQuery.setFilters(resolveFilterSpecs(resourceClass, querySpec.getFilters(), registry));
    String idName = SelectionHandler.getIdAttribute(resourceClass, resourceRegistry);

    List<D> dList = mappingLayer.mapEntitiesToDto(newQuery, fetchEntities(ids, newQuery, idName));

    Long resourceCount = dinaService.getResourceCount(
      entityClass,
      (cb, root) -> filterResolver.buildPredicates(newQuery, cb, root, ids, idName));

    DefaultPagedMetaInformation metaInformation = new DefaultPagedMetaInformation();
    metaInformation.setTotalResourceCount(resourceCount);
    return new DefaultResourceList<>(dList, metaInformation, NO_LINK_INFORMATION);
  }

  private static List<FilterSpec> resolveFilterSpecs(
    Class<?> resource,
    List<FilterSpec> filters,
    DinaMappingRegistry registry
  ) {
    List<FilterSpec> newFilters = new ArrayList<>();
    for (FilterSpec filterSpec : filters) {
      List<String> attributePath = filterSpec.getAttributePath();
      Class<?> dtoClass = resource;

      // find last attribute in path
      String attr = attributePath.stream().reduce((s, s2) -> s2)
        .orElseThrow(() -> new IllegalArgumentException("Query spec must provide an attribute path"));

      // Find nested dto class
      for (String attribute : attributePath) {
        Optional<DinaMappingRegistry.InternalRelation> relation = registry
          .findMappableRelationsForClass(dtoClass).stream()
          .filter(internalRelation -> internalRelation.getName().equalsIgnoreCase(attribute))
          .findAny();
        if (relation.isPresent()) {
          dtoClass = relation.get().getElementType();
        } else {
          break;
        }
      }

      DinaFieldAdapterHandler<?> handler = registry.getFieldAdaptersPerClass().get(resource);
      if (handler != null) {
        handler.findFilterSpec(attr).ifPresentOrElse(
          specs -> newFilters.addAll(List.of(specs.apply(filterSpec.getValue()))),
          () -> newFilters.add(filterSpec));
      }
    }

    return newFilters;
  }

  private List<E> fetchEntities(Collection<Serializable> ids, QuerySpec querySpec, String idName) {
    List<IncludeRelationSpec> relationsToEagerLoad = querySpec.getIncludedRelations().stream()
      .filter(ir -> {
        // Skip eager loading on JsonApiExternalRelation-marked fields:
        Class<?> dtoClass = querySpec.getResourceClass();
        for (String attr : ir.getAttributePath()) {
          if (isExternalRelation(dtoClass, attr)) {
            return false;
          }
          dtoClass = PropertyUtils.getPropertyClass(dtoClass, attr);
        }
        return true;
      }).collect(Collectors.toList());

    return dinaService.findAll(
      entityClass,
      (cb, root) -> {
        DinaFilterResolver.eagerLoadRelations(root, relationsToEagerLoad);
        return filterResolver.buildPredicates(querySpec, cb, root, ids, idName);
      },
      (cb, root) -> DinaFilterResolver.getOrders(querySpec, cb, root),
      Math.toIntExact(querySpec.getOffset()),
      Optional.ofNullable(querySpec.getLimit()).orElse(DEFAULT_LIMIT).intValue());
  }

  @Override
  public <S extends D> S save(S resource) {
    Object id = PropertyUtils.getProperty(resource, findIdFieldName(resourceClass));

    E entity = dinaService.findOne(id, entityClass);
    authorizationService.ifPresent(auth -> auth.authorizeUpdate(entity));

    if (entity == null) {
      throw new ResourceNotFoundException(
        resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }

    mappingLayer.mapToEntity(resource, entity);
    dinaService.update(entity);
    auditService.ifPresent(service -> service.audit(resource));
    return resource;
  }

  @Override
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public <S extends D> S create(S resource) {
    E entity = entityClass.getConstructor().newInstance();

    mappingLayer.mapToEntity(resource, entity);

    authorizationService.ifPresent(auth -> auth.authorizeCreate(entity));
    dinaService.create(entity);

    D dto = findOne(
      (Serializable) PropertyUtils.getProperty(entity, findIdFieldName(resourceClass)),
      new QuerySpec(resourceClass));
    auditService.ifPresent(service -> service.audit(dto));
    return (S) dto;
  }

  @Override
  public void delete(Serializable id) {
    E entity = dinaService.findOne(id, entityClass);
    if (entity == null) {
      throw new ResourceNotFoundException(
        resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }
    authorizationService.ifPresent(auth -> auth.authorizeDelete(entity));
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

  /**
   * Returns true if the given class has a given field with an annotation of a given type.
   *
   * @param <T>   - Class type
   * @param clazz - class of the field
   * @param field - field to check
   * @return true if a dto field is generated and read-only
   */
  @SneakyThrows(NoSuchFieldException.class)
  private static <T> boolean isExternalRelation(Class<T> clazz, String field) {
    return clazz.getDeclaredField(field).isAnnotationPresent(JsonApiExternalRelation.class);
  }

  /**
   * Returns the id field name for a given class.
   *
   * @param clazz - class to find the id field name for
   * @return - id field name for a given class.
   */
  private String findIdFieldName(Class<?> clazz) {
    return this.resourceRegistry.findEntry(clazz)
      .getResourceInformation()
      .getIdField()
      .getUnderlyingName();
  }
}
