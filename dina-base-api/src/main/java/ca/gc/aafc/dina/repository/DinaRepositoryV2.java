package ca.gc.aafc.dina.repository;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tennaito.rsql.misc.ArgumentParser;
import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.JsonApiExternalResource;
import ca.gc.aafc.dina.dto.JsonApiResource;
import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterArgumentParser;
import ca.gc.aafc.dina.filter.EntityFilterHelper;
import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.filter.SimpleFilterHandlerV2;
import ca.gc.aafc.dina.mapper.DinaMapperV2;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.security.auth.DinaAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaService;

import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import static com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder.jsonApiModel;

@Log4j2
public class DinaRepositoryV2<D,E extends DinaEntity> {

  // default page limit/page size
  private static final int DEFAULT_PAGE_LIMIT = 20;
  private static final int MAX_PAGE_LIMIT = 100;

  private final DinaAuthorizationService authorizationService;
  private final DinaService<E> dinaService;
  private final Class<E> entityClass;
  private final Class<D> resourceClass;
  private final DinaMapperV2<D, E> dinaMapper;

  protected final DinaMappingRegistry registry;

  private final ArgumentParser rsqlArgumentParser = new DinaFilterArgumentParser();

  public DinaRepositoryV2(@NonNull DinaService<E> dinaService,
                          @NonNull DinaAuthorizationService authorizationService,
                          @NonNull Optional<AuditService> auditService,
                          @NonNull DinaMapperV2<D, E> dinaMapper,
                          @NonNull Class<D> resourceClass,
                          @NonNull Class<E> entityClass,
                          @NonNull BuildProperties buildProperties,
                          ObjectMapper objMapper) {

    this.authorizationService = authorizationService;
    this.dinaService = dinaService;
    this.entityClass = entityClass;
    this.resourceClass = resourceClass;
    this.dinaMapper = dinaMapper;

    // build registry instance for resource class (dto)
    this.registry = new DinaMappingRegistry(resourceClass);
  }

  /**
   * Used to provide mapping between {@link ExternalRelationDto} and new JsonApiExternalResource instances.
   * By default, this method returns null since the supported types are unknown.
   * @param externalRelationDto
   * @return
   */
  protected JsonApiExternalResource externalRelationDtoToJsonApiExternalResource(ExternalRelationDto externalRelationDto) {
    return null;
  }

  /**
   * @param identifier
   * @param queryString
   * @return
   */
  public JsonApiDto<D> getOne(UUID identifier, String queryString) {

    // the only part of QueryComponent that can be used on getOne is "includes"
    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    Set<String> includes = queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of();

    validateIncludes(includes);

    E entity = dinaService.findOne(identifier, entityClass, includes);

    authorizationService.authorizeRead(entity);

    Set<String> attributes = new HashSet<>(registry.getAttributesPerClass().get(entityClass));
    attributes.addAll(includes);
    addNestedAttributesFromIncludes(attributes, includes);

    D dto = dinaMapper.toDto(entity, attributes, null);

    return toJsonApiDto(dto, includes);
  }

