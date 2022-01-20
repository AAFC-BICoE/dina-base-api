package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.exception.UnknownAttributeException;
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
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.boot.info.BuildProperties;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
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
  private final String idFieldName;

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

  @Setter
  private boolean caseSensitiveOrderBy = false;

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
    this.hasFieldAdapters = this.registry.hasFieldAdapters();
    this.idFieldName = this.registry.findJsonIdFieldName(resourceClass);
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
    // Find the Dto entity 
    List<D> dtoList = fetchEntities(Collections.singletonList(id), querySpec, true);

    if (dtoList.size() == 0) {
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

    return dtoList.get(0);
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
    // Retrieve all of the dto entities, authentication turned off.
    List<D> dtoList = fetchEntities(ids, querySpec, false);

    // Generate meta information
    Long resourceCount = dinaService.getResourceCount( entityClass,
      (criteriaBuilder, root, em) -> filterResolver.buildPredicates(querySpec, criteriaBuilder, root, ids, idFieldName, em));
    DefaultPagedMetaInformation metaInformation = new DefaultPagedMetaInformation();
    metaInformation.setTotalResourceCount(resourceCount);

    return new DefaultResourceList<>(dtoList, metaInformation, NO_LINK_INFORMATION);
  }

  /**
   * Helper method to retrieve a list of entities based on ids provided or QuerySpec. This method is
   * used for the findOne and findAll methods.
   * 
   * A limit will automatically be set based on if ids are provided to search for.
   * 
   * @param ids Entity ids to search the database for.
   * @param querySpec Query specifications to apply to the request.
   * @param readAuthorization If read authorization should be performed on each of entities found.
   * @return List of DTOs
   * @throws UnknownAttributeException If an attribute used in the {@link QuerySpec} is unknown
   */
  private List<D> fetchEntities(Collection<Serializable> ids, QuerySpec querySpec, boolean readAuthorization) throws UnknownAttributeException {
    // Setup filters for entity searching.
    final QuerySpec spec = resolveFilterAdapters(querySpec);
    if (spec.getLimit() == null) {
      spec.setLimit(ids == null ? DEFAULT_LIMIT : ids.size());
    }

    // Retrieve the entities using the dina service.
    List<E> entities = dinaService.findAll(
      entityClass,
      (criteriaBuilder, root, em) -> {
        DinaFilterResolver.leftJoinRelations(root, spec, registry);
        return filterResolver.buildPredicates(spec, criteriaBuilder, root, ids, idFieldName, em);
      },
      (cb, root) -> DinaFilterResolver.getOrders(spec, cb, root, caseSensitiveOrderBy),
      Math.toIntExact(spec.getOffset()),
      spec.getLimit().intValue()
    );
    List<D> dtoList = new ArrayList<D>();

    // Go through each of the entities found to perform authentication, 
    // setting permissions and converting to Dto entities.
    entities.forEach(entity -> {
      // Convert entity to DTO.
      D dto = mappingLayer.mapToDto(spec, entity);

      // Set permissions to the DTO if needed.
      if (permissionsRequested()) {
        if (dto instanceof AttributeMetaInfoProvider) {
          Set<String> permissions = authorizationService.getPermissionsForObject(entity);

          AttributeMetaInfoProvider dtoMeta = (AttributeMetaInfoProvider) dto;
          dtoMeta.setMeta(AttributeMetaInfoProvider.DinaJsonMetaInfo.builder()
            .permissionsProvider(authorizationService.getName())
            .permissions(permissions)
            .build()
          );
        }
      }

      // Add dto to dto entity list to return back.
      dtoList.add(dto);
    });

    return dtoList;
  }

  private boolean permissionsRequested() {
    if (!AttributeMetaInfoProvider.class.isAssignableFrom(resourceClass)
      || !httpRequestContextProvider.hasThreadRequestContext()) {
      return false;
    }

    return httpRequestContextProvider.getRequestContext().getRequestHeader(PERMISSION_META_HEADER_KEY) != null;
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
    QuerySpec spec = querySpec != null ? querySpec : new QuerySpec(resourceClass);

    if (hasFieldAdapters) {
      QuerySpec fieldAdapterSpec = spec.clone();
      fieldAdapterSpec.setFilters(
        DinaFilterResolver.resolveFilterAdapters(resourceClass, querySpec.getFilters(), registry));
      return fieldAdapterSpec;
    }
    return spec;
  }

  /**
   * Save an existing resource.
   * @param resource resource to update. If the resource to update was received in a PATCH
   *                 Crnk will give the current DTO (from findOne) with the fields received
   *                 in the PATCH changed.
   * @param <S>
   * @return
   */
  @Transactional
  @Override
  public <S extends D> S save(S resource) {
    Object id = PropertyUtils.getProperty(resource, idFieldName);

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

  /**
   * create an existing resource.
   *
   * @param resource resource to create. If the resource to create was received in a POST Crnk will give the
   *                 current DTO with the fields received in the POST. Fields missing from the request body
   *                 will default to the values set by the java class.
   * @param <S>
   * @return
   */
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
      (Serializable) PropertyUtils.getProperty(entity, idFieldName),
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

  @Override
  public void setHttpRequestContextProvider(HttpRequestContextProvider httpRequestContextProvider) {
    this.httpRequestContextProvider = httpRequestContextProvider;
  }
}
