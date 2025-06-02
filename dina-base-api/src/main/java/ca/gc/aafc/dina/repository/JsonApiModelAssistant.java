package ca.gc.aafc.dina.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;

import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;

import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.JsonApiMeta;
import ca.gc.aafc.dina.dto.JsonApiResource;

import static com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder.jsonApiModel;

/**
 * Utility class to assist with configuring {@link JsonApiModelBuilder} for {@link JsonApiDto}.
 * The main focus is around relationships.
 *
 * Mostly supposed to be used by {@link DinaRepositoryV2} or other repositories to configure
 * {@link JsonApiModelBuilder} to return Spring hateoas compliant response.
 */
@Log4j2
public class JsonApiModelAssistant <D extends JsonApiResource> {

  private final String moduleVersion;

  public JsonApiModelAssistant() {
    this("");
  }

  public JsonApiModelAssistant(String moduleVersion) {
    this.moduleVersion = moduleVersion;
  }

  /**
   * Extract the UUID(id) of a created resource from a {@link RepresentationModel} object.
   * @param responseEntity
   * @return
   */
  public static UUID extractUUIDFromRepresentationModelLink(ResponseEntity<RepresentationModel<?>> responseEntity) {

    if (responseEntity.getBody() == null ||
      responseEntity.getBody().getLink(IanaLinkRelations.SELF).isEmpty()) {
      return null;
    }

    return UUID.fromString(StringUtils.substringAfterLast(responseEntity.getBody()
      .getLink(IanaLinkRelations.SELF).get().getHref(), "/"));
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

  /**
   * Responsible to create the {@link JsonApiModelBuilder} for the provided {@link JsonApiDto}.
   *
   * @param jsonApiDto
   * @return
   */
  public JsonApiModelBuilder createJsonApiModelBuilder(JsonApiDto<D> jsonApiDto) {
    Set<UUID> included = new HashSet<>(jsonApiDto.getRelationships().size());

    JsonApiModelBuilder mainBuilder = jsonApiModel();

    JsonApiModelBuilder builder = JsonApiModelAssistant.
      createJsonApiModelBuilder(jsonApiDto, mainBuilder, included);

    // Set meta on the resource object if required
    if (jsonApiDto.getMeta() != null) {
      jsonApiDto.getMeta().populateMeta(builder::meta);
    }

    JsonApiMeta.builder()
      .moduleVersion(moduleVersion)
      .build()
      .populateMeta(mainBuilder::meta);
    mainBuilder.model(builder.build());
    return mainBuilder;
  }

  /**
   * Same as {@link #createJsonApiModelBuilder(JsonApiDto)} but for {@link DinaRepositoryV2.PagedResource}.
   * @param jsonApiDtos
   * @return
   */
  public JsonApiModelBuilder createJsonApiModelBuilder(
    DinaRepositoryV2.PagedResource<JsonApiDto<D>> jsonApiDtos) {
    return createJsonApiModelBuilder(jsonApiDtos.resourceList(), jsonApiDtos.totalCount());
  }

  /**
   *
   * @param jsonApiDtos
   * @param totalCount totalCount of resources or null to not include a totalResourceCount in the meta section.
   * @return
   */
  public JsonApiModelBuilder createJsonApiModelBuilder(List<JsonApiDto<D>> jsonApiDtos, Integer totalCount) {
    JsonApiModelBuilder mainBuilder = jsonApiModel();
    List<RepresentationModel<?>> repModels = new ArrayList<>();
    Set<UUID> included = new HashSet<>();
    for (JsonApiDto<D> currResource : jsonApiDtos) {
      JsonApiModelBuilder builder = JsonApiModelAssistant.
        createJsonApiModelBuilder(currResource, mainBuilder, included);
      repModels.add(builder.build());
    }

    // use custom metadata instead of PagedModel.PageMetadata so we can control
    // the content and key names
    var metaSectionBuilder = JsonApiMeta.builder()
      .moduleVersion(moduleVersion);

    if (totalCount != null) {
      metaSectionBuilder.totalResourceCount(totalCount);
    }

    metaSectionBuilder.build()
      .populateMeta(mainBuilder::meta);

    mainBuilder.model(CollectionModel.of(repModels));
    return mainBuilder;
  }
}
