package ca.gc.aafc.dina.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;

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
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
public class DinaRepositoryV2<D extends JsonApiResource,E extends DinaEntity> {

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
  protected final JsonApiDtoAssistant<D> jsonApiDtoAssistant;

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

    // configure an assistant for this specific resource
    this.jsonApiDtoAssistant = new JsonApiDtoAssistant<>(registry,
      this::externalRelationDtoToJsonApiExternalResource, resourceClass);

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
   * Override this method to generate link to newly created resource.
   * Example return linkTo(methodOn(PersonRepositoryV2.class).onFindOne(dto.getUuid(), null)
   * import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
   * @param dto
   * @return
   */
  protected Link generateLinkToResource(D dto) {
    return Link.of(dto.getJsonApiType() + "/" + dto.getJsonApiId().toString());
  }

  /**
   * Handles findOne at the Spring hateoas level.
   * @param id
   * @param req
   * @return
   * @throws ResourceNotFoundException
   */
  public ResponseEntity<RepresentationModel<?>> handleFindOne(UUID id, HttpServletRequest req)
      throws ResourceNotFoundException {

    String queryString = req != null ? decodeQueryString(req) : null;

    JsonApiDto<D> jsonApiDto = getOne(id, queryString);

    JsonApiModelBuilder builder = createJsonApiModelBuilder(jsonApiDto);

    return ResponseEntity.ok(builder.build());
  }

  /**
   * Handles findAll at the Spring hateoas level.
   * @param req used for query string
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleFindAll(HttpServletRequest req) {
    String queryString = req != null ? decodeQueryString(req) : null;

    PagedResource<JsonApiDto<D>> dtos;
    try {
      dtos = getAll(queryString);
    } catch (IllegalArgumentException iaEx) {
      return ResponseEntity.badRequest().build();
    }

    JsonApiModelBuilder builder = createJsonApiModelBuilder(dtos);

    return ResponseEntity.ok(builder.build());
  }

  /**
   * Handles create at the Spring hateoas level.
   * @param postedDocument
   * @param dtoCustomizer
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleCreate(JsonApiDocument postedDocument,
                                                             Consumer<D> dtoCustomizer)
      throws ResourceNotFoundException {

    if (postedDocument == null) {
      return ResponseEntity.badRequest().build();
    }

    UUID uuid = create(postedDocument, dtoCustomizer);

    // reload dto
    JsonApiDto<D> jsonApiDto = getOne(uuid, null);
    JsonApiModelBuilder builder = createJsonApiModelBuilder(jsonApiDto);
    builder.link(generateLinkToResource(jsonApiDto.getDto()).withSelfRel());

    RepresentationModel<?> model = builder.build();
    URI uri = model.getRequiredLink(IanaLinkRelations.SELF).toUri();

    return ResponseEntity.created(uri).body(model);
  }

  /**
   * Handles update at the Spring hateoas level.
   * @param partialPatchDto
   * @param id
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleUpdate(JsonApiDocument partialPatchDto,
                                                             UUID id) throws ResourceNotFoundException {
    // Sanity check
    if (!Objects.equals(id, partialPatchDto.getId())) {
      return ResponseEntity.badRequest().build();
    }

    update(partialPatchDto);

    // reload dto
    JsonApiDto<D> jsonApiDto = getOne(partialPatchDto.getId(), null);
    JsonApiModelBuilder builder = createJsonApiModelBuilder(jsonApiDto);

    return ResponseEntity.ok().body(builder.build());
  }

  /**
   * Handles delete at the Spring hateoas level.
   * @param id
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleDelete(UUID id) throws ResourceNotFoundException {
    delete(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Handles findOne at the {@link JsonApiDto} level.
   * Responsible to call the service, apply authorization run mapper and build {@link JsonApiDto}.
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

    return jsonApiDtoAssistant.toJsonApiDto(dto, includes);
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
      dtos.add(jsonApiDtoAssistant.toJsonApiDto(dinaMapper.toDto(e, attributes, null), includes));
    }

    Long resourceCount = dinaService.getResourceCount( entityClass,
      (criteriaBuilder, root, em) -> {
        Predicate restriction = SimpleFilterHandlerV2.getRestriction(root, criteriaBuilder, rsqlArgumentParser::parse, em.getMetamodel(), fc != null ? List.of(fc) : List.of());
        return new Predicate[]{restriction};
      });

    return new PagedResource<>(pageOffset, pageLimit, resourceCount.intValue(), dtos);
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

    JsonApiModelBuilder builder = JsonApiModelBuilderHelper.
      createJsonApiModelBuilder(jsonApiDto, mainBuilder, included);
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
      JsonApiModelBuilder builder = JsonApiModelBuilderHelper.
        createJsonApiModelBuilder(currResource, mainBuilder, included);
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
    if (dtoCustomizer != null) {
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
