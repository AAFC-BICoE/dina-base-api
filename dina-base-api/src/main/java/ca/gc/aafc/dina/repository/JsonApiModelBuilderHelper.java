package ca.gc.aafc.dina.repository;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

import org.springframework.hateoas.RepresentationModel;

import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;

import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.JsonApiResource;

import static com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder.jsonApiModel;

/**
 * Utility class to assist with configuring {@link JsonApiModelBuilder} for {@link JsonApiDto}.
 * The main focus is around relationships.
 *
 * Mostly supposed to be used by {@link DinaRepositoryV2}
 */
@Log4j2
public final class JsonApiModelBuilderHelper {

  private JsonApiModelBuilderHelper () {
    // utility class
  }

  /**
   * Internal(package protected) method to create {@link JsonApiModelBuilder}.
   * @param jsonApiDto
   * @param includeBuilder builder to use to add "included" documents. if null, the builder
   *                       created for that document will be used.
   * @param included set of already included uuid
   * @return
   */
  public static JsonApiModelBuilder createJsonApiModelBuilder(JsonApiDto<?> jsonApiDto,
                                                        JsonApiModelBuilder includeBuilder,
                                                        Set<UUID> included) {
    JsonApiModelBuilder builder = jsonApiModel().model(RepresentationModel.of(jsonApiDto.getDto()));

    for (var rel : jsonApiDto.getRelationships().entrySet()) {
      switch (rel.getValue()) {
        case JsonApiDto.RelationshipToOne toOne ->
          setToOneOnJsonApiModelBuilder(builder, rel.getKey(), toOne, includeBuilder, included);
        case JsonApiDto.RelationshipToMany toMany ->
          setToManyOnJsonApiModelBuilder(builder, rel.getKey(), toMany, includeBuilder, included);
        case JsonApiDto.RelationshipToOneExternal toOneExt ->
          setToOneExtOnJsonApiModelBuilder(builder, rel.getKey(), toOneExt);
        case JsonApiDto.RelationshipManyExternal toManyExt ->
          setToManyExtOnJsonApiModelBuilder(builder, rel.getKey(), toManyExt);
        default -> throw new IllegalStateException("Unexpected value: " + rel.getValue());
      }
    }
    return builder;
  }

  /**
   * Internal method to set {@link JsonApiDto.RelationshipToOne} to a {@link JsonApiModelBuilder}.
   * @param builder current builder
   * @param relationshipName
   * @param toOne
   * @param includeBuilder if null, builder will be used
   * @param included set of already included uuid
   */
  private static void setToOneOnJsonApiModelBuilder(JsonApiModelBuilder builder, String relationshipName,
                                                    JsonApiDto.RelationshipToOne toOne,
                                                    JsonApiModelBuilder includeBuilder,
                                                    Set<UUID> included) {

    if (toOne.getIncluded() != null) {
      builder.relationship(relationshipName, toOne.getIncluded());
      addUniqueIncluded(includeBuilder != null ? includeBuilder : builder, toOne.getIncluded(), included);
    } else {
      // requires spring-hateoas-jsonapi 2.x
      // builder.relationship(relationshipName, (Object) null);
      log.warn("Ignoring null value for toOne internal relationship {}", relationshipName);
    }
  }

  /**
   * Internal method to set {@link JsonApiDto.RelationshipToMany} to a {@link JsonApiModelBuilder}.
   * @param builder current builder
   * @param relationshipName
   * @param toMany
   * @param includeBuilder if null, builder will be used
   * @param included set of already included uuid
   */
  private static void setToManyOnJsonApiModelBuilder(JsonApiModelBuilder builder, String relationshipName,
                                                     JsonApiDto.RelationshipToMany toMany, JsonApiModelBuilder includeBuilder,Set<UUID> included) {

    if (toMany.getIncluded() != null) {
      builder.relationship(relationshipName, toMany.getIncluded());
      for (var includedResource : toMany.getIncluded()) {
        addUniqueIncluded(includeBuilder != null ? includeBuilder : builder, includedResource, included);
      }
    } else {
      // requires spring-hateoas-jsonapi 2.x
      // builder.relationship(relationshipName, (Object) null);
      log.warn("Ignoring null value for toMany internal relationship {}", relationshipName);
    }
  }

  private static void setToOneExtOnJsonApiModelBuilder(JsonApiModelBuilder builder, String relationshipName,
                                                       JsonApiDto.RelationshipToOneExternal toOneExt) {

    if (toOneExt.getIncluded() != null) {
      builder.relationship(relationshipName, toOneExt.getIncluded());
      // addUniqueIncluded(builder, toOneExt.getIncluded(), included);
    } else {
      //requires spring-hateoas-jsonapi 2.x
      // builder.relationship(relationshipName, (Object) null);
      log.warn("Ignoring null value for toOne external relationship {}", relationshipName);
    }
  }

  private static void setToManyExtOnJsonApiModelBuilder(JsonApiModelBuilder builder, String relationshipName,
                                                        JsonApiDto.RelationshipManyExternal toManyExt) {
    if (toManyExt.getIncluded() != null) {
      builder.relationship(relationshipName, toManyExt.getIncluded());
    } else {
      //requires spring-hateoas-jsonapi 2.x
      log.warn("Ignoring null value for toMany external relationship {}", relationshipName);
    }
  }

  /**
   * Add an include {@link JsonApiResource} is not already present.
   * The main goal of this method is to avoid duplicates in the include section.
   *
   * @param builder the current builder
   * @param include the {@link JsonApiResource} to include
   * @param included writable non-null set containing the already included uuid
   */
  private static void addUniqueIncluded(JsonApiModelBuilder builder,
                                        JsonApiResource include, Set<UUID> included) {
    Objects.requireNonNull(include);

    if (!included.contains(include.getJsonApiId())) {
      builder.included(include);
      included.add(include.getJsonApiId());
    }
  }
}
