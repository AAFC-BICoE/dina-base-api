package ca.gc.aafc.dina.jpa.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;

import ca.gc.aafc.dina.jpa.filter.FilterHandler;
import ca.gc.aafc.dina.jpa.meta.JpaMetaInformationProvider;
import ca.gc.aafc.dina.jpa.repository.JpaDtoRepository.FindAllParams;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.registry.ResourceRegistryAware;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.RelationshipRepository;
import io.crnk.core.resource.list.ResourceList;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * JSONAPI repository that interfaces using DTOs, and uses JPA entities internally.
 *
 * @param <S> the source resource
 * @param <T> the target resource
 */
@Transactional
@RequiredArgsConstructor
//CHECKSTYLE:OFF AnnotationUseStyle
public class JpaRelationshipRepository<S, T>
    implements RelationshipRepository<S, Serializable, T, Serializable>, ResourceRegistryAware {

  @NonNull
  @Getter(onMethod_ = @Override)
  private final Class<S> sourceResourceClass;

  @NonNull
    
  @Getter(onMethod_ = @Override)
  private final Class<T> targetResourceClass;

  @NonNull
  private final JpaDtoRepository dtoRepository;

  @NonNull
  private final List<FilterHandler> filterHandlers;

  @Nullable
  private final JpaMetaInformationProvider metaInformationProvider;
  @Setter(onMethod_ = @Override)
  private ResourceRegistry resourceRegistry;

  @Override
  public void setRelation(S source, Serializable targetId, String fieldName) {
    this.dtoRepository.modifyRelation(
        this.findEntityFromDto(source),
        Collections.singletonList(targetId),
        fieldName,
        null,
        Collection::add,
        (targetEntity, oppositeFieldName, sourceEntity) -> PropertyUtils.setProperty(
            targetEntity,
            oppositeFieldName,
            sourceEntity
        ),
        this.resourceRegistry
    );
  }

  @Override
  public void setRelations(S source, Collection<Serializable> targetIds, String fieldName) {
    this.dtoRepository.modifyRelation(
        this.findEntityFromDto(source),
        targetIds,
        fieldName,
        (sourceCollection, targetEntities) -> {
          sourceCollection.clear();
          sourceCollection.addAll(targetEntities);
        },
        Collection::add,
        (targetEntity, oppositeFieldName, sourceEntity) -> PropertyUtils.setProperty(
            targetEntity,
            oppositeFieldName,
            sourceEntity
        ),
        this.resourceRegistry
    );
  }

  @Override
  public void addRelations(S source, Collection<Serializable> targetIds, String fieldName) {
    this.dtoRepository.modifyRelation(
        this.findEntityFromDto(source),
        targetIds,
        fieldName,
        Collection::addAll,
        Collection::add,
        (targetEntity, oppositeFieldName, sourceEntity) -> PropertyUtils.setProperty(
            targetEntity,
            oppositeFieldName,
            sourceEntity
        ),
        this.resourceRegistry
    );
  }

  @Override
  public void removeRelations(S source, Collection<Serializable> targetIds, String fieldName) {
    this.dtoRepository.modifyRelation(
        this.findEntityFromDto(source),
        targetIds,
        fieldName,
        Collection::removeAll,
        Collection::remove,
        (targetEntity, oppositeFieldName, sourceEntity) -> PropertyUtils.setProperty(
            targetEntity,
            oppositeFieldName,
            null
        ),
        this.resourceRegistry
    );
  }

  @Override
  public T findOneTarget(Serializable sourceId, String fieldName, QuerySpec targetQuerySpec) {
    // Use the findManyTargets method, but limit the result size to 1.
    targetQuerySpec.setLimit(Long.valueOf(1));
    ResourceList<T> resultSet = this.findManyTargets(sourceId, fieldName, targetQuerySpec);

    // Throw the 404 exception if the resource is not found.
    if (resultSet.size() == 0) {
      throw new ResourceNotFoundException("");
    }

    // There should only be one result element in the list.
    return resultSet.get(0);
  }

  @Override
  public ResourceList<T> findManyTargets(Serializable sourceId, String fieldName,
      QuerySpec querySpec) {

    // The source entity has the to-many relationship.
    Class<?> sourceEntityClass = dtoRepository.getDtoJpaMapper()
        .getEntityClassForDto(sourceResourceClass);

    // Wrapper array to hold reference to the source entity's JPA path. Lambda scoping prevents this
    // from being a regular variable.
    From<?, ?>[] sourcePathHolder = new From<?, ?>[1];

    @SuppressWarnings("unchecked")
    ResourceList<T> resultSet = (ResourceList<T>) dtoRepository.findAll(
        FindAllParams.builder()
            .sourceDtoClass(sourceResourceClass)
            .querySpec(querySpec)
            .resourceRegistry(resourceRegistry)
            .metaInformationProvider(metaInformationProvider)
            .customFilter((targetPath, query, cb) -> {
              From<?, ?> sourcePath = sourcePathHolder[0];

              List<Predicate> restrictions = new ArrayList<>();

              // Add the filter handler's restriction.
              for (FilterHandler filterHandler : this.filterHandlers) {
                restrictions.add(filterHandler.getRestriction(querySpec, targetPath, query, cb));
              }

              // Restrict the source entity to the given sourceId.
              restrictions.add(
                  cb.equal(
                      sourcePath.get(
                          dtoRepository.getEntityManager()
                              .getMetamodel()
                              .entity(sourceEntityClass)
                              .getId(Serializable.class)
                      ),
                      sourceId
                  )
              );

              // Combine predicates in an 'and' operation.
              return cb.and(restrictions.stream().toArray(Predicate[]::new));
            })
            .customRoot(sourcePath -> {
              // Get the reference to the source entity's path.
              sourcePathHolder[0] = sourcePath;
              // Create the Join to the target entity.
              return sourcePath.join(fieldName);
            })
            .build()
    );

    return resultSet;
  }

  private Object findEntityFromDto(Object dto) {
    return this.dtoRepository.getEntityManager().find(
        this.dtoRepository.getDtoJpaMapper()
            .getEntityClassForDto(dto.getClass()),
        this.resourceRegistry.findEntry(dto.getClass())
            .getResourceInformation()
            .getId(dto)
    );
  }
  
}
