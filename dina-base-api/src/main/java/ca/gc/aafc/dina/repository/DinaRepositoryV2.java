package ca.gc.aafc.dina.repository;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tennaito.rsql.misc.ArgumentParser;
import com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder;

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
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.security.auth.DinaAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaService;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.Predicate;
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
                          ExternalResourceProvider externalResourceProvider,
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
   * TODO return relationships loaded
   * @param identifier
   * @param queryString
   * @return
   */
  public JsonApiDto<D> getOne(UUID identifier, String queryString) {

    // the only part of QueryComponent that can be used on getOne is "includes"
    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    Set<String> includes = queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of();

    E entity = dinaService.findOne(identifier, entityClass, includes);

    authorizationService.authorizeRead(entity);

    Set<String> attributes = new HashSet<>(registry.getAttributesPerClass().get(entityClass));
    attributes.addAll(includes);

    D dto = dinaMapper.toDto(entity, attributes);

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

  private static void handleToOneRelationship(JsonApiDto.JsonApiDtoBuilder<?> builder, String name,
                                              Object rel) {
    if (rel instanceof JsonApiResource ddto) {
      builder.relationship(name,
        JsonApiDto.RelationshipToOne.builder()
          .included(ddto).build());
    } else {
      log.warn("Not an instance of JsonApiResource, ignoring {}", name);
    }
  }

  private static void handleToManyRelationship(JsonApiDto.JsonApiDtoBuilder<?> builder, String name,
                                               Collection<?> rel) {

    List<JsonApiResource> castSafeList = new ArrayList<>(rel.size());
    for (Object element : rel) {
      if (element instanceof JsonApiResource jar) {
        castSafeList.add(jar);
      } else {
        log.warn("Not an instance of JsonApiResource, ignoring {}", name);
      }
    }

    builder.relationship(name,
      JsonApiDto.RelationshipToMany.builder()
        .included(castSafeList).build());
  }

  protected JsonApiModelBuilder createJsonApiModelBuilder(JsonApiDto<D> jsonApiDto) {
    JsonApiModelBuilder builder = jsonApiModel().model(RepresentationModel.of(jsonApiDto.getDto()));

    Set<UUID> included = new HashSet<>(jsonApiDto.getRelationships().size());
    for(var a : jsonApiDto.getRelationships().entrySet()) {
      if (a.getValue() instanceof JsonApiDto.RelationshipToOne toOne) {
        builder.relationship(a.getKey(), toOne.getIncluded());
        addUniqueIncluded(builder, toOne.getIncluded(), included);
      } else if (a.getValue() instanceof JsonApiDto.RelationshipToMany toMany) {
        builder.relationship(a.getKey(), toMany.getIncluded());
        for(var includedResource: toMany.getIncluded()){
          addUniqueIncluded(builder, includedResource, included);
        }
      }
    }
    return builder;
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

  public PagedResource<D> getAll(String queryString) {
    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    return getAll(queryComponents);
  }

  public PagedResource<D> getAll(QueryComponent queryComponents) {

    FilterComponent fc = queryComponents.getFilters();

    Set<String> relationshipsPath = EntityFilterHelper.extractRelationships(queryComponents.getIncludes(), resourceClass, registry);
    Set<String> includes = queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of();

    List<E> entities = dinaService.findAll(
      entityClass,
      (criteriaBuilder, root, em) -> {
        EntityFilterHelper.leftJoinSortRelations(root, queryComponents.getSorts(), resourceClass, registry);

        Predicate restriction = SimpleFilterHandlerV2.getRestriction(root, criteriaBuilder, rsqlArgumentParser::parse, em.getMetamodel(), fc != null ? List.of(fc) : List.of());
        return new Predicate[]{restriction};
      },
      (cb, root) -> EntityFilterHelper.getOrders(cb, root, queryComponents.getSorts(), false),
      toSafePageOffset(queryComponents.getPageOffset()),
      toSafePageLimit(queryComponents.getPageLimit()),
      includes, relationshipsPath);

    List<D> dtos = new ArrayList<>(entities.size());

    Set<String> attributes = new HashSet<>(registry.getAttributesPerClass().get(entityClass));
    attributes.addAll(queryComponents.getIncludes() != null ? queryComponents.getIncludes() : Set.of());
    for (E e : entities) {
      dtos.add(dinaMapper.toDto(e, attributes));
    }

    Long resourceCount = dinaService.getResourceCount( entityClass,
      (criteriaBuilder, root, em) -> {
        Predicate restriction = SimpleFilterHandlerV2.getRestriction(root, criteriaBuilder, rsqlArgumentParser::parse, em.getMetamodel(), fc != null ? List.of(fc) : List.of());
        return new Predicate[]{restriction};
      });
    return new PagedResource<>(resourceCount.intValue(), dtos);
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

  public record PagedResource<D>(int resourceCount, List<D> resourceList) {
  }
}
