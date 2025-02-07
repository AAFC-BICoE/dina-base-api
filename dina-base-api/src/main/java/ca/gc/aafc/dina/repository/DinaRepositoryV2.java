package ca.gc.aafc.dina.repository;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tennaito.rsql.misc.ArgumentParser;
import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.JsonApiExternalResource;
import ca.gc.aafc.dina.dto.JsonApiMeta;
import ca.gc.aafc.dina.dto.JsonApiResource;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.filter.DinaFilterArgumentParser;
import ca.gc.aafc.dina.filter.EntityFilterHelper;
import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.filter.SimpleFilterHandlerV2;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.mapper.DinaMapperV2;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.security.auth.DinaAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.util.ReflectionUtils;

import static com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder.jsonApiModel;

import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

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
  private final BuildProperties buildProperties;

  protected final DinaMappingRegistry registry;

  protected ObjectMapper objMapper;
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
    this.buildProperties = buildProperties;

    // build registry instance for resource class (dto)
    this.registry = new DinaMappingRegistry(resourceClass);

    // copy the object mapper and set it to fail on unknown properties
    this.objMapper = objMapper.copy()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
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
   * @return the DTO wrapped in a {@link JsonApiDto} or null if not found
   */
  public JsonApiDto<D> getOne(UUID identifier, String queryString) throws ResourceNotFoundException {

    // the only part of QueryComponent that can be used on getOne is "includes"
    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    Set<String> includes = queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of();

    validateIncludes(includes);

    E entity = dinaService.findOne(identifier, entityClass, includes);
    if (entity == null) {
      throw ResourceNotFoundException.create(resourceClass.getSimpleName(), identifier);
    }

    authorizationService.authorizeRead(entity);

    Set<String> attributes = new HashSet<>(registry.getAttributesPerClass().get(entityClass));
    attributes.addAll(includes);
    addNestedAttributesFromIncludes(attributes, includes);

    D dto = dinaMapper.toDto(entity, attributes, null);

    return toJsonApiDto(dto, includes);
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
          .included(externalRelationDtoToJsonApiExternalResource(erd)).build());
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
          castSafeListExternal.add(externalRelationDtoToJsonApiExternalResource(erd));
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

    Class<?> internalRelClass = registry.getInternalRelationClass(resourceClass, relationshipName);
    if (internalRelClass != null) {
      if (DinaMappingRegistry.isCollection(internalRelClass)) {
        builder.relationship(relationshipName,
          JsonApiDto.RelationshipToMany.builder().build());
      } else {
        builder.relationship(relationshipName,
          JsonApiDto.RelationshipToOne.builder().build());
      }
    } else {
      Class<?> relClass = registry.getExternalRelationClass(resourceClass, relationshipName);
      if (DinaMappingRegistry.isCollection(relClass)) {
        builder.relationship(relationshipName,
          JsonApiDto.RelationshipManyExternal.builder().build());
      } else {
        builder.relationship(relationshipName,
          JsonApiDto.RelationshipToOneExternal.builder().build());
      }
    }
  }

  /**
   * Responsible to create the {@link JsonApiModelBuilder} for the provided {@link JsonApiDto}.
   *
   * @param jsonApiDto
   * @return
   */
  protected JsonApiModelBuilder createJsonApiModelBuilder(JsonApiDto<D> jsonApiDto) {
    Set<UUID> included = new HashSet<>(jsonApiDto.getRelationships().size());

    JsonApiModelBuilder mainBuilder = jsonApiModel();

    JsonApiModelBuilder builder = createJsonApiModelBuilder(jsonApiDto, mainBuilder, included);
    JsonApiMeta.builder()
      .moduleVersion(buildProperties.getVersion())
      .build()
      .populateMeta(mainBuilder::meta);
    mainBuilder.model(builder.build());
    return mainBuilder;
  }

  /**
   * Same as {@link #createJsonApiModelBuilder(JsonApiDto)} but for pages resource.
   * @param jsonApiDtos
   * @return
   */
  protected JsonApiModelBuilder createJsonApiModelBuilder(PagedResource<JsonApiDto<D>> jsonApiDtos) {

    JsonApiModelBuilder mainBuilder = jsonApiModel();
    List<RepresentationModel<?>> repModels = new ArrayList<>();
    Set<UUID> included = new HashSet<>();
    for (JsonApiDto<D> currResource : jsonApiDtos.resourceList()) {
      JsonApiModelBuilder builder = createJsonApiModelBuilder(currResource, mainBuilder, included);
      repModels.add(builder.build());
    }

    // use custom metadata instead of PagedModel.PageMetadata so we can control
    // the content and key names
    JsonApiMeta.builder()
      .totalResourceCount(jsonApiDtos.totalCount)
      .moduleVersion(buildProperties.getVersion())
      .build()
      .populateMeta(mainBuilder::meta);

    mainBuilder.model(CollectionModel.of(repModels));
    return mainBuilder;
  }

  /**
   * Internal method to create {@link JsonApiModelBuilder}.
   * @param jsonApiDto
   * @param includeBuilder builder to use to add "included" documents. if null, the builder
   *                       created for that document will be used.
   * @param included set of already included uuid
   * @return
   */
  private JsonApiModelBuilder createJsonApiModelBuilder(JsonApiDto<D> jsonApiDto,
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
   * Create a new resource.
   * @param docToCreate
   * @param dtoCustomizer used to customize the dto before being transformed to entity.
   *                      Example, setting the authenticated user as createdBy. Can be null.
   * @return the uuid assigned or used
   */
  public UUID create(JsonApiDocument docToCreate, Consumer<D> dtoCustomizer) {

    D dto = objMapper.convertValue(docToCreate.getAttributes(), resourceClass);
    if(dtoCustomizer != null) {
      dtoCustomizer.accept(dto);
    }

    E entity = dinaMapper.toEntity(dto,
      registry.getAttributesPerClass().get(resourceClass),
      null);

    updateRelationships(entity, docToCreate.getRelationships());

    authorizationService.authorizeCreate(entity);
    E created = dinaService.create(entity);
    return created.getUuid();
  }

  /**
   * Update the resource defined by the id in {@link JsonApiDocument} with the provided
   * attributes.
   * @param patchDto
   */
  public void update(JsonApiDocument patchDto) throws ResourceNotFoundException {

    // We need to use Jackson for now here since MapStruct doesn't support setting
    // values from Map<String, Object> yet.
    // Reflection can't really be used since we won't know the type of the source
    // and how to convert it.
    D dto = objMapper.convertValue(patchDto.getAttributes(), resourceClass);

    // load entity
    E entity = dinaService.findOne(patchDto.getId(), entityClass);
    if (entity == null) {
      throw ResourceNotFoundException.create(resourceClass.getSimpleName(), patchDto.getId());
    }

    // Check for authorization on the entity
    authorizationService.authorizeUpdate(entity);

    // apply DTO on entity using the keys from patchDto
    dinaMapper.patchEntity(entity, dto, patchDto.getData().getAttributesName(), null);

    updateRelationships(entity, patchDto.getRelationships());

    dinaService.update(entity);
  }

  /**
   * Update the relationships with the ones provided.
   * If defined in relationships map, the relationships will be <b>replaced</b> by the one(s) provided.
   * If null is provided as value for a relationship, the relationship will be removed.
   * @param entity the entity from which the relationships should be updated
   * @param relationships the relationships to update
   */
  private void updateRelationships(E entity, Map<String, JsonApiDocument.RelationshipObject> relationships) {

    if (relationships == null) {
      return;
    }

    for (var relationship : relationships.entrySet()) {
      String relName = relationship.getKey();

      // get information about the relationship
      DinaMappingRegistry.InternalRelation relation = registry.getInternalRelation(entityClass, relName);
      if (relation == null) {
        throw new IllegalArgumentException("Unknown relationship [" + relName + "]");
      }

      JsonApiDocument.RelationshipObject relObject = relationship.getValue();
      // we are keeping a (or a list of) Hibernate reference to the relationship instead of a complete object.
      Object relationshipsReference;

      if (!relObject.isNull()) {
        // to-many
        if (relObject.isCollection()) {
          List<Object> relationshipsReferences = new ArrayList<>();
          for (Object el : relObject.getDataAsCollection()) {
            var resourceIdentifier = toResourceIdentifier(el);
            if (resourceIdentifier != null) {
              relationshipsReferences.add(
                dinaService.getReferenceByNaturalId(relation.getEntityType(),
                  resourceIdentifier.getId()));
            } else {
              log.warn("Can't convert to ResourceIdentifier list element, ignoring");
              return;
            }
          }
          relationshipsReference = relationshipsReferences;
        } else { // to-one
          var resourceIdentifier = toResourceIdentifier(relObject.getData());
          if (resourceIdentifier != null) {
            relationshipsReference = dinaService.getReferenceByNaturalId(relation.getEntityType(),
              resourceIdentifier.getId());
          } else {
            log.warn("Can't convert to ResourceIdentifier, ignoring");
            return;
          }
        }
      } else {
        // remove relationship
        relationshipsReference = null;
      }

      try {
        ReflectionUtils.getSetterMethod(relName, entityClass).invoke(entity, relationshipsReference);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Convert the provided Object to {@link JsonApiDocument.ResourceIdentifier}
   * @param obj
   * @return
   */
  private JsonApiDocument.ResourceIdentifier toResourceIdentifier(Object obj) {
    return objMapper.convertValue(obj, JsonApiDocument.ResourceIdentifier.class);
  }

  /**
   * Delete the resource identified by the provided identifier.
   *
   * @param identifier
   */
  public void delete(UUID identifier) throws ResourceNotFoundException {
    E entity = dinaService.findOne(identifier, entityClass);
    if (entity == null) {
      throw ResourceNotFoundException.create(resourceClass.getSimpleName(), identifier);
    }
    authorizationService.authorizeDelete(entity);
    dinaService.delete(entity);
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
