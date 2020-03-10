package ca.gc.aafc.dina.repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Selection;
import javax.transaction.Transactional;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.JpaDtoMapper;
import ca.gc.aafc.dina.repository.meta.JpaMetaInformationProvider;
import ca.gc.aafc.dina.repository.meta.JpaMetaInformationProvider.JpaMetaInformationParams;
import ca.gc.aafc.dina.util.TriFunction;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Repository
@Transactional
//CHECKSTYLE:OFF AnnotationUseStyle
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class JpaDtoRepository {

  @NonNull
  @Getter
  private final EntityManager entityManager;

  @NonNull
  @Getter
  private final BaseDAO baseDAO;

  @NonNull
  @Getter
  private final SelectionHandler selectionHandler;

  @NonNull
  @Getter
  private final JpaDtoMapper dtoJpaMapper;
  
  /* Forces CRNK to not display any top-level links. */
  private static final NoLinkInformation NO_LINK_INFORMATION = new NoLinkInformation();

  /**
   * Query the DTO repository backed by a JPA datasource for a list of DTOs.
   *
   * @param sourceDtoClass
   *          the source DTO class. It will be PcrBatchDto for GET /pcrBatch/1 , and it will be
   *          PcrBatchDto for /pcrBatch/1/reactions .
   * @param querySpec
   *          the crnk QuerySpec
   * @param resourceRegistry
   *          the crnk ResourceRegistry
   * @param customFilter
   *          custom JPA filter
   * @param customRoot
   *          function to change the root path of the query. E.g. when searching related elements in
   *          a request like localhost:8080/api/pcrBatch/10/reactions
   * @return the resource list
   */
  public <D> ResourceList<D> findAll(FindAllParams options) {
    QuerySpec querySpec = options.getQuerySpec();
    Class<?> sourceDtoClass = options.getSourceDtoClass();
    Function<From<?, ?>, From<?, ?>> customRoot = options.getCustomRoot();
    ResourceRegistry resourceRegistry = options.getResourceRegistry();
    TriFunction<From<?, ?>, CriteriaQuery<?>, CriteriaBuilder, Predicate> customFilter = options.getCustomFilter();
    JpaMetaInformationProvider metaInformationProvider = options.getMetaInformationProvider();
    
    @SuppressWarnings("unchecked")
    Class<D> targetDtoClass = (Class<D>) querySpec.getResourceClass();
    
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<?> criteriaQuery = cb.createQuery();
    From<?, ?> sourcePath = criteriaQuery.from(dtoJpaMapper.getEntityClassForDto(sourceDtoClass));

    From<?, ?> targetPath = customRoot != null ? customRoot.apply(sourcePath) : sourcePath;

    criteriaQuery.select((Selection) targetPath);

    // Eager load any included entities:
    for (IncludeRelationSpec relation : querySpec.getIncludedRelations()) {
      FetchParent<?, ?> join = targetPath;
      for (String path : relation.getAttributePath()) {
        join = join.fetch(path, JoinType.LEFT);
      }
    }

    if (querySpec.getSort().isEmpty()) {
      // When no sorts are requested, sort by ascending ID by default.
      criteriaQuery.orderBy(cb.asc(
          this.selectionHandler.getIdExpression(targetPath, targetDtoClass, resourceRegistry)));
    } else {
      // Otherwise use the requested sorts.
      List<Order> orders = querySpec.getSort().stream().map(sort -> {
        Function<Expression<?>, Order> orderFunc = sort.getDirection() == Direction.ASC ? cb::asc
            : cb::desc;
        return orderFunc
            .apply(this.selectionHandler.getExpression(targetPath, sort.getAttributePath()));
      }).collect(Collectors.toList());

      criteriaQuery.orderBy(orders);
    }

    // Add the custom filter to the criteria query.
    if (customFilter != null) {
      criteriaQuery.where(customFilter.apply(targetPath, criteriaQuery, cb));
    }

    List<?> result = entityManager.createQuery(criteriaQuery)
        .setFirstResult(
            Optional.ofNullable(querySpec.getOffset()).orElse(Long.valueOf(0)).intValue())
        .setMaxResults(
            Optional.ofNullable(querySpec.getLimit()).orElse(Long.valueOf(100)).intValue())
        .getResultList();

    return new DefaultResourceList<>(
      result.stream()
        .map(entity -> (D) dtoJpaMapper.toDto(entity, querySpec, resourceRegistry))
        .collect(Collectors.toList()),
        metaInformationProvider.getMetaInformation(
            JpaMetaInformationParams.builder()
              .sourceResourceClass(sourceDtoClass)
              .customRoot(customRoot).customFilter(customFilter).build()
        ),
        NO_LINK_INFORMATION
      );
  }

  /**
   * Update a JPA entity using a DTO.
   *
   * @param resource
   * @return the updated resource's ID
   */
  public Serializable save(Object resource, ResourceRegistry resourceRegistry) {
    // Get the entity of this DTO.
    Object id = PropertyUtils.getProperty(
        resource,
        selectionHandler.getIdAttribute(resource.getClass(), resourceRegistry)
    );
    Object entity = baseDAO.findOneById(
        id,
        dtoJpaMapper.getEntityClassForDto(resource.getClass())
    );

    this.dtoJpaMapper.applyDtoToEntity(resource, entity, resourceRegistry);

    return (Serializable) baseDAO.getId(entity);
  }

  /**
   * Persist a JPA entity using a DTO.
   * 
   * @param resource
   * @return the created resource's ID
   */
  public Serializable create(Object resource, ResourceRegistry resourceRegistry) {
    Object entity = BeanUtils
        .instantiate(this.dtoJpaMapper.getEntityClassForDto(resource.getClass()));

    this.dtoJpaMapper.applyDtoToEntity(resource, entity, resourceRegistry);

    entityManager.persist(entity);

    return (Serializable) baseDAO.getId(entity);
  }

  /**
   * Deletes a JPA entity given a DTO.
   * 
   * @param resource
   *          the resource DTO to be deleted.
   * @param resourceRegistry
   *          the Crnk ResourceRegistry.
   */
  public void delete(Object resource, ResourceRegistry resourceRegistry) {
    Object entity = baseDAO.findOneById(
      resourceRegistry.findEntry(resource.getClass()).getResourceInformation().getId(resource),
      this.dtoJpaMapper.getEntityClassForDto(resource.getClass())
    );
    entityManager.remove(entity);
  }

  /**
   * Named parameters for the "findAll" method.
   */
  @Builder
  @Getter
  public static class FindAllParams {
    @NonNull private Class<?> sourceDtoClass;
    @NonNull private QuerySpec querySpec;
    @NonNull private ResourceRegistry resourceRegistry;
    
    // Provide a null "meta" section by default:
    @NonNull
    @Builder.Default
    private JpaMetaInformationProvider metaInformationProvider = params -> null;
    
    @Nullable private TriFunction<From<?, ?>, CriteriaQuery<?>, CriteriaBuilder, Predicate> customFilter;
    @Nullable private Function<From<?, ?>, From<?, ?>> customRoot;
  }

}
