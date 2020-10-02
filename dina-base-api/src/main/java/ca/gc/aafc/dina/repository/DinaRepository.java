package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.mapper.DerivedDtoField;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.meta.DinaMetaInfo;
import ca.gc.aafc.dina.repository.meta.ExternalResourceProvider;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaAuthorizationService;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.registry.ResourceRegistryAware;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.MetaRepository;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;
import io.crnk.core.resource.meta.MetaInformation;
import io.crnk.core.resource.meta.PagedMetaInformation;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  private final DinaMapper<D, E> dinaMapper;
  private final DinaFilterResolver filterResolver;

  private final Map<Class<?>, Set<String>> resourceFieldsPerClass;
  private final Map<Class<?>, Set<String>> entityFieldsPerClass;
  private final Map<String, String> externalMetaMap;

  private static final long DEFAULT_LIMIT = 100;

  @Getter
  @Setter(onMethod_ = @Override)
  private ResourceRegistry resourceRegistry;

  @Inject
  public DinaRepository(
    @NonNull DinaService<E> dinaService,
    @NonNull Optional<DinaAuthorizationService> authorizationService,
    @NonNull Optional<AuditService> auditService,
    @NonNull DinaMapper<D, E> dinaMapper,
    @NonNull Class<D> resourceClass,
    @NonNull Class<E> entityClass,
    @NonNull DinaFilterResolver filterResolver,
    ExternalResourceProvider externalResourceProvider
  ) {
    this.dinaService = dinaService;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.dinaMapper = dinaMapper;
    this.resourceClass = resourceClass;
    this.entityClass = entityClass;
    this.filterResolver = filterResolver;
    this.resourceFieldsPerClass =
      parseFieldsPerClass(resourceClass, new HashMap<>(), DinaRepository::isNotMappable);
    this.entityFieldsPerClass = getFieldsPerEntity();
    if (externalResourceProvider != null) {
      this.externalMetaMap =
        DinaMetaInfo.parseExternalTypes(resourceClass, externalResourceProvider);
    } else {
      this.externalMetaMap = null;
    }
  }

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

  @Override
  public ResourceList<D> findAll(QuerySpec querySpec) {
    return findAll(null, querySpec);
  }

  @Override
  public ResourceList<D> findAll(Collection<Serializable> ids, QuerySpec querySpec) {
    String idName = SelectionHandler.getIdAttribute(resourceClass, resourceRegistry);

    List<D> dList = mapToDto(querySpec, fetchEntities(ids, querySpec, idName));

    Long resourceCount = dinaService.getResourceCount(
      entityClass,
      (cb, root) -> filterResolver.buildPredicates(querySpec, cb, root, ids, idName));

    DefaultPagedMetaInformation metaInformation = new DefaultPagedMetaInformation();
    metaInformation.setTotalResourceCount(resourceCount);
    return new DefaultResourceList<>(dList, metaInformation, NO_LINK_INFORMATION);
  }

  private List<E> fetchEntities(Collection<Serializable> ids, QuerySpec querySpec, String idName) {
    List<IncludeRelationSpec> relationsToEagerLoad = querySpec.getIncludedRelations().stream()
      .filter(ir -> ir.getAttributePath().stream()
        .noneMatch(rel -> hasAnnotation(resourceClass, rel, JsonApiExternalRelation.class))
      ).collect(Collectors.toList());

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

  private List<D> mapToDto(QuerySpec querySpec, List<E> returnedEntities) {
    Set<String> relationsToMap = Stream.concat(
      querySpec.getIncludedRelations().stream().map(ir -> ir.getAttributePath().get(0)),
      FieldUtils.getFieldsListWithAnnotation(resourceClass, JsonApiExternalRelation.class)
        .stream().map(Field::getName)
    ).collect(Collectors.toSet());

    List<ResourceField> shallowRelationsToMap = findRelations(resourceClass).stream()
      .filter(rel -> relationsToMap.stream().noneMatch(rel.getUnderlyingName()::equalsIgnoreCase))
      .collect(Collectors.toList());

    return returnedEntities.stream()
      .map(e -> {
        D dto = dinaMapper.toDto(e, entityFieldsPerClass, relationsToMap);
        mapShallowRelations(e, dto, shallowRelationsToMap);
        return dto;
      })
      .collect(Collectors.toList());
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

    mapToEntity(resource, entity, findRelations(resourceClass));
    dinaService.update(entity);
    auditService.ifPresent(service -> service.audit(resource));
    return resource;
  }

  @Override
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public <S extends D> S create(S resource) {
    E entity = entityClass.getConstructor().newInstance();

    List<ResourceField> relationFields = findRelations(resourceClass);
    mapToEntity(resource, entity, relationFields);

    authorizationService.ifPresent(auth -> auth.authorizeCreate(entity));
    dinaService.create(entity);

    D dto = dinaMapper.toDto(entity, entityFieldsPerClass,
      relationFields.stream().map(ResourceField::getUnderlyingName).collect(Collectors.toSet()));
    auditService.ifPresent(service -> service.audit(dto));
    return (S) dto;
  }

  private <S extends D> void mapToEntity(S dto, E entity, List<ResourceField> relationFields) {
    Set<String> relationsToMap = relationFields.stream()
      .map(ResourceField::getUnderlyingName).collect(Collectors.toSet());
    dinaMapper.applyDtoToEntity(dto, entity, resourceFieldsPerClass, relationsToMap);

    List<ResourceField> relationsToLink = relationFields.stream()
      .filter(relation ->
        !hasAnnotation(resourceClass, relation.getUnderlyingName(), JsonApiExternalRelation.class))
      .collect(Collectors.toList());
    linkRelations(entity, relationsToLink);
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

    D dto = dinaMapper.toDto(entity, entityFieldsPerClass, Collections.emptySet());
    auditService.ifPresent(service -> service.auditDeleteEvent(dto));
  }

  @Override
  public MetaInformation getMetaInformation(
    Collection<D> collection, QuerySpec querySpec, MetaInformation metaInformation
  ) {
    DinaMetaInfo metaInfo = new DinaMetaInfo();
    // Set External types
    metaInfo.setExternalTypes(externalMetaMap);
    // Set resource counts
    if (metaInformation instanceof PagedMetaInformation) {
      PagedMetaInformation pagedMetaInformation = (PagedMetaInformation) metaInformation;
      if (pagedMetaInformation.getTotalResourceCount() != null) {
        metaInfo.setTotalResourceCount(pagedMetaInformation.getTotalResourceCount());
      }
    } else {
      metaInfo.setTotalResourceCount((long) collection.size());
    }
    return metaInfo;
  }

  /**
   * Returns a map of fields per entity class.
   *
   * @return a map of fields per entity class.
   */
  private Map<Class<?>, Set<String>> getFieldsPerEntity() {
    return resourceFieldsPerClass.entrySet()
      .stream()
      .filter(e -> getRelatedEntity(e.getKey()) != null)
      .map(e -> new SimpleEntry<>(getRelatedEntity(e.getKey()).value(), e.getValue()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Transverses a given class to return a map of fields per class parsed from the given class. Used
   * to determine the necessary classes and fields per class when mapping a java bean. Fields marked
   * with {@link JsonApiRelation} will be treated as separate classes to map and will be transversed
   * and mapped.
   *
   * @param <T>            - Type of class
   * @param clazz          - Class to parse
   * @param fieldsPerClass - initial map to use
   * @param ignoreIf       - predicate to return true for fields to be removed
   * @return a map of fields per class
   */
  @SneakyThrows
  private static <T> Map<Class<?>, Set<String>> parseFieldsPerClass(
    @NonNull Class<T> clazz,
    @NonNull Map<Class<?>, Set<String>> fieldsPerClass,
    @NonNull Predicate<Field> ignoreIf
  ) {
    if (fieldsPerClass.containsKey(clazz)) {
      return fieldsPerClass;
    }

    List<Field> relationFields = FieldUtils.getFieldsListWithAnnotation(
      clazz,
      JsonApiRelation.class
    );

    List<Field> attributeFields = FieldUtils.getAllFieldsList(clazz).stream()
      .filter(f -> !relationFields.contains(f) && !f.isSynthetic() && !ignoreIf.test(f))
      .collect(Collectors.toList());

    Set<String> fieldsToInclude = attributeFields.stream()
      .map(Field::getName)
      .collect(Collectors.toSet());

    fieldsPerClass.put(clazz, fieldsToInclude);

    parseRelations(clazz, fieldsPerClass, relationFields, ignoreIf);

    return fieldsPerClass;
  }

  /**
   * Helper method to parse the fields of a given list of relations and add them to a given map.
   *
   * @param <T>            - Type of class
   * @param clazz          - class containing the relations
   * @param fieldsPerClass - map to add to
   * @param relationFields - relation fields to transverse
   */
  @SneakyThrows
  private static <T> void parseRelations(
    Class<T> clazz,
    Map<Class<?>, Set<String>> fieldsPerClass,
    List<Field> relationFields,
    Predicate<Field> removeIf
  ) {
    for (Field relationField : relationFields) {
      if (Collection.class.isAssignableFrom(relationField.getType())) {
        ParameterizedType genericType = (ParameterizedType) clazz
          .getDeclaredField(relationField.getName())
          .getGenericType();
        for (Type elementType : genericType.getActualTypeArguments()) {
          parseFieldsPerClass((Class<?>) elementType, fieldsPerClass, removeIf);
        }
      } else {
        parseFieldsPerClass(relationField.getType(), fieldsPerClass, removeIf);
      }
    }
  }

  /**
   * Replaces the given relations of given entity with there JPA entity equivalent. Relations id's
   * are used to map a relation to its JPA equivalent.
   *
   * @param entity    - entity containing the relations
   * @param relations - list of relations to map
   */
  private void linkRelations(@NonNull E entity, @NonNull List<ResourceField> relations) {
    mapRelations(entity, entity, relations,
      (resourceField, relation) ->
        returnPersistedObject(findIdFieldName(resourceField.getElementType()), relation));
  }

  /**
   * Maps the given relations from the given entity to a given dto in a shallow form (Id only).
   *
   * @param entity         - source of the mapping
   * @param dto            - target of the mapping
   * @param relationsToMap - relations to map
   */
  private void mapShallowRelations(E entity, D dto, List<ResourceField> relationsToMap) {
    mapRelations(entity, dto, relationsToMap,
      (resourceField, relation) -> {
        Class<?> elementType = resourceField.getElementType();
        return createShallowDTO(findIdFieldName(elementType), elementType, relation);
      });
  }

  /**
   * Maps the given relations from a given source to a given target with a given mapping function.
   *
   * @param source    - source of the mapping
   * @param target    - target of the mapping
   * @param relations - relations to map
   * @param mapper    - mapping function to apply
   */
  private void mapRelations(
    Object source,
    Object target,
    List<ResourceField> relations,
    BiFunction<ResourceField, Object, Object> mapper
  ) {
    for (ResourceField relation : relations) {
      String fieldName = relation.getUnderlyingName();
      if (relation.isCollection()) {
        Collection<?> relationValue = (Collection<?>) PropertyUtils.getProperty(source, fieldName);
        if (relationValue != null) {
          Collection<?> mappedCollection = relationValue.stream()
            .map(rel -> mapper.apply(relation, rel))
            .collect(Collectors.toList());
          PropertyUtils.setProperty(target, fieldName, mappedCollection);
        }
      } else {
        Object relationValue = PropertyUtils.getProperty(source, fieldName);
        if (relationValue != null) {
          Object mappedRelation = mapper.apply(relation, relationValue);
          PropertyUtils.setProperty(target, fieldName, mappedRelation);
        }
      }
    }
  }

  /**
   * Returns the jpa entity representing a given object with an id field of a given id field name.
   *
   * @param idFieldName - name of the id field
   * @param object      - object to map
   * @return - jpa entity representing a given object
   */
  private Object returnPersistedObject(String idFieldName, Object object) {
    Object relationID = PropertyUtils.getProperty(object, idFieldName);
    return dinaService.findOneReferenceByNaturalId(object.getClass(), relationID);
  }

  /**
   * Returns true if the dina repo should not map the given field. currently that means if the field
   * is generated (Marked with {@link DerivedDtoField}) or final.
   *
   * @param field - field to evaluate
   * @return - true if the dina repo should not map the given field
   */
  private static boolean isNotMappable(Field field) {
    return hasAnnotation(field.getDeclaringClass(), field.getName(), DerivedDtoField.class)
           || Modifier.isFinal(field.getModifiers());
  }

  /**
   * Returns true if the given class has a given field with an annotation of a given type.
   *
   * @param <T>             - Class type
   * @param clazz           - class of the field
   * @param field           - field to check
   * @param annotationClass - annotation that is present
   * @return true if a dto field is generated and read-only
   */
  @SneakyThrows(NoSuchFieldException.class)
  private static <T> boolean hasAnnotation(
    Class<T> clazz,
    String field,
    Class<? extends Annotation> annotationClass
  ) {
    return clazz.getDeclaredField(field).isAnnotationPresent(annotationClass);
  }

  /**
   * Returns a Dto's related entity (Marked with {@link RelatedEntity}) or else null.
   *
   * @param <T>   - Class type
   * @param clazz - Class with a related entity.
   * @return a Dto's related entity, or else null
   */
  private static <T> RelatedEntity getRelatedEntity(Class<T> clazz) {
    return clazz.getAnnotation(RelatedEntity.class);
  }

  /**
   * Maps the given id field name from a given entity to a new instance of a given type.
   *
   * @param idFieldName - name of the id field for the mapping
   * @param type        - type of new instance to return with the mapping
   * @param entity      - entity with the id to map
   * @return - a new instance of a given type with a id value mapped from a given entity.
   */
  @SneakyThrows
  private static Object createShallowDTO(String idFieldName, Class<?> type, Object entity) {
    Object shallowDTO = type.getConstructor().newInstance();
    PropertyUtils.setProperty(
      shallowDTO,
      idFieldName,
      PropertyUtils.getProperty(entity, idFieldName));
    return shallowDTO;
  }

  /**
   * Returns a list of resource fields for a given class.
   *
   * @param clazz - class of relations to find
   * @return - list of resource fields
   */
  private List<ResourceField> findRelations(Class<?> clazz) {
    return this.resourceRegistry.findEntry(clazz).getResourceInformation().getRelationshipFields();
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