  /**
   * Build a {@link JsonApiDto} for a given dto and a set of includes.
   *
   * @param dto
   * @param includes
   * @return
   */
  private JsonApiDto<D> toJsonApiDto(D dto, Set<String> includes) {
    JsonApiDto.JsonApiDtoBuilder<D> jsonApiDtoBuilder = JsonApiDto.builder();
    for (String include : includes) {
      try {
        Object rel = PropertyUtils.getProperty(dto, include);
        if (rel instanceof Collection<?> coll) {
          handleToManyRelationship(jsonApiDtoBuilder, include, coll);
        } else {
          handleToOneRelationship(jsonApiDtoBuilder, include, rel);
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
          .included(externalRelationDtoToJsonApiExternalResource(erd)).build());
      case null, default -> log.warn("Not an instance of JsonApiResource, ignoring {}", name);
    }
  }

  private void handleToManyRelationship(JsonApiDto.JsonApiDtoBuilder<?> builder, String name,
                                               Collection<?> rel) {

    List<JsonApiResource> castSafeList = new ArrayList<>(rel.size());
    List<JsonApiExternalResource> castSafeListExternal = new ArrayList<>();

    for (Object element : rel) {
      if (element instanceof JsonApiExternalResource jaer) {
        castSafeListExternal.add(jaer);
      } else if (element instanceof JsonApiResource jar) {
        castSafeList.add(jar);
      } else if (element instanceof ExternalRelationDto erd) {
        castSafeListExternal.add(externalRelationDtoToJsonApiExternalResource(erd));
      } else {
        log.warn("Not an instance of JsonApiResource, ignoring {}", name);
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
   * Responsible to create the {@link JsonApiModelBuilder} for the provided {@link JsonApiDto}.
   *
   * @param jsonApiDto
   * @return
   */
  protected JsonApiModelBuilder createJsonApiModelBuilder(JsonApiDto<D> jsonApiDto) {
    JsonApiModelBuilder builder = jsonApiModel().model(RepresentationModel.of(jsonApiDto.getDto()));

    Set<UUID> included = new HashSet<>(jsonApiDto.getRelationships().size());
    for (var rel : jsonApiDto.getRelationships().entrySet()) {
      switch (rel.getValue()) {
        case JsonApiDto.RelationshipToOne toOne -> {
          builder.relationship(rel.getKey(), toOne.getIncluded());
          addUniqueIncluded(builder, toOne.getIncluded(), included);
        }
        case JsonApiDto.RelationshipToMany toMany -> {
          builder.relationship(rel.getKey(), toMany.getIncluded());
          for (var includedResource : toMany.getIncluded()) {
            addUniqueIncluded(builder, includedResource, included);
          }
        }
        case JsonApiDto.RelationshipToOneExternal toOneExt -> {
          builder.relationship(rel.getKey(), toOneExt.getIncluded());
          builder.included(toOneExt.getIncluded());
        }
        case JsonApiDto.RelationshipManyExternal toManyExt -> {
          builder.relationship(rel.getKey(), toManyExt.getIncluded());
          builder.included(toManyExt.getIncluded());
        }
        default -> throw new IllegalStateException("Unexpected value: " + rel.getValue());
      }
    }
    return builder;
  }

  protected JsonApiModelBuilder createJsonApiModelCollectionBuilder(PagedResource<JsonApiDto<D>> jsonApiDtos) {

    JsonApiModelBuilder mbuilder = jsonApiModel();
    List<RepresentationModel<?>> repModels = new ArrayList<>();

    for (JsonApiDto<D> currResource : jsonApiDtos.resourceList()) {
      Set<UUID> included = new HashSet<>(currResource.getRelationships().size());
      JsonApiModelBuilder builder = jsonApiModel().model(RepresentationModel.of(currResource.getDto()));
      for (var rel : currResource.getRelationships().entrySet()) {
        switch (rel.getValue()) {
          case JsonApiDto.RelationshipToOne toOne -> {
            builder.relationship(rel.getKey(), toOne.getIncluded());
            addUniqueIncluded(mbuilder, toOne.getIncluded(), included);
          }
          case JsonApiDto.RelationshipToMany toMany -> {
            builder.relationship(rel.getKey(), toMany.getIncluded());
            for (var includedResource : toMany.getIncluded()) {
              addUniqueIncluded(mbuilder, includedResource, included);
            }
          }
          case JsonApiDto.RelationshipToOneExternal toOneExt -> {
            builder.relationship(rel.getKey(), toOneExt.getIncluded());
        //    builder.included(toOneExt.getIncluded());
          }
          case JsonApiDto.RelationshipManyExternal toManyExt -> {
            builder.relationship(rel.getKey(), toManyExt.getIncluded());
        //    builder.included(toManyExt.getIncluded());
          }
          default -> throw new IllegalStateException("Unexpected value: " + rel.getValue());
        }
      }
      repModels.add(builder.build());
    }

    PagedModel.PageMetadata pageMetadata = new PagedModel.PageMetadata(repModels.size(),
      jsonApiDtos.pageOffset, jsonApiDtos.totalCount);

    PagedModel<? extends RepresentationModel<?>> pagedModel =
      PagedModel.of(repModels, pageMetadata);

    mbuilder.model(pagedModel);
    return mbuilder;
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
   * null-safe query string UTF-8 decode function.
   * @param req
   * @return decoded query string or empty string if query string is absent
   */
  protected static String decodeQueryString(HttpServletRequest req) {
    Objects.requireNonNull(req);

    if (StringUtils.isBlank(req.getQueryString())) {
      return "";
    }
    return URLDecoder.decode(req.getQueryString(), StandardCharsets.UTF_8);
  }

  public PagedResource<JsonApiDto<D>> getAll(String queryString) {
    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    return getAll(queryComponents);
  }

  public PagedResource<JsonApiDto<D>> getAll(QueryComponent queryComponents) {

    FilterComponent fc = queryComponents.getFilters();

    Set<String> relationshipsPath = EntityFilterHelper.extractRelationships(queryComponents.getIncludes(), resourceClass, registry);
    Set<String> includes = queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of();

    validateIncludes(includes);
    int pageOffset = toSafePageOffset(queryComponents.getPageOffset());
    int pageLimit = toSafePageLimit(queryComponents.getPageLimit());

    List<E> entities = dinaService.findAll(
      entityClass,
      (criteriaBuilder, root, em) -> {
        EntityFilterHelper.leftJoinSortRelations(root, queryComponents.getSorts(), resourceClass, registry);

        Predicate restriction = SimpleFilterHandlerV2.getRestriction(root, criteriaBuilder, rsqlArgumentParser::parse, em.getMetamodel(), fc != null ? List.of(fc) : List.of());
        return new Predicate[]{restriction};
      },
      (cb, root) -> EntityFilterHelper.getOrders(cb, root, queryComponents.getSorts(), false),
      pageOffset, pageLimit, includes, relationshipsPath);

    List<JsonApiDto<D>> dtos = new ArrayList<>(entities.size());

    Set<String> attributes = new HashSet<>(registry.getAttributesPerClass().get(entityClass));
    attributes.addAll(queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of());
    addNestedAttributesFromIncludes(attributes, includes);

    for (E e : entities) {
      dtos.add(toJsonApiDto(dinaMapper.toDto(e, attributes, null), includes));
    }

    Long resourceCount = dinaService.getResourceCount( entityClass,
      (criteriaBuilder, root, em) -> {
        Predicate restriction = SimpleFilterHandlerV2.getRestriction(root, criteriaBuilder, rsqlArgumentParser::parse, em.getMetamodel(), fc != null ? List.of(fc) : List.of());
        return new Predicate[]{restriction};
      });

    return new PagedResource<>(pageOffset, pageLimit, resourceCount.intValue(), dtos);
  }

  /**
   * Make sure the include list is valid.
   * @param includes
   */
  private void validateIncludes(Set<String> includes) throws IllegalArgumentException {
    for (String inc : includes) {
      if (!registry.isInternalRelationship(entityClass, inc)
        && !registry.isRelationExternal(entityClass, inc)) {
        throw new IllegalArgumentException("Invalid include");
      }
    }
  }

  /**
   * From a list of included relationships, add the nested attributes
   * using the name of the relationship as prefix.
   *
   * @param attributes current attribute set (new attributes to be added to it)
   * @param includes list of relationship included (could be internal or external)
   */
  private void addNestedAttributesFromIncludes(Set<String> attributes, Set<String> includes) {
    for (String inc : includes) {
      Class<?> c = registry.getInternalRelationClass(entityClass, inc);
      // external relationship would return null
      if (c != null) {
        Set<String> ca = registry.getAttributesPerClass().get(c);
        if (ca != null) {
          for (String cal : ca) {
            attributes.add(inc + "." + cal);
          }
        }
      }
    }
  }

  private static int toSafePageOffset(Integer pageOffset) {
    if (pageOffset == null || pageOffset <= 0) {
      return 0;
    }
    return pageOffset;
  }

  private static int toSafePageLimit(Integer pageLimit) {
    if (pageLimit == null || pageLimit <= 0) {
      return DEFAULT_PAGE_LIMIT;
    }

    if (pageLimit > MAX_PAGE_LIMIT) {
      return DEFAULT_PAGE_LIMIT;
    }
    return pageLimit;
  }

  /**
   *
   * @param pageOffset
   * @param pageLimit
   * @param totalCount
   * @param resourceList
   * @param <D>
   */
  public record PagedResource<D>(int pageOffset, int pageLimit, int totalCount,
                                 List<D> resourceList) {
  }
}
