package ca.gc.aafc.dina.repository;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tennaito.rsql.misc.ArgumentParser;
import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.dto.JsonApiDtoMeta;
import ca.gc.aafc.dina.dto.JsonApiExternalResource;
import ca.gc.aafc.dina.dto.JsonApiResource;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.exception.ResourceGoneException;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.exception.ResourcesGoneException;
import ca.gc.aafc.dina.exception.ResourcesNotFoundException;
import ca.gc.aafc.dina.filter.DinaFilterArgumentParser;
import ca.gc.aafc.dina.filter.EntityFilterHelper;
import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.filter.SimpleFilterHandlerV2;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkResourceIdentifierDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.mapper.DinaMapperV2;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.repository.auditlog.AuditSnapshotRepository;
import ca.gc.aafc.dina.security.auth.DinaAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaService;
import ca.gc.aafc.dina.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
public class DinaRepositoryV2<D extends JsonApiResource, E extends DinaEntity>
  implements DinaRepositoryLayer<UUID, D> {

  public static final String JSON_API_BULK = "application/vnd.api+json; ext=bulk";
  public static final String INCLUDE_PERMISSION_HEADER_KEY = "include-dina-permission";

  public static final String JSON_API_BULK_PATH = "bulk";
  public static final String JSON_API_BULK_LOAD_PATH = "bulk-load";

  // default page limit/page size
  private static final int DEFAULT_PAGE_LIMIT = 20;
  private static final int MAX_PAGE_LIMIT = 100;

  private final DinaAuthorizationService authorizationService;
  private final AuditService auditService;
  private final DinaService<E> dinaService;
  private final Class<E> entityClass;

  private final Class<D> resourceClass;
  private final String jsonApiType;

  private final DinaMapperV2<D, E> dinaMapper;
  private final BuildProperties buildProperties;

  protected final DinaMappingRegistry registry;

  protected final JsonApiDtoAssistant<D> jsonApiDtoAssistant;
  protected final JsonApiModelAssistant<D> jsonApiModelAssistant;

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
    this(dinaService, authorizationService, auditService, dinaMapper,
      resourceClass, entityClass, buildProperties, objMapper,

      // build registry instance for resource class (dto)
      new DinaMappingRegistry(resourceClass));
  }

  public DinaRepositoryV2(@NonNull DinaService<E> dinaService,
                          @NonNull DinaAuthorizationService authorizationService,
                          @NonNull Optional<AuditService> auditService,
                          @NonNull DinaMapperV2<D, E> dinaMapper,
                          @NonNull Class<D> resourceClass,
                          @NonNull Class<E> entityClass,
                          @NonNull BuildProperties buildProperties,
                          ObjectMapper objMapper, DinaMappingRegistry registry) {

    this.authorizationService = authorizationService;
    this.auditService = auditService.orElse(null);
    this.dinaService = dinaService;
    this.entityClass = entityClass;
    this.resourceClass = resourceClass;

    JsonApiTypeForClass annotation = resourceClass.getAnnotation(JsonApiTypeForClass.class);
    if (annotation != null) {
      jsonApiType = annotation.value();
    } else {
      jsonApiType = null;
    }

    this.dinaMapper = dinaMapper;
    this.buildProperties = buildProperties;
    this.registry = registry;

    // configure an assistant for this specific resource
    this.jsonApiDtoAssistant = new JsonApiDtoAssistant<>(registry,
      this::externalRelationDtoToJsonApiExternalResource, resourceClass);

    this.jsonApiModelAssistant = new JsonApiModelAssistant<>(buildProperties.getVersion());

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
    return Link.of(
      Objects.toString(dto.getJsonApiType(), "") + "/" + Objects.toString(dto.getJsonApiId(), ""));
  }

  /**
   * Handles bulk load.
   * @param jsonApiBulkDocument
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleBulkLoad(JsonApiBulkResourceIdentifierDocument jsonApiBulkDocument,
                                                               HttpServletRequest req)
      throws ResourcesNotFoundException, ResourcesGoneException {

    String queryString = req != null ? decodeQueryString(req) : null;
    List<JsonApiDto<D>> dtos = new ArrayList<>();

    // initialize to null since it won't be used most of the time
    List<String> resourcesNotFound = null;
    Map<String, String> resourcesGone = null;

    for (var data : jsonApiBulkDocument.getData()) {
      try {
        dtos.add(getOne(data.getId(), queryString));
      } catch (ResourceNotFoundException exNotFound) {
        if (resourcesNotFound == null) {
          resourcesNotFound = new ArrayList<>();
        }
        resourcesNotFound.add(exNotFound.getIdentifier());
      } catch (ResourceGoneException exGone) {
        if (resourcesGone == null) {
          resourcesGone = new HashMap<>();
        }
        resourcesGone.put(exGone.getIdentifier(), exGone.getLink());
      }
    }

    // errors handling
    if (resourcesNotFound != null) {
      throw ResourcesNotFoundException.create(jsonApiType, resourcesNotFound);
    }

    if (resourcesGone != null) {
      throw ResourcesGoneException.create(jsonApiType, resourcesGone);
    }

    JsonApiModelBuilder builder = jsonApiModelAssistant.createJsonApiModelBuilder(dtos, null);

    return ResponseEntity.ok().body(builder.build());
  }

  /**
   * Handles findOne at the Spring hateoas level.
   * @param id
   * @param req
   * @return
   * @throws ResourceNotFoundException
   */
  public ResponseEntity<RepresentationModel<?>> handleFindOne(UUID id, HttpServletRequest req)
      throws ResourceNotFoundException, ResourceGoneException {

    String queryString = req != null ? decodeQueryString(req) : null;
    boolean includePermission = req != null && req.getHeader(INCLUDE_PERMISSION_HEADER_KEY) != null;

    JsonApiDto<D> jsonApiDto = getOne(id, queryString, includePermission);
    JsonApiModelBuilder builder = jsonApiModelAssistant.createJsonApiModelBuilder(jsonApiDto);

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

    JsonApiModelBuilder builder = jsonApiModelAssistant.createJsonApiModelBuilder(dtos);

    return ResponseEntity.ok(builder.build());
  }

  /**
   * Handles bulk updates.
   * @param jsonApiBulkDocument
   * @param dtoCustomizer
   * @throws ResourceNotFoundException
   */
  public ResponseEntity<RepresentationModel<?>> handleBulkCreate(JsonApiBulkDocument jsonApiBulkDocument,
                               Consumer<D> dtoCustomizer) {

    List<JsonApiDto<D> > dtos = new ArrayList<>();
    for (var data : jsonApiBulkDocument.getData()) {
      dtos.add(create(JsonApiDocument.builder().data(data).build(), dtoCustomizer));
    }

    JsonApiModelBuilder builder = jsonApiModelAssistant.createJsonApiModelBuilder(dtos, null);

    return ResponseEntity.ok().body(builder.build());
  }

  /**
   * Handles create at the Spring hateoas level.
   * @param postedDocument
   * @param dtoCustomizer
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleCreate(JsonApiDocument postedDocument,
                                                             Consumer<D> dtoCustomizer) {

    if (postedDocument == null) {
      return ResponseEntity.badRequest().build();
    }

    JsonApiDto<D> jsonApiDto = create(postedDocument, dtoCustomizer);
    JsonApiModelBuilder builder = jsonApiModelAssistant.createJsonApiModelBuilder(jsonApiDto);
    builder.link(generateLinkToResource(jsonApiDto.getDto()));

    RepresentationModel<?> model = builder.build();
    URI uri = model.getRequiredLink(IanaLinkRelations.SELF).toUri();

    return ResponseEntity.created(uri).body(model);
  }

  /**
   * Handles bulk updates.
   * @param jsonApiBulkDocument
   * @return
   * @throws ResourceNotFoundException
   */
  public ResponseEntity<RepresentationModel<?>> handleBulkUpdate(JsonApiBulkDocument jsonApiBulkDocument)
      throws ResourceNotFoundException, ResourceGoneException {
    List<JsonApiDto<D> > dtos = new ArrayList<>();
    for (var data : jsonApiBulkDocument.getData()) {
      dtos.add(update(JsonApiDocument.builder().data(data).build()));
    }

    JsonApiModelBuilder builder = jsonApiModelAssistant.createJsonApiModelBuilder(dtos, null);
    return ResponseEntity.ok().body(builder.build());
  }

  /**
   * Handles update at the Spring hateoas level.
   * @param partialPatchDto
   * @param id
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleUpdate(JsonApiDocument partialPatchDto,
                                                             UUID id)
      throws ResourceNotFoundException, ResourceGoneException {

    // Sanity check
    if (!Objects.equals(id, partialPatchDto.getId())) {
      return ResponseEntity.badRequest().build();
    }

    update(partialPatchDto);

    // reload dto
    JsonApiDto<D> jsonApiDto = getOne(partialPatchDto.getId(), null);
    JsonApiModelBuilder builder = jsonApiModelAssistant.createJsonApiModelBuilder(jsonApiDto);

    return ResponseEntity.ok().body(builder.build());
  }

  /**
   * Handles bulk deletes.
   * @param jsonApiBulkDocument
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleBulkDelete(JsonApiBulkResourceIdentifierDocument jsonApiBulkDocument)
      throws ResourceNotFoundException, ResourceGoneException {
    for (var data : jsonApiBulkDocument.getData()) {
      delete(data.getId());
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * Handles delete at the Spring hateoas level.
   * @param id
   * @return
   */
  public ResponseEntity<RepresentationModel<?>> handleDelete(UUID id)
      throws ResourceNotFoundException, ResourceGoneException {
    delete(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * see {@link #getOne(UUID, String, boolean)}
   * @param identifier
   * @param queryString
   * @return
   * @throws ResourceNotFoundException
   * @throws ResourceGoneException
   */
  public JsonApiDto<D> getOne(UUID identifier, String queryString) throws ResourceNotFoundException,
      ResourceGoneException {
    return getOne(identifier, queryString, false);
  }

  /**
   * Handles findOne at the {@link JsonApiDto} level.
   * Responsible to call the service, apply authorization run mapper and build {@link JsonApiDto}.
   * @param identifier
   * @param queryString
   * @param includePermissions should the metadata about permission be included ?
   * @return the DTO wrapped in a {@link JsonApiDto} or null if not found
   */
  public JsonApiDto<D> getOne(UUID identifier, String queryString, boolean includePermissions) throws ResourceNotFoundException,
      ResourceGoneException {

    // the only parts of QueryComponent that can be used on getOne is "includes" and "fields"
    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    Set<String> includes = queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of();
    Map<String, List<String>> fields = queryComponents.getFields();

    validateIncludes(includes);

    E entity = dinaService.findOne(identifier, entityClass, includes);

    // Throw not found or gone exceptions if required.
    handleEntityAuditExceptions(entity, identifier);

    authorizationService.authorizeRead(entity);

    Set<String> attributes = new HashSet<>(registry.getAttributesPerClass().get(entityClass));
    attributes.addAll(includes);
    addNestedAttributesFromIncludes(attributes, includes);

    D dto = dinaMapper.toDto(entity, attributes, null);

    if (includePermissions) {
      return jsonApiDtoAssistant.toJsonApiDto(dto, buildResourceObjectPermissionMeta(entity),
        fields, includes);
    }

    return jsonApiDtoAssistant.toJsonApiDto(dto, fields, includes);
  }

  public PagedResource<JsonApiDto<D>> getAll(String queryString) {
    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    return getAll(queryComponents);
  }

  public PagedResource<JsonApiDto<D>> getAll(QueryComponent queryComponents) {

    FilterComponent fc = queryComponents.getFilters();

    Set<String> relationshipsPath = EntityFilterHelper.extractRelationships(queryComponents.getIncludes(), resourceClass, registry);
    Set<String> includes = queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of();
    Map<String, List<String>> fields = queryComponents.getFields();

    validateIncludes(includes);
    int pageOffset = toSafePageOffset(queryComponents.getPageOffset());
    int pageLimit = toSafePageLimit(queryComponents.getPageLimit());

    List<E> entities = dinaService.findAll(
      entityClass,
      (criteriaBuilder, root, em) -> {
        EntityFilterHelper.leftJoinSortRelations(root, queryComponents.getSorts(), resourceClass, registry);

        Predicate restriction = SimpleFilterHandlerV2.createPredicate(root, criteriaBuilder, rsqlArgumentParser::parse, em.getMetamodel(), fc);
        return restriction == null ? null : new Predicate[]{restriction};
      },
      (cb, root) -> EntityFilterHelper.getOrders(cb, root, queryComponents.getSorts(), false),
      pageOffset, pageLimit, includes, relationshipsPath);

    List<JsonApiDto<D>> dtos = new ArrayList<>(entities.size());

    Set<String> attributes = new HashSet<>(registry.getAttributesPerClass().get(entityClass));
    attributes.addAll(queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of());
    addNestedAttributesFromIncludes(attributes, includes);

    for (E e : entities) {
      dtos.add(jsonApiDtoAssistant.toJsonApiDto(dinaMapper.toDto(e, attributes, null), fields, includes));
    }

    Long resourceCount = dinaService.getResourceCount( entityClass,
      (criteriaBuilder, root, em) -> {
        Predicate restriction = SimpleFilterHandlerV2.createPredicate(root, criteriaBuilder, rsqlArgumentParser::parse, em.getMetamodel(), fc);
        return restriction == null ? null : new Predicate[]{restriction};
      });

    return new PagedResource<>(pageOffset, pageLimit, resourceCount.intValue(), dtos);
  }

  private JsonApiDtoMeta buildResourceObjectPermissionMeta(E entity) {
    Set<String> permissions = authorizationService.getPermissionsForObject(entity);

    return JsonApiDtoMeta.builder()
      .permissionsProvider(authorizationService.getName())
      .permissions(permissions)
      .build();
  }

  /**
   * null-safe query string UTF-8 decode function.
   * @param req
   * @return decoded query string or empty string if query string is absent
   */
  public static String decodeQueryString(HttpServletRequest req) {
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

  public static int toSafePageOffset(Integer pageOffset) {
    if (pageOffset == null || pageOffset <= 0) {
      return 0;
    }
    return pageOffset;
  }

  public static int toSafePageLimit(Integer pageLimit) {
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
   * @return freshly reloaded dto of the created resource
   */
  public JsonApiDto<D> create(JsonApiDocument docToCreate, Consumer<D> dtoCustomizer) {

    D dto = objMapper.convertValue(docToCreate.getAttributes(), resourceClass);
    if (dtoCustomizer != null) {
      dtoCustomizer.accept(dto);
    }

    // apply DTO on entity using the keys from docToCreate but remove all immutable fields (if any)
    Set<String> attributesToConsider = new HashSet<>(registry.getAttributesPerClass().get(resourceClass));
    attributesToConsider.removeAll(registry.getAttributeImmutableOnCreatePerClass().get(resourceClass));

    E entity = dinaMapper.toEntity(dto, attributesToConsider, null);

    updateRelationships(entity, docToCreate.getRelationships());

    authorizationService.authorizeCreate(entity);
    E created = dinaService.create(entity);

    // reload dto to make sure calculated values and server generated values are returned
    JsonApiDto<D> reloadedDto;
    try {
      reloadedDto = getOne(created.getUuid(), null);
    } catch (ResourceNotFoundException | ResourceGoneException e) {
      throw new RuntimeException(e);
    }

    if (auditService != null) {
      auditService.audit(reloadedDto.getDto());
    }

    return reloadedDto;
  }

  /**
   * Update the resource defined by the id in {@link JsonApiDocument} with the provided
   * attributes.
   * @param patchDto
   * @return freshly reloaded dto of the updated resource
   */
  public JsonApiDto<D> update(JsonApiDocument patchDto)
      throws ResourceNotFoundException, ResourceGoneException {

    // We need to use Jackson for now here since MapStruct doesn't support setting
    // values from Map<String, Object> yet.
    // Reflection can't really be used since we won't know the type of the source
    // and how to convert it.
    D dto = objMapper.convertValue(patchDto.getAttributes(), resourceClass);

    // load entity
    E entity = dinaService.findOne(patchDto.getId(), entityClass);

    // Throw not found or gone exceptions if required.
    handleEntityAuditExceptions(entity, patchDto.getId());

    // Check for authorization on the entity
    authorizationService.authorizeUpdate(entity);

    // apply DTO on entity using the keys from patchDto but remove all immutable fields (if any)
    Set<String> attributesToPatch = new HashSet<>(patchDto.getData().getAttributesName());
    attributesToPatch.removeAll(registry.getAttributeImmutableOnUpdatePerClass().get(resourceClass));
    dinaMapper.patchEntity(entity, dto, attributesToPatch, null);

    updateRelationships(entity, patchDto.getRelationships());

    dinaService.update(entity);

    // reload dto to make sure calculated values and server generated values are returned
    JsonApiDto<D> reloadedDto = getOne(entity.getUuid(), null);

    if (auditService != null) {
      auditService.audit(reloadedDto.getDto());
    }
    return reloadedDto;
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
  public void delete(UUID identifier) throws ResourceNotFoundException, ResourceGoneException {
    E entity = dinaService.findOne(identifier, entityClass);
    if (entity == null) {
      throw ResourceNotFoundException.create(resourceClass.getSimpleName(), identifier);
    }
    authorizationService.authorizeDelete(entity);

    if (auditService != null) {
      JsonApiDto<D> dto = getOne(identifier, null);
      auditService.auditDeleteEvent(dto.getDto());
    }

    dinaService.delete(entity);
  }

  /**
   * Checks the entity and audit service to determine if the correct exception should be thrown.
   * 
   * No exceptions are thrown if the entity exists.
   * 
   * @param entity The Dina Entity to be checked.
   * @param identifier The UUID for the entity.
   * @throws ResourceNotFoundException if the entity is not found.
   * @throws ResourceGoneException if the entity has been deleted since a terminal snapshot has 
   *    been found.
   */
  private void handleEntityAuditExceptions(E entity, UUID identifier)
      throws ResourceNotFoundException, ResourceGoneException {
    if (entity == null) {
      if (auditService != null) {
        AuditService.AuditInstance auditInstance = AuditService.AuditInstance.builder()
            .id(identifier.toString()).type(jsonApiType).build();
        if (auditService.hasTerminalSnapshot(auditInstance)) {
          throw ResourceGoneException.create(resourceClass.getSimpleName(), identifier,
              AuditSnapshotRepository.generateUrlLink(jsonApiType, identifier.toString()));
        }
      }
      throw ResourceNotFoundException.create(resourceClass.getSimpleName(), identifier);
    }
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
