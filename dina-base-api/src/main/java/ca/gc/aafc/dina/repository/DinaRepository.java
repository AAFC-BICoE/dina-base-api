package ca.gc.aafc.dina.repository;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.DinaService;
import ca.gc.aafc.dina.mapper.DerivedDtoField;
import ca.gc.aafc.dina.mapper.DinaMapper;
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
import io.crnk.data.jpa.query.criteria.JpaCriteriaQuery;
import io.crnk.data.jpa.query.criteria.JpaCriteriaQueryFactory;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * JSONAPI repository that interfaces using DTOs, and uses JPA entities
 * internally.
 * 
 * @param <D>
 *              - Dto type
 * @param <E>
 *              - Entity type
 */
@Transactional
public class DinaRepository<D, E extends DinaEntity>
    implements ResourceRepository<D, Serializable>, ResourceRegistryAware {

  @NonNull
  private final DinaService<E> dinaService;

  @NonNull
  private final DinaMapper<D, E> dinaMapper;

  @Getter
  @NonNull
  private final Class<D> resourceClass;

  @NonNull
  private final Class<E> entityClass;

  private Map<Class<?>, Set<String>> resourceFieldsPerClass;

  private Map<Class<?>, Set<String>> entityFieldsPerClass;

  @Getter
  @Setter(onMethod_ = @Override)
  private ResourceRegistry resourceRegistry;

  private JpaCriteriaQueryFactory queryFactory;

  @Inject
  public DinaRepository(
    @NonNull DinaService<E> dinaService,
    @NonNull DinaMapper<D, E> dinaMapper,
    @NonNull Class<D> resourceClass,
    @NonNull Class<E> entityClass
  ) {
    this.dinaService = dinaService;
    this.dinaMapper = dinaMapper;
    this.resourceClass = resourceClass;
    this.entityClass = entityClass;

    this.resourceFieldsPerClass = parseFieldsPerClass(resourceClass, new HashMap<>());
    this.resourceFieldsPerClass.forEach(
      (clazz, fields) -> fields.removeIf(f -> isGenerated(clazz, f))
    );

    this.entityFieldsPerClass = getFieldsPerEntity();
  }

  @PostConstruct
  void setup() {
    queryFactory = dinaService.createJpaCritFactory();
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

    return dinaMapper.toDto(entity, entityFieldsPerClass, includedRelations);
  }

  @Override
  public ResourceList<D> findAll(QuerySpec querySpec) {
    return findAll(null, querySpec);
  }

  @Override
  public ResourceList<D> findAll(Collection<Serializable> ids, QuerySpec querySpec) {
    Set<String> includedRelations = querySpec.getIncludedRelations()
      .stream()
      .map(ir-> ir.getAttributePath().get(0))
      .collect(Collectors.toSet());

    JpaCriteriaQuery<E> query = queryFactory.query(entityClass);

    List<D> dtos = query.buildExecutor(querySpec)
      .getResultList()
      .stream()
      .map(e -> dinaMapper.toDto(e, entityFieldsPerClass, includedRelations))
      .collect(Collectors.toList());

    if (CollectionUtils.isNotEmpty(ids)) {
      String idFieldName = this.resourceRegistry
        .findEntry(resourceClass)
        .getResourceInformation()
        .getIdField()
        .getUnderlyingName();
      dtos = dtos.stream()
        .filter(dto -> ids.contains(PropertyUtils.getProperty(dto, idFieldName)))
        .collect(Collectors.toList());
    }

    return new DefaultResourceList<>(dtos, null, null);
  }

  @Override
  public <S extends D> S save(S resource) {
    ResourceInformation resourceInformation = this.resourceRegistry
      .findEntry(resourceClass)
      .getResourceInformation();

    String idFieldName = resourceInformation.getIdField().getUnderlyingName();
    Object id = PropertyUtils.getProperty(resource, idFieldName);

    E entity = dinaService.findOne(id, entityClass);

    if (entity == null) {
      throw new ResourceNotFoundException(
          resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }

    Set<String> relations = resourceInformation.getRelationshipFields()
      .stream()
      .map(rf -> rf.getUnderlyingName())
      .collect(Collectors.toSet());

    dinaMapper.applyDtoToEntity(resource, entity, resourceFieldsPerClass, relations);

    linkRelations(entity, resourceInformation.getRelationshipFields());

    dinaService.update(entity);

    return resource;
  }

  @Override
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public <S extends D> S create(S resource) {
    E entity = entityClass.newInstance();

    ResourceInformation resourceInformation = this.resourceRegistry
      .findEntry(resourceClass)
      .getResourceInformation();

    Set<String> relations = resourceInformation.getRelationshipFields()
      .stream()
      .map(af -> af.getUnderlyingName())
      .collect(Collectors.toSet());

    dinaMapper.applyDtoToEntity(resource, entity, resourceFieldsPerClass, relations);

    linkRelations(entity, resourceInformation.getRelationshipFields());

    dinaService.create(entity);

    return (S) dinaMapper.toDto(entity, entityFieldsPerClass, relations);
  }

  @Override
  public void delete(Serializable id) {
    E entity = dinaService.findOne(id, entityClass);
    if (entity == null) {
      throw new ResourceNotFoundException(
          resourceClass.getSimpleName() + " with ID " + id + " Not Found.");
    }
    dinaService.delete(entity);
  }

  /**
   * Returns a map of fields per entity class.
   *
   * @return a map of fields per entity class.
   */
  private Map<Class<?>, Set<String>> getFieldsPerEntity() {
    return resourceFieldsPerClass.entrySet()
      .stream()
      .map(e -> new SimpleEntry<>(getRelatedEntity(e.getKey()), e.getValue()))
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
   * @return a map of fields per class
   */
  @SneakyThrows
  private static <T> Map<Class<?>, Set<String>> parseFieldsPerClass(
    Class<T> clazz,
    Map<Class<?>, Set<String>> fieldsPerClass
  ) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(fieldsPerClass);

    if (fieldsPerClass.containsKey(clazz)) {
      return fieldsPerClass;
    }

    List<Field> relationFields = FieldUtils.getFieldsListWithAnnotation(
      clazz,
      JsonApiRelation.class
    );

    Set<String> attributeFields = FieldUtils.getAllFieldsList(clazz).stream()
      .filter(f -> !relationFields.contains(f) && !f.isSynthetic())
      .map(af -> af.getName())
      .collect(Collectors.toSet());

    fieldsPerClass.put(clazz, attributeFields);

    parseRelations(clazz, fieldsPerClass, relationFields);

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
    List<Field> relationFields
  ) {
    for (Field relationField : relationFields) {
      if (Collection.class.isAssignableFrom(relationField.getType())) {
        ParameterizedType genericType = (ParameterizedType) clazz
          .getDeclaredField(relationField.getName())
          .getGenericType();
        for (Type elementType : genericType.getActualTypeArguments()) {
          parseFieldsPerClass((Class<?>) elementType, fieldsPerClass);
        }
      } else {
        parseFieldsPerClass(relationField.getType(), fieldsPerClass);
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
  private void linkRelations(E entity, List<ResourceField> relations) {
    Objects.requireNonNull(entity);
    Objects.requireNonNull(relations);

    for (ResourceField relationField : relations) {
      ResourceInformation relationInfo =this.resourceRegistry
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
    return dinaService.findOne(relationID, object.getClass());
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
  private static <T> Class<?> getRelatedEntity(Class<T> clazz){
    return clazz.getAnnotation(RelatedEntity.class).value();
  }

}
