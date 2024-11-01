package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.repository.meta.JsonApiExternalRelation;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiRelation;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry to track information regarding a given resource class. Useful to obtain certain meta information
 * regarding the domain of resource.
 */
// CHECKSTYLE:OFF NoFinalizer
// CHECKSTYLE:OFF SuperFinalize
public class DinaMappingRegistry {

  @Getter
  private final Map<Class<?>, Set<String>> attributesPerClass;
  // Set of entries tracked by class for faster lookup.
  private final Map<Class<?>, DinaResourceEntry> resourceGraph;

  /**
   * <p>The given resource class will have its graph traversed and registered into the registry. All
   * relations will be considered a node of the graph and traversed accordingly. </p>
   *
   * <p>Parsing a given resource graph requires the use of reflection. A DinaMappingRegistry should not be
   * constructed in a repetitive manner where performance is needed.</p>
   *
   * <br>
   * <h2>Concepts</h2>
   *
   * <list>
   * <li>A relation is a field that is marked as a {@link JsonApiRelation}</li>
   * <li>A relation is considered internal unless marked with {@link JsonApiExternalRelation}</li>
   * <li>An attribute is a field that is not {@link IgnoreDinaMapping} or marked as a relation and will be
   * mapped directly as a value.</li>
   * <li>An attribute must have the same data type on the DTO class and its related entity</li>
   * <li>A field that is considered an attribute but with a different data type will throw an {@link
   * IllegalStateException}, unless the data type of the field is a valid DTO/RelatedEntity mapping between
   * the classes</li>
   * </list>
   *
   * @param resourceClass - resource traverse and register
   */
  public DinaMappingRegistry(@NonNull Class<?> resourceClass) {
    resourceGraph = initGraph(resourceClass, new HashSet<>());
    this.attributesPerClass = this.resourceGraph.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getAttributeNames()));
  }

  private static Map<Class<?>, DinaResourceEntry> initGraph(Class<?> resourceClass, Set<Class<?>> visited) {
    HashMap<Class<?>, DinaResourceEntry> graph = new HashMap<>();

    if (visited.contains(resourceClass)) {
      return graph;
    }
    visited.add(resourceClass);

    if (resourceClass.getAnnotation(RelatedEntity.class) != null) {
      Class<?> entityClass = resourceClass.getAnnotation(RelatedEntity.class).value();
      DinaResourceEntry entry = parseRegistryEntry(entityClass, resourceClass, visited, graph);
      graph.put(resourceClass, entry);
      graph.put(entityClass, entry);
    }
    return graph;
  }

  private static DinaResourceEntry parseRegistryEntry(
    Class<?> entityClass, Class<?> resourceClass,
    Set<Class<?>> visited, Map<Class<?>, DinaResourceEntry> graph
  ) {
    Set<String> attributes = new HashSet<>();
    Set<InternalRelation> internalRelations = new HashSet<>();

    for (Field dtoField : getAllFields(resourceClass)) {
      if (isMappableRelation(resourceClass, entityClass, dtoField)) {
        // If relation register and traverse graph
        internalRelations.add(mapToInternalRelation(dtoField));
        graph.putAll(initGraph(parseGenericTypeForField(dtoField), visited));
      } else if (isFieldConsideredAnAttribute(resourceClass, entityClass, dtoField)) {
        if (fieldHasSameDataType(entityClass, dtoField)) {
          // Un marked dtoField with same data type considered attribute
          attributes.add(dtoField.getName());
        } else {
          // Un marked dtoField without the same data type but have a related entity are considered a hidden relation
          if (!parseGenericTypeForField(dtoField).isAnnotationPresent(RelatedEntity.class)) {
            throwDataTypeMismatchException(resourceClass, entityClass, dtoField.getName());
          } else {
            internalRelations.add(mapToInternalRelation(dtoField));
            graph.putAll(initGraph(parseGenericTypeForField(dtoField), visited));
          }
        }
      }
    }
    return buildResourceEntry(resourceClass, entityClass, attributes, internalRelations);
  }

  /**
   * Returns a set of the mappable relations for a given class.
   *
   * @param cls - class with relations
   * @return Returns a set of the mappable relations for a given class.
   * @throws IllegalArgumentException if the class is not tracked by the registry
   */
  public Set<InternalRelation> findMappableRelationsForClass(Class<?> cls) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getInternalRelations();
  }

  /**
   * Return the class (dto class) of the internal relation represented by
   * the attribute.
   * @param cls (dto class)
   * @param attribute
   * @return
   */
  public Class<?> getInternalRelationClass(Class<?> cls, String attribute) {
    checkClassTracked(cls);

    return resourceGraph.get(cls).getInternalRelations().stream()
      .filter( i -> i.name.equalsIgnoreCase(attribute))
      .map(InternalRelation::getDtoType)
      .findAny()
      .orElse(null);
  }

  /**
   * Returns the set of external relation field names tracked by the registry.
   *
   * @return set of external relation field names.
   */
  public Set<String> getExternalRelations(Class<?> cls) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getExternalNameToTypeMap().keySet();
  }

  /**
   * Returns the {@link JsonApiExternalRelation} type of the given external relation field name if tracked by
   * the registry.
   *
   * @param relationFieldName - field name of the external relation.
   * @return type of the given external relation.
   * @throws IllegalArgumentException if the relationFieldName is not tracked by the registry
   */
  public String findExternalType(Class<?> cls, String relationFieldName) {
    checkClassTracked(cls);
    if (!this.resourceGraph.get(cls).getExternalNameToTypeMap().containsKey(relationFieldName)) {
      throw new IllegalArgumentException(
        "external relation with name: " + relationFieldName + " is not tracked by the registry");
    }
    return this.resourceGraph.get(cls).getExternalNameToTypeMap().get(relationFieldName);
  }

  /**
   * Returns true if the relation with the given field name is external.
   *
   * @param relationFieldName - field name of the external relation.
   * @return Returns true if the relation with the given field name is external.
   */
  public boolean isRelationExternal(Class<?> cls, String relationFieldName) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getExternalNameToTypeMap().keySet().stream()
      .anyMatch(relationFieldName::equalsIgnoreCase);
  }

  public boolean isInternalRelationship(Class<?> cls, String relationFieldName) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getInternalRelationByName(relationFieldName) != null;
  }

  /**
   * Returns the json id field name of a given class.
   *
   * @param cls - cls with json id field
   * @return the json id field name of a given class.
   * @throws IllegalArgumentException if the class is not tracked by the registry
   */
  public String findJsonIdFieldName(Class<?> cls) {
    checkClassTracked(cls);
    return this.resourceGraph.get(cls).getJsonIdFieldName();
  }

  /**
   * Returns the Dina field adapter for a given class or optional empty if it does not exist.
   *
   * @param cls - class of the {@link DinaFieldAdapterHandler}
   * @return the {@link DinaFieldAdapterHandler} for a given class
   */
  public Optional<DinaFieldAdapterHandler<?>> findFieldAdapterForClass(Class<?> cls) {
    if (!this.resourceGraph.containsKey(cls)) {
      return Optional.empty();
    }
    return Optional.ofNullable(this.resourceGraph.get(cls).getFieldAdapterHandler());
  }

  /**
   * Returns true if the registry is tracking a registered field adapter.
   *
   * @return true if the registry is tracking a registered field adapter.
   */
  public boolean hasFieldAdapters() {
    return this.resourceGraph.values().stream()
      .map(DinaResourceEntry::getFieldAdapterHandler).findFirst().isPresent();
  }

  /**
   * Returns the nested resource type from a given base resource type and attribute path. The deepest nested
   * resource that can be resolved will be returned. The original resource is returned if a nested resource is
   * not present in the attribute path, or the resources are not tracked by the registry.
   *
   * @param resource      - base resource to traverse
   * @param attributePath - attribute path to follow
   * @return - the nested resource type from a given path.
   */
  public Class<?> resolveNestedResourceFromPath(
    @NonNull Class<?> resource,
    @NonNull List<String> attributePath
  ) {
    Class<?> nested = resource;
    for (String attribute : attributePath) {
      Optional<InternalRelation> relation = this.findMappableRelationsForClass(nested).stream()
        .filter(internalRelation -> internalRelation.getName().equalsIgnoreCase(attribute))
        .findAny();
      if (relation.isPresent()) {
        nested = relation.get().getDtoType();
      } else {
        break;
      }
    }
    return nested;
  }

  private static InternalRelation mapToInternalRelation(Field field) {
    Class<?> fieldType = field.getType();
    boolean isCollection = false;

    if (isCollection(fieldType)) {
      fieldType = parseGenericTypeForField(field);
      isCollection = true;
    }

    return InternalRelation.builder()
        .name(field.getName())
        .isCollection(isCollection)
        .dtoType(fieldType)
        .entityType(fieldType.getAnnotation(RelatedEntity.class).value())
        .build();
  }

  /**
   * Returns a map of external relation field names to their JsonApiExternalRelation.type for a given class.
   *
   * @param resourceClass - a given class with external relations.
   * @return a map of external relation field names to their JsonApiExternalRelation.type
   */
  private static Map<String, String> parseExternalRelationNamesToType(Class<?> resourceClass) {
    return Map.copyOf(
      FieldUtils.getFieldsListWithAnnotation(resourceClass, JsonApiExternalRelation.class)
        .stream().collect(Collectors.toMap(
          Field::getName,
          field -> field.getAnnotation(JsonApiExternalRelation.class).type())));
  }

  /**
   * Returns true if the dina repo should map the given field as an attribute. An attribute in this context is
   * a field that has its value mapped directly.
   *
   * @param dtoClass    dto class to validate against
   * @param entityClass entity class to validate against
   * @param field       field to evaluate
   * @return - true if the dina repo should not map the given field
   */
  private static boolean isFieldConsideredAnAttribute(Class<?> dtoClass, Class<?> entityClass, Field field) {
    return !field.isAnnotationPresent(IgnoreDinaMapping.class)
      && !field.isAnnotationPresent(JsonApiRelation.class)
      && fieldExistsInBothClasses(dtoClass, entityClass, field.getName())
      && !Modifier.isFinal(field.getModifiers())
      && !field.isSynthetic();
  }

  /**
   * Returns true if the dina repo should map the given relation. A relation should be mapped if it is not
   * external, and present in the given dto and entity classes.
   *
   * @param dto              - resource class of the relation
   * @param entity           - entity class of the relation
   * @param dtoRelationField - relation field to map
   * @return true if the dina repo should map the given relation.
   */
  private static boolean isMappableRelation(Class<?> dto, Class<?> entity, Field dtoRelationField) {
    return dtoRelationField.isAnnotationPresent(JsonApiRelation.class)
      && !dtoRelationField.isAnnotationPresent(IgnoreDinaMapping.class)
      && !dtoRelationField.isAnnotationPresent(JsonApiExternalRelation.class)
      && fieldExistsInBothClasses(dto, entity, dtoRelationField.getName());
  }

  private static boolean fieldExistsInBothClasses(Class<?> dtoClass, Class<?> entityClass, String fieldName) {
    return getAllFields(dtoClass).stream().anyMatch(field -> fieldName.equals(field.getName()))
      && getAllFields(entityClass).stream().anyMatch(field -> fieldName.equals(field.getName()));
  }

  @SneakyThrows
  private static boolean fieldHasSameDataType(Class<?> entityClass, Field dtoField) {
    Field entityClassDeclaredField = getAllFields(entityClass).stream()
      .filter(f -> f.getName().equals(dtoField.getName()))
      .findFirst()
      .orElse(null);

    if (entityClassDeclaredField == null) {
      return false;
    }

    Type typeOnDto = dtoField.getGenericType();
    if (!entityClassDeclaredField.getGenericType().equals(typeOnDto)) { // Data types must match!
      return false;
    }

    if (typeOnDto instanceof ParameterizedType) { // If parameterized generic type must match
      return parseGenericTypeForField(dtoField)
        .equals(parseGenericTypeForField(entityClassDeclaredField));
    }
    return true;
  }

  private static Class<?> parseGenericTypeForField(Field field) {
    if (field.getGenericType() instanceof ParameterizedType) {
      return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    } else {
      return field.getType();
    }
  }

  private static String parseJsonIdFieldName(Class<?> resourceClass) {
    for (Field field : getAllFields(resourceClass)) {
      if (field.isAnnotationPresent(JsonApiId.class)) {
        return field.getName();
      }
    }
    return null;
  }

  private static DinaResourceEntry buildResourceEntry(
    Class<?> resourceClass,
    Class<?> entityClass,
    Set<String> attributes,
    Set<InternalRelation> internalRelations
  ) {
    return DinaResourceEntry.builder()
      .dtoClass(resourceClass)
      .entityClass(entityClass)
      .externalNameToTypeMap(parseExternalRelationNamesToType(resourceClass))
      .attributeNames(attributes)
      .internalRelations(internalRelations)
      .jsonIdFieldName(parseJsonIdFieldName(resourceClass))
      .fieldAdapterHandler(new DinaFieldAdapterHandler<>(resourceClass))
      .build();
  }

  private static List<Field> getAllFields(Class<?> type) {
    List<Field> fields = new ArrayList<>(Arrays.asList(type.getDeclaredFields()));

    if (type.getSuperclass() != null) {
      fields.addAll(getAllFields(type.getSuperclass()));
    }

    return fields;
  }

  private void checkClassTracked(Class<?> cls) {
    if (!this.resourceGraph.containsKey(cls)) {
      throw new IllegalArgumentException(cls.getSimpleName() + " is not tracked by the registry");
    }
  }

  private static void throwDataTypeMismatchException(Class<?> dto, Class<?> entity, String attrib) {
    throw new IllegalStateException("data type for Field:{" + attrib + "} on DTO:{" + dto.getSimpleName()
      + "} does not match the field from Entity:{" + entity.getSimpleName() + "}");
  }

  /**
   * Returns true if the given class is a collection
   *
   * @param clazz - class to check
   * @return true if the given class is a collection
   */
  private static boolean isCollection(Class<?> clazz) {
    return Collection.class.isAssignableFrom(clazz);
  }

  // Avoid CT_CONSTRUCTOR_THROW
  protected final void finalize() {
    // no-op
  }

  /**
   * Internal Relation Representing a field of class to be mapped
   */
  @Builder
  @Getter
  public static class InternalRelation {
    private final String name;
    private final Class<?> dtoType;
    private final Class<?> entityType;
    private final boolean isCollection;
  }

  @Builder
  @Getter
  public static class DinaResourceEntry {
    private Class<?> dtoClass;
    private Class<?> entityClass;
    private String jsonIdFieldName;
    private Set<String> attributeNames;
    private Set<InternalRelation> internalRelations;
    private Map<String, String> externalNameToTypeMap;
    private DinaFieldAdapterHandler<?> fieldAdapterHandler;

    public InternalRelation getInternalRelationByName(String name) {
      return internalRelations.stream()
        .filter(i -> i.name.equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
    }
  }
}
