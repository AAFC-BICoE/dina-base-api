package ca.gc.aafc.dina.mapper;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Mapping layer handles the responsibilities for dina repo mapping operations. Those
 * responsibilities are the following.
 * <pre>
 *   <ul>
 *   <li>Bean mapping from resource to entity and vise versa</li>
 *   <li>Returning shallow ids when returning DTOs</li>
 *   <li>Linking relations to database backed resources when returning entities</li>
 *   <li>Mapping external relations for all mapping directions</li>
 * </ul>
 * </pre>
 *
 * @param <D> - Resource type for the mapping layer
 * @param <E> - Entity type for the mapping layer
 */
public class DinaMappingLayer<D, E> {

  private final Class<D> resourceClass;
  private final DinaMapper<D, E> dinaMapper;
  private final DinaService<?> dinaService;
  private final DinaMappingRegistry registry;

  public DinaMappingLayer(
    @NonNull Class<D> resourceClass,
    @NonNull DinaService<?> dinaService,
    @NonNull DinaMapper<D, E> dinaMapper
  ) {
    this.resourceClass = resourceClass;
    this.dinaService = dinaService;
    this.dinaMapper = dinaMapper;
    this.registry = new DinaMappingRegistry(resourceClass);
  }

  /**
   * Returns a list of resources mapped from the given entities. Relations included in the query
   * spec are fully mapped. External relations are always mapped. Relations not included in the
   * query spec are shallow mapped (natural id only).
   *
   * @param query    - query spec of the request
   * @param entities - entities to map
   * @return - list of resources.
   */
  public List<D> mapEntitiesToDto(@NonNull QuerySpec query, @NonNull List<E> entities) {
    Set<String> includedRelations = query.getIncludedRelations().stream()
      .map(ir -> ir.getAttributePath().get(0))
      .filter(Predicate.not(registry::isRelationExternal)).collect(Collectors.toSet());

    Set<DinaMappingRegistry.InternalRelation> shallowRelationsToMap = registry
      .findMappableRelationsForClass(resourceClass)
      .stream()
      .filter(rel -> includedRelations.stream().noneMatch(rel.getName()::equalsIgnoreCase))
      .collect(Collectors.toSet());

    return entities.stream()
      .map(e -> {
        // Bean mapping
        D dto = dinaMapper.toDto(e, registry.getAttributesPerClass(), includedRelations);
        // Map shallow ids fo un-included relations
        mapShallowRelations(e, dto, shallowRelationsToMap);
        // Map External Relations
        mapExternalRelationsToDto(e, dto);
        return dto;
      })
      .collect(Collectors.toList());
  }

  /**
   * Maps a given dto to a given entity. All external/internal relations are mapped, relations are
   * linked to their database backed resource if they are not external.
   *
   * @param dto    - source of the mapping
   * @param entity - target of the mapping
   * @param <S>    dto type
   */
  public <S extends D> void mapToEntity(@NonNull S dto, @NonNull E entity) {
    Set<DinaMappingRegistry.InternalRelation> mappableRelationsForClass = registry
      .findMappableRelationsForClass(dto.getClass());

    // Bean mapping
    Set<String> relationNames = mappableRelationsForClass.stream()
      .map(DinaMappingRegistry.InternalRelation::getName).collect(Collectors.toSet());
    dinaMapper.applyDtoToEntity(
      dto, entity, registry.getAttributesPerClass(), relationNames);
    // Link relations to Database backed resources
    linkRelations(entity, mappableRelationsForClass);
    // Map External Relations
    mapExternalRelationsToEntity(dto, entity);
  }

  /**
   * Returns a new dto with only the attributes mapped from a given entity.
   *
   * @param entity - source of the mapping
   * @return a new dto mapped from a given source
   */
  public D toDtoSimpleMapping(@NonNull E entity) {
    return dinaMapper.toDto(entity, registry.getAttributesPerClass(), Collections.emptySet());
  }

  /**
   * Maps the external relations from the given source to the given target
   *
   * @param source - source of the external relations
   * @param target - target of the mapping
   */
  private void mapExternalRelationsToDto(E source, D target) {
    registry.getExternalRelations().forEach(external -> {
      Object id = PropertyUtils.getProperty(source, external);
      if (id != null) {
        PropertyUtils.setProperty(target, external,
          ExternalRelationDto.builder()
            .type(registry.findExternalType(external))
            .id(id.toString())
            .build());
      } else {
        PropertyUtils.setProperty(target, external, null);
      }
    });
  }

  /**
   * Maps the external relations from the given source to the given target.
   *
   * @param source - source of the external relations
   * @param target - target of the mapping
   */
  private void mapExternalRelationsToEntity(D source, E target) {
    registry.getExternalRelations().forEach(external -> {
      Object externalRelation = PropertyUtils.getProperty(source, external);
      if (externalRelation != null) {
        String jsonIdFieldName = registry.findJsonIdFieldName(ExternalRelationDto.class);
        PropertyUtils.setProperty(target, external,
          UUID.fromString(PropertyUtils.getProperty(externalRelation, jsonIdFieldName).toString()));
      } else {
        PropertyUtils.setProperty(target, external, null);
      }
    });
  }

  /**
   * Maps the given relations from the given entity to a given dto in a shallow form (Id only).
   *
   * @param entity    - source of the mapping
   * @param dto       - target of the mapping
   * @param relations - relations to map
   */
  private void mapShallowRelations(
    @NonNull E entity,
    @NonNull D dto,
    @NonNull Set<DinaMappingRegistry.InternalRelation> relations
  ) {
    mapRelations(entity, dto, relations,
      (aClass, relation) ->
        createShallowDTO(registry.findJsonIdFieldName(aClass), aClass, relation));
  }

  /**
   * Replaces the given relations of given entity with there JPA entity equivalent. Relations id's
   * are used to map a relation to its JPA equivalent.
   *
   * @param entity    - entity containing the relations
   * @param relations - list of relations to map
   */
  private void linkRelations(
    @NonNull E entity,
    @NonNull Set<DinaMappingRegistry.InternalRelation> relations
  ) {
    mapRelations(entity, entity, relations,
      (aClass, relation) -> returnPersistedObject(registry.findJsonIdFieldName(aClass), relation));
  }

  /**
   * Maps the given relations from a given source to a given target with a given mapping function.
   *
   * @param source - source of the mapping
   * @param target - target of the mapping
   * @param mapper - mapping function to apply
   */
  private static void mapRelations(
    @NonNull Object source,
    @NonNull Object target,
    @NonNull Set<DinaMappingRegistry.InternalRelation> relations,
    @NonNull BiFunction<Class<?>, Object, Object> mapper
  ) {
    for (DinaMappingRegistry.InternalRelation relation : relations) {
      String relationName = relation.getName();
      Class<?> relationType = relation.getElementType();
      if (relation.isCollection()) {
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

}