package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.mapper.DerivedDtoField;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaAuthorizationService;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.registry.ResourceRegistryAware;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.core.resource.annotations.JsonApiRelation;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.Serializable;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * JSONAPI repository that interfaces using DTOs, and uses JPA entities
 * internally. Sparse fields sets are handled by the underlying Crnk
 * ResourceRepository.
 *
 * @param <D>
 *              - Dto type
 * @param <E>
 *              - Entity type
 */
@Transactional
public class DinaRepository<D, E extends DinaEntity>
  implements ResourceRepository<D, Serializable>, ResourceRegistryAware {

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

  private static final int DEFAULT_OFFSET = 0;
  private static final int DEFAULT_LIMIT = 100;

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
    @NonNull DinaFilterResolver filterResolver
  ) {
    this.dinaService = dinaService;
    this.authorizationService = authorizationService;
    this.auditService = auditService;
    this.dinaMapper = dinaMapper;
    this.resourceClass = resourceClass;
    this.entityClass = entityClass;
    this.filterResolver = filterResolver;
    this.resourceFieldsPerClass = parseFieldsPerClass(
      resourceClass,
      new HashMap<>(),
      DinaRepository::isNotMappable);
    this.entityFieldsPerClass = getFieldsPerEntity();
  }

  @Override
  public D findOne(Serializable id, QuerySpec querySpec) {
    E entity = dinaService.findOne(id, entityClass);

    if (entity == null) {
      throw new ResourceNotFoundException(
          resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }

    Set<String> includedRelations = querySpec.getIncludedRelations()
      .stream()
      .map(ir -> ir.getAttributePath().get(0))
      .collect(Collectors.toSet());

    D dto = dinaMapper.toDto(entity, entityFieldsPerClass, includedRelations);
    mapShallowRelations(entity, dto, includedRelations);
    return dto;
  }

  private void mapShallowRelations(E entity, D dto, Set<String> excluded) {
    ResourceInformation resourceInformation = this.resourceRegistry
      .findEntry(resourceClass)
      .getResourceInformation();

    List<ResourceField> relationsToMap = resourceInformation.getRelationshipFields()
      .stream()
      .filter(resourceField -> excluded.stream()
        .noneMatch(resourceField.getUnderlyingName()::equalsIgnoreCase))
      .collect(Collectors.toList());

    for (ResourceField relation : relationsToMap) {
      String fieldName = relation.getUnderlyingName();
      String relationIdFieldName = resourceInformation.getIdField().getUnderlyingName();
      Class<?> elementType = relation.getElementType();

      if (relation.isCollection()) {
        Collection<?> relationValue = (Collection<?>) PropertyUtils.getProperty(entity, fieldName);
        if (relationValue != null) {
          Collection<?> mappedCollection = relationValue.stream()
            .map(rel -> createShallowDTO(relationIdFieldName, elementType, rel))
            .collect(Collectors.toList());
          PropertyUtils.setProperty(dto, fieldName, mappedCollection);
        }
      } else {
        Object relationValue = PropertyUtils.getProperty(entity, fieldName);
        if (relationValue != null) {
          Object shallowDTO = createShallowDTO(relationIdFieldName, elementType, relationValue);
          PropertyUtils.setProperty(dto, fieldName, shallowDTO);
        }
      }
    }
  }

  @SneakyThrows
  private static Object createShallowDTO(String idFieldName, Class<?> type, Object entity) {
    Object shallowDTO = type.getConstructor().newInstance();
    PropertyUtils.setProperty(
      shallowDTO,
      idFieldName,
      PropertyUtils.getProperty(entity, idFieldName));
    return shallowDTO;
  }

  @Override
  public ResourceList<D> findAll(QuerySpec querySpec) {
    return findAll(null, querySpec);
  }

  @Override
  public ResourceList<D> findAll(Collection<Serializable> ids, QuerySpec querySpec) {

    String idName = SelectionHandler.getIdAttribute(resourceClass, resourceRegistry);

    List<E> returnedEntities = dinaService.findAll(
      entityClass,
      (cb, root) -> filterResolver.buildPredicates(querySpec, cb, root, ids, idName),
      (cb, root) -> DinaFilterResolver.getOrders(querySpec, cb, root),
      Optional.ofNullable(querySpec.getOffset()).orElse(Long.valueOf(DEFAULT_OFFSET)).intValue(),
      Optional.ofNullable(querySpec.getLimit()).orElse(Long.valueOf(DEFAULT_LIMIT)).intValue());

    Set<String> includedRelations = querySpec.getIncludedRelations()
      .stream()
      .map(ir-> ir.getAttributePath().get(0))
      .collect(Collectors.toSet());

    List<D> dtos = returnedEntities.stream()
      .map(e -> dinaMapper.toDto(e, entityFieldsPerClass, includedRelations))
      .collect(Collectors.toList());

    Long resourceCount = dinaService.getResourceCount(
      entityClass,
      (cb, root) -> filterResolver.buildPredicates(querySpec, cb, root, ids, idName));

    DefaultPagedMetaInformation metaInformation = new DefaultPagedMetaInformation();
    metaInformation.setTotalResourceCount(resourceCount);
    return new DefaultResourceList<>(dtos, metaInformation, NO_LINK_INFORMATION);
  }

  @Override
  public <S extends D> S save(S resource) {
    ResourceInformation resourceInformation = this.resourceRegistry
      .findEntry(resourceClass)
      .getResourceInformation();

    String idFieldName = resourceInformation.getIdField().getUnderlyingName();
    Object id = PropertyUtils.getProperty(resource, idFieldName);

    E entity = dinaService.findOne(id, entityClass);
    authorizationService.ifPresent(auth -> auth.authorizeUpdate(entity));

    if (entity == null) {
      throw new ResourceNotFoundException(
          resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }

    Set<String> relations = resourceInformation.getRelationshipFields()
      .stream()
      .map(ResourceField::getUnderlyingName)
      .collect(Collectors.toSet());

    dinaMapper.applyDtoToEntity(resource, entity, resourceFieldsPerClass, relations);
    linkRelations(entity, resourceInformation.getRelationshipFields());

    dinaService.update(entity);
    auditService.ifPresent(service -> service.audit(resource));

    return resource;
  }

  @Override
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public <S extends D> S create(S resource) {
    E entity = entityClass.getConstructor().newInstance();

    ResourceInformation resourceInformation = this.resourceRegistry
      .findEntry(resourceClass)
      .getResourceInformation();

    Set<String> relations = resourceInformation.getRelationshipFields()
      .stream()
      .map(ResourceField::getUnderlyingName)
      .collect(Collectors.toSet());

    dinaMapper.applyDtoToEntity(resource, entity, resourceFieldsPerClass, relations);

    linkRelations(entity, resourceInformation.getRelationshipFields());

    authorizationService.ifPresent(auth -> auth.authorizeCreate(entity));
    dinaService.create(entity);

    D dto = dinaMapper.toDto(entity, entityFieldsPerClass, relations);
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

    D dto = dinaMapper.toDto(entity, entityFieldsPerClass, Collections.emptySet());
    auditService.ifPresent(service -> service.auditDeleteEvent(dto));
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
   * Transverses a given class to return a map of fields per class parsed from the
   * given class. Used to determine the nessesasry classes and fields per class
   * when mapping a java bean. Fields marked with {@link JsonApiRelation} will be
   * teated as seperate classes to map and will be transversed and mapped.
   *
   * @param <T>
   *                         - Type of class
   * @param clazz
   *                         - Class to parse
   * @param fieldsPerClass
   *                         - initial map to use
   * @param ignoreIf
   *                         - predicate to return true for fields to be removed
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
   * Helper method to parse the fields of a given list of relations and add them
   * to a given map.
   *
   * @param <T>
   *                         - Type of class
   * @param clazz
   *                         - class containing the relations
   * @param fieldsPerClass
   *                         - map to add to
   * @param relationFields
   *                         - relation fields to transverse
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
   * Replaces the given relations of given entity with there JPA entity
   * equivalent. Relations id's are used to map a relation to its JPA equivalent.
   *
   * @param entity
   *                    - entity containing the relations
   * @param relations
   *                    - list of relations to map
   */
  private void linkRelations(@NonNull E entity, @NonNull List<ResourceField> relations) {
    for (ResourceField relationField : relations) {
      ResourceInformation relationInfo = this.resourceRegistry
        .findEntry(relationField.getElementType())
        .getResourceInformation();

      String fieldName = relationField.getUnderlyingName();
      String idFieldName = relationInfo.getIdField().getUnderlyingName();

      if (relationField.isCollection()) {
        Collection<?> relation = (Collection<?>) PropertyUtils.getProperty(entity, fieldName);
        if (relation != null) {
          Collection<?> mappedCollection = relation.stream()
              .map(rel -> returnPersistedObject(idFieldName, rel))
              .collect(Collectors.toList());
          PropertyUtils.setProperty(entity, fieldName, mappedCollection);
        }
      } else {
        Object relation = PropertyUtils.getProperty(entity, fieldName);
        if (relation != null) {
          Object persistedRelationObject = returnPersistedObject(idFieldName, relation);
          PropertyUtils.setProperty(entity, fieldName, persistedRelationObject);
        }
      }
    }
  }

  /**
   * Returns the jpa entity representing a given object with an id field of a
   * given id field name.
   *
   * @param idFieldName
   *                      - name of the id field
   * @param object
   *                      - object to map
   * @return - jpa entity representing a given object
   */
  private Object returnPersistedObject(String idFieldName, Object object) {
    Object relationID = PropertyUtils.getProperty(object, idFieldName);
    return dinaService.findOneReferenceByNaturalId(object.getClass(), relationID);
  }

  /**
   * Returns true if the dina repo should not map the given field. currently that
   * means if the field is generated (Marked with {@link DerivedDtoField}) or final.
   *
   * @param field - field to evaluate
   * @return - true if the dina repo should not map the given field
   */
  private static boolean isNotMappable(Field field) {
    return isGenerated(field.getDeclaringClass(), field.getName())
      || Modifier.isFinal(field.getModifiers());
  }

  /**
   * Returns true if a dto field is generated and read-only (Marked with
   * {@link DerivedDtoField}).
   *
   * @param <T>
   *                - Class type
   * @param clazz
   *                - class of the field
   * @param field
   *                - field to check
   * @return true if a dto field is generated and read-only
   */
  @SneakyThrows(NoSuchFieldException.class)
  private static <T> boolean isGenerated(Class<T> clazz, String field) {
    return clazz.getDeclaredField(field).isAnnotationPresent(DerivedDtoField.class);
  }

  /**
   * Returns a Dto's related entity (Marked with {@link RelatedEntity}) or else
   * null.
   *
   * @param <T>
   *                - Class type
   * @param clazz
   *                - Class with a related entity.
   * @return a Dto's related entity, or else null
   */
  private static <T> RelatedEntity getRelatedEntity(Class<T> clazz) {
    return clazz.getAnnotation(RelatedEntity.class);
  }

}
