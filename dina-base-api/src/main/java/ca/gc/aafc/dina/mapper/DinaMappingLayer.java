package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DinaMappingLayer<D, E> {

  private final Map<Class<?>, Set<String>> resourceFieldsPerClass;
  private final Map<Class<?>, Set<String>> entityFieldsPerClass;
  private final Map<String, String> externalNameToTypeMap;
  private final Map<String, Class<?>> relationNamePerClass;
  private final Set<String> collectionBasedRelations;
  private final DinaMapper<D, E> dinaMapper;
  private final DinaService<?> dinaService;

  public DinaMappingLayer(
    @NonNull Class<D> resourceClass,
    @NonNull DinaService<?> dinaService,
    @NonNull DinaMapper<D, E> dinaMapper
  ) {
    this.resourceFieldsPerClass =
      parseFieldsPerClass(resourceClass, new HashMap<>(), DinaMappingLayer::isNotMappable);
    this.entityFieldsPerClass = getFieldsPerEntity();
    this.dinaService = dinaService;
    this.dinaMapper = dinaMapper;
    this.relationNamePerClass = FieldUtils
      .getFieldsListWithAnnotation(resourceClass, JsonApiRelation.class).stream()
      .filter(field -> !field.isAnnotationPresent(JsonApiExternalRelation.class))
      .collect(Collectors.toMap(
        Field::getName,
        field -> DinaMapper.isCollection(field.getType()) ?
          DinaMapper.getGenericType(field.getDeclaringClass(), field.getName()) : field.getType()));
    this.collectionBasedRelations = FieldUtils
      .getFieldsListWithAnnotation(resourceClass, JsonApiRelation.class)
      .stream()
      .filter(field -> DinaMapper.isCollection(field.getType()))
      .map(Field::getName).collect(Collectors.toSet());
    this.externalNameToTypeMap = FieldUtils
      .getFieldsListWithAnnotation(resourceClass, JsonApiExternalRelation.class)
      .stream().collect(Collectors.toMap(
        Field::getName,
        field -> field.getAnnotation(JsonApiExternalRelation.class).type()));
  }

  public List<D> mapEntitiesToDto(@NonNull QuerySpec query, @NonNull List<E> entities) {
    Set<String> relationsToMap = query.getIncludedRelations().stream()
      .map(ir -> ir.getAttributePath().get(0))
      .filter(this::isNotExternal).collect(Collectors.toSet());

    Map<String, Class<?>> shallowRelationsToMap = relationNamePerClass.entrySet().stream()
      .filter(relation -> relationsToMap.stream().noneMatch(relation.getKey()::equalsIgnoreCase))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return entities.stream()
      .map(e -> {
        D dto = dinaMapper.toDto(e, entityFieldsPerClass, relationsToMap);
        mapShallowRelations(e, dto, shallowRelationsToMap);
        mapExternalRelationsToDto(e, dto);
        return dto;
      })
      .collect(Collectors.toList());
  }

  public <S extends D> void mapToEntity(@NonNull S dto, @NonNull E entity) {
    dinaMapper.applyDtoToEntity(dto, entity, resourceFieldsPerClass, relationNamePerClass.keySet());
    linkRelations(entity, relationNamePerClass);
    mapExternalRelationsToEntity(dto, entity);
  }

  public D mapForDelete(@NonNull E entity) {
    return dinaMapper.toDto(entity, entityFieldsPerClass, Collections.emptySet());
  }

  private void mapExternalRelationsToDto(E source, D target) {
    externalNameToTypeMap.keySet().forEach(external -> {
      String id = PropertyUtils.getProperty(source, external).toString();
      PropertyUtils.setProperty(target, external,
        ExternalRelationDto.builder().type(externalNameToTypeMap.get(external)).id(id).build());
    });
  }

  private void mapExternalRelationsToEntity(D source, E target) {
    externalNameToTypeMap.keySet().forEach(external -> {
      Object externalRelation = PropertyUtils.getProperty(source, external);
      PropertyUtils.setProperty(target, external,
        UUID.fromString(PropertyUtils.getProperty(externalRelation, "id").toString()));
    });
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
   * Returns a map of fields per entity class.
   *
   * @return a map of fields per entity class.
   */
  private Map<Class<?>, Set<String>> getFieldsPerEntity() {
    return resourceFieldsPerClass.entrySet()
      .stream()
      .filter(e -> getRelatedEntity(e.getKey()) != null)
      .map(e -> new AbstractMap.SimpleEntry<>(getRelatedEntity(e.getKey()).value(), e.getValue()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
   * Returns true if the dina repo should not map the given field. currently that means if the field
   * is generated (Marked with {@link DerivedDtoField}) or final.
   *
   * @param field - field to evaluate
   * @return - true if the dina repo should not map the given field
   */
  private static boolean isNotMappable(Field field) {
    return field.isAnnotationPresent(DerivedDtoField.class) ||
           Modifier.isFinal(field.getModifiers());
  }

  /**
   * Maps the given relations from the given entity to a given dto in a shallow form (Id only).
   *
   * @param entity    - source of the mapping
   * @param dto       - target of the mapping
   * @param relations - relations to map
   */
  private void mapShallowRelations(E entity, D dto, @NonNull Map<String, Class<?>> relations) {
    mapRelations(entity, dto, relations,
      (aClass, relation) -> createShallowDTO(findIdFieldName(aClass), aClass, relation));
  }

  /**
   * Replaces the given relations of given entity with there JPA entity equivalent. Relations id's
   * are used to map a relation to its JPA equivalent.
   *
   * @param entity    - entity containing the relations
   * @param relations - list of relations to map
   */
  private void linkRelations(@NonNull E entity, @NonNull Map<String, Class<?>> relations) {
    mapRelations(entity, entity, relations,
      (aClass, relation) -> returnPersistedObject(findIdFieldName(aClass), relation));
  }

  /**
   * Maps the given relations from a given source to a given target with a given mapping function.
   *
   * @param source - source of the mapping
   * @param target - target of the mapping
   * @param mapper - mapping function to apply
   */
  private void mapRelations(
    Object source,
    Object target,
    Map<String, Class<?>> relations,
    BiFunction<Class<?>, Object, Object> mapper
  ) {
    for (Map.Entry<String, Class<?>> relation : relations.entrySet()) {
      String relationName = relation.getKey();
      Class<?> relationType = relation.getValue();
      if (collectionBasedRelations.contains(relationName)) {
        Collection<?> relationValue = (Collection<?>) PropertyUtils.getProperty(
          source, relationName);
        if (relationValue != null) {
          Collection<?> mappedCollection = relationValue.stream()
            .map(rel -> mapper.apply(relationType, rel)).collect(Collectors.toList());
          PropertyUtils.setProperty(target, relationName, mappedCollection);
        }
      } else {
        Object relationValue = PropertyUtils.getProperty(source, relationName);
        if (relationValue != null) {
          Object mappedRelation = mapper.apply(relationType, relationValue);
          PropertyUtils.setProperty(target, relationName, mappedRelation);
        }
      }
    }
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
   * Returns the id field name for a given class.
   *
   * @param clazz - class to find the id field name for
   * @return - id field name for a given class.
   */
  private String findIdFieldName(Class<?> clazz) {
    return Arrays.stream(FieldUtils.getFieldsWithAnnotation(clazz, JsonApiId.class))
      .findAny().orElseThrow(() -> new IllegalArgumentException(""))
      .getName();
  }

  private boolean isNotExternal(String field) {
    return externalNameToTypeMap.keySet().stream().noneMatch(field::equalsIgnoreCase);
  }

}
