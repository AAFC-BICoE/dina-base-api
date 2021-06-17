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
import ca.gc.aafc.dina.security.spring.SecurityChecker;
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
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.transaction.Transactional;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
  implements ResourceRepository<D, Serializable>, MetaRepository<D>, HttpRequestContextAware {

  /* Forces CRNK to not display any top-level links. */
  private static final NoLinkInformation NO_LINK_INFORMATION = new NoLinkInformation();
  private static final long DEFAULT_LIMIT = 100;
  public static final String PERMISSION_META_HEADER_KEY = "dina-permission-enabled";

  @Getter
  private final Class<D> resourceClass;
  private final Class<E> entityClass;

  private final DinaService<E> dinaService;
  private final SecurityChecker securityChecker;
  private final Optional<DinaAuthorizationService> authorizationService;
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
    @NonNull Optional<DinaAuthorizationService> authorizationService,
    @NonNull Optional<AuditService> auditService,
    @NonNull DinaMapper<D, E> dinaMapper,
    @NonNull Class<D> resourceClass,
    @NonNull Class<E> entityClass,
    @NonNull SecurityChecker securityChecker,
    DinaFilterResolver filterResolver,
    ExternalResourceProvider externalResourceProvider,
    @NonNull BuildProperties buildProperties
  ) {
    this.dinaService = dinaService;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.resourceClass = resourceClass;
    this.entityClass = entityClass;
    this.securityChecker = securityChecker;
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
    final QuerySpec spec = resolveFilterAdapters(querySpec);
    String idName = findIdFieldName(resourceClass);

    List<D> dList = mappingLayer.mapEntitiesToDto(spec, fetchEntities(ids, spec, idName));

    handleMetaPermissionsResponse(dList);//TODO use a new service possibly

    Long resourceCount = dinaService.getResourceCount( entityClass,
      (criteriaBuilder, root, em) -> filterResolver.buildPredicates(spec, criteriaBuilder, root, ids, idName, em));

    DefaultPagedMetaInformation metaInformation = new DefaultPagedMetaInformation();
    metaInformation.setTotalResourceCount(resourceCount);
    return new DefaultResourceList<>(dList, metaInformation, NO_LINK_INFORMATION);
  }

  @SuppressWarnings({"unchecked"}) //Method exits if resource is not castable
  private void handleMetaPermissionsResponse(List<D> dList) {
    if (!AttributeMetaInfoProvider.class.isAssignableFrom(resourceClass)
      || !httpRequestContextProvider.hasThreadRequestContext()) {
      return;
    }

    Set<String> requestHeaderNames = httpRequestContextProvider.getRequestContext().getRequestHeaderNames();
    if (CollectionUtils.isNotEmpty(requestHeaderNames) &&
      requestHeaderNames.stream().anyMatch(rh -> rh.equalsIgnoreCase(PERMISSION_META_HEADER_KEY))) {
      setPermissions((List<AttributeMetaInfoProvider>) dList);
    }
  }

  private void setPermissions(List<AttributeMetaInfoProvider> providerList) {//TODO placeholder implementation
    authorizationService.ifPresent(as -> {
      Set<String> permissions = new HashSet<>();
      providerList.forEach(p -> {
        if (securityChecker.check(getPreAuthorizeExpression(
          "authorizeCreate",
          as.getClass().getSuperclass()))) {
          permissions.add("create");
        }
        if (securityChecker.check(getPreAuthorizeExpression(
          "authorizeUpdate",
          as.getClass().getSuperclass()))) {
          permissions.add("delete");
        }
        if (securityChecker.check(getPreAuthorizeExpression(
          "authorizeDelete",
          as.getClass().getSuperclass()))) {
          permissions.add("update");
        }
        p.setMeta(AttributeMetaInfoProvider.DinaJsonMetaInfo.builder().permissions(permissions).build());
      });
    });
  }

  private String getPreAuthorizeExpression(String methodName, Class<?> aClass) {
    Method matchingMethod = MethodUtils.getMatchingMethod(aClass, methodName, Object.class);
    PreAuthorize annotation = matchingMethod.getAnnotation(PreAuthorize.class);
    return annotation.value();
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
