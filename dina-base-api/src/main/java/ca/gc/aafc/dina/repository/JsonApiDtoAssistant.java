package ca.gc.aafc.dina.repository;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.beanutils.PropertyUtils;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.JsonApiDtoMeta;
import ca.gc.aafc.dina.dto.JsonApiExternalResource;
import ca.gc.aafc.dina.dto.JsonApiResource;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;

/**
 * Assistant created by {@link DinaRepositoryV2} instances to assist building and configuring
 * {@link JsonApiDto}.
 * @param <D>
 */
@Log4j2
public class JsonApiDtoAssistant<D> {

  private final DinaMappingRegistry dinaMappingRegistry;
  private final Function<ExternalRelationDto, JsonApiExternalResource> externalRelationDtoToResourceFunction;
  private final Class<D> resourceClass;

  public JsonApiDtoAssistant(DinaMappingRegistry dinaMappingRegistry,
                             Function<ExternalRelationDto, JsonApiExternalResource> externalRelationDtoToResourceFunction,
                             Class<D> resourceClass) {
    this.dinaMappingRegistry = dinaMappingRegistry;
    this.externalRelationDtoToResourceFunction = externalRelationDtoToResourceFunction;
    this.resourceClass = resourceClass;
  }

  /**
   * see {@link #toJsonApiDto(Object, JsonApiDtoMeta, Map, Set)}
   */
  public JsonApiDto<D> toJsonApiDto(D dto, Map<String, List<String>> fields, Set<String> includes) {
    return toJsonApiDto(dto, null, fields, includes);
  }

  /**
   * Build a {@link JsonApiDto} for a given dto, metadata and a set of includes.
   *
   * @param dto
   * @param meta metadata related to the specific dto
   * @param includes sparse field-set by type
   * @param includes
   * @return
   */
  public JsonApiDto<D> toJsonApiDto(D dto, JsonApiDtoMeta meta, Map<String, List<String>> fields,
                                    Set<String> includes) {
    JsonApiDto.JsonApiDtoBuilder<D> jsonApiDtoBuilder = JsonApiDto.builder();
    jsonApiDtoBuilder.meta(meta);
    jsonApiDtoBuilder.fields(fields);

    for (String include : includes) {
      try {
        Object rel = PropertyUtils.getProperty(dto, include);
        if (rel instanceof Collection<?> coll) {
          handleToManyRelationship(jsonApiDtoBuilder, include, coll);
        } else if (rel != null) {
          handleToOneRelationship(jsonApiDtoBuilder, include, rel);
        } else {
          handleNullValueRelationship(jsonApiDtoBuilder, include);
        }
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
    return jsonApiDtoBuilder.dto(dto).build();
  }

  private void handleToOneRelationship(JsonApiDto.JsonApiDtoBuilder<?> builder, String name,
                                       Object rel) {
    switch (rel) {
      case JsonApiExternalResource jaer -> builder.relationship(name,
        JsonApiDto.RelationshipToOneExternal.builder()
          .included(jaer).build());
      case JsonApiResource ddto -> builder.relationship(name,
        JsonApiDto.RelationshipToOne.builder()
          .included(ddto).build());
      case ExternalRelationDto erd -> builder.relationship(name,
        JsonApiDto.RelationshipToOneExternal.builder()
          .included(externalRelationDtoToResourceFunction.apply(erd)).build());
      case null, default -> log.warn("Not an instance of JsonApiResource, ignoring {}", name);
    }
  }

  private void handleToManyRelationship(JsonApiDto.JsonApiDtoBuilder<?> builder, String name,
                                        Collection<?> rel) {

    List<JsonApiResource> castSafeList = new ArrayList<>(rel.size());
    List<JsonApiExternalResource> castSafeListExternal = new ArrayList<>();

    for (Object element : rel) {
      switch (element) {
        case JsonApiExternalResource jaer -> castSafeListExternal.add(jaer);
        case JsonApiResource jar -> castSafeList.add(jar);
        case ExternalRelationDto erd ->
          castSafeListExternal.add(externalRelationDtoToResourceFunction.apply(erd));
        case null, default -> log.warn("Not an instance of JsonApiResource, ignoring {}", name);
      }
    }

    if (!castSafeListExternal.isEmpty()) {
      builder.relationship(name, JsonApiDto.RelationshipManyExternal.builder()
        .included(castSafeListExternal).build());
    } else {
      builder.relationship(name,
        JsonApiDto.RelationshipToMany.builder()
          .included(castSafeList).build());
    }
  }

  /**
   * Handle relationships when the assigned value is null.
   * @param builder
   * @param relationshipName
   */
  private void handleNullValueRelationship(JsonApiDto.JsonApiDtoBuilder<?> builder, String relationshipName) {

    Class<?> internalRelClass = dinaMappingRegistry.getInternalRelationClass(resourceClass, relationshipName);
    if (internalRelClass != null) {
      if (DinaMappingRegistry.isCollection(internalRelClass)) {
        builder.relationship(relationshipName,
          JsonApiDto.RelationshipToMany.builder().build());
      } else {
        builder.relationship(relationshipName,
          JsonApiDto.RelationshipToOne.builder().build());
      }
    } else {
      Class<?> relClass = dinaMappingRegistry.getExternalRelationClass(resourceClass, relationshipName);
      if (DinaMappingRegistry.isCollection(relClass)) {
        builder.relationship(relationshipName,
          JsonApiDto.RelationshipManyExternal.builder().build());
      } else {
        builder.relationship(relationshipName,
          JsonApiDto.RelationshipToOneExternal.builder().build());
      }
    }
  }
}
