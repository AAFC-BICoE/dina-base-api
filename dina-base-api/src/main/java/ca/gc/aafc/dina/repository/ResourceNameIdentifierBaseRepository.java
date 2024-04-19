package ca.gc.aafc.dina.repository;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.dto.ResourceNameIdentifierRequestDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.FilterExpression;
import ca.gc.aafc.dina.filter.FilterGroup;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.security.auth.GroupAuth;
import ca.gc.aafc.dina.security.auth.GroupWithReadAuthorizationService;
import ca.gc.aafc.dina.service.NameUUIDPair;
import ca.gc.aafc.dina.service.ResourceNameIdentifierService;

import static java.util.stream.Collectors.groupingBy;

/**
 * Base repository that can be subclassed to support find-by-name using a query string.
 */
public class ResourceNameIdentifierBaseRepository {

  private final ResourceNameIdentifierService resourceNameIdentifierService;
  private final GroupWithReadAuthorizationService authorizationService;
  private final Map<String, Class< ? extends DinaEntity>> typeToEntity;

  public ResourceNameIdentifierBaseRepository(ResourceNameIdentifierService resourceNameIdentifierService,
                                              GroupWithReadAuthorizationService authorizationService,
                                              Map<String, Class<? extends DinaEntity>> typeToEntity) {

    this.resourceNameIdentifierService = resourceNameIdentifierService;
    this.authorizationService = authorizationService;
    this.typeToEntity = typeToEntity;
  }

  /**
   * Find an Identifier (UUID) based on the name.
   * This class assumes there can be only 1 or 0 records matching.
   *
   * @param queryString
   * @return the pair name/uuid.
   */
  public NameUUIDPair findOne(String queryString) throws IllegalArgumentException {

    QueryComponent queryComponents = QueryStringParser.parse(queryString);

    FilterGroup fg = queryComponents.getFilterGroup().orElseThrow(IllegalArgumentException::new);
    ResourceNameIdentifierRequestDto.ResourceNameIdentifierRequestDtoBuilder builder = ResourceNameIdentifierRequestDto.builder();
    for (FilterComponent fc : fg.getComponents()) {
      if (fc instanceof FilterExpression fex) {
        buildResourceNameIdentifierDto(fex, builder);
      }
    }

    ResourceNameIdentifierRequestDto resourceNameIdentifierDto = builder.build();

    // Make sure the group is specified
    if(StringUtils.isBlank(resourceNameIdentifierDto.getGroup())) {
      throw new IllegalArgumentException("group should be provided");
    }

    authorizationService.authorizeRead(GroupAuth.of(resourceNameIdentifierDto.getGroup()));

    return NameUUIDPair.builder().name(resourceNameIdentifierDto.getSingleName()).uuid(resourceNameIdentifierService
      .findByName(typeToEntity.get(resourceNameIdentifierDto.getType()), resourceNameIdentifierDto.getSingleName(), resourceNameIdentifierDto.getGroup()))
      .build();
  }

  /**
   * Find all identifiers for all the provided names.
   * @param queryString
   * @return list of pair name/uuid
   */
  public List<NameUUIDPair> findAll(String queryString) throws IllegalArgumentException {

    QueryComponent queryComponents = QueryStringParser.parse(queryString);

    FilterGroup fg = queryComponents.getFilterGroup().orElseThrow(IllegalArgumentException::new);
    ResourceNameIdentifierRequestDto.ResourceNameIdentifierRequestDtoBuilder builder = ResourceNameIdentifierRequestDto.builder();


    for (FilterComponent fc : fg.getComponents()) {
      if (fc instanceof FilterExpression fex) {
        buildResourceNameIdentifierDto(fex, builder);
      } else if( fc instanceof FilterGroup fgrp) {
        // multiple values can be submitted with en EQUALS to create an OR.
        // if it's the case, we change it to an IN internally
        if(fgrp.getConjunction() == FilterGroup.Conjunction.OR ) {
          extractExpressionForInClause(fgrp.getComponents(), builder);
        }
      }
    }

    ResourceNameIdentifierRequestDto resourceNameIdentifierDto = builder.build();

    // Make sure the group is specified
    if(StringUtils.isBlank(resourceNameIdentifierDto.getGroup())) {
      throw new IllegalArgumentException("group should be provided");
    }
    authorizationService.authorizeRead(GroupAuth.of(resourceNameIdentifierDto.getGroup()));

    // if a list of names is provided
    if (resourceNameIdentifierDto.getNames() != null && !resourceNameIdentifierDto.getNames().isEmpty()) {
      return resourceNameIdentifierService
        .findAllByNames(typeToEntity.get(resourceNameIdentifierDto.getType()),
          resourceNameIdentifierDto.getNames(), resourceNameIdentifierDto.getGroup());
    }


    // else list all of them
    return resourceNameIdentifierService
      .listNameUUIDPair(typeToEntity.get(resourceNameIdentifierDto.getType()),
        resourceNameIdentifierDto.getGroup(),
        ObjectUtils.defaultIfNull(queryComponents.getPageOffset(), -1),
        ObjectUtils.defaultIfNull(queryComponents.getPageLimit(), -1));
  }

  /**
   * From a component list, try to create an IN based on multiple expressions using OR on the same attribute.
   *
   * @param components
   * @param builder
   */
  private void extractExpressionForInClause(List<FilterComponent> components,
                                                    ResourceNameIdentifierRequestDto.ResourceNameIdentifierRequestDtoBuilder builder) {
    // sanity checks
    // make sure the components are all FilerExpression instances.
    List<FilterExpression> expressions =
      components.stream()
        .filter(c -> c instanceof FilterExpression)
        .map(c -> (FilterExpression) c)
        .filter(f -> f.operator() == Ops.EQ).toList();
    if (components.size() != expressions.size()) {
      return;
    }

    // group them by attribute
    Map<String, List<FilterExpression>> byAttribute = expressions.stream()
      .collect(groupingBy(FilterExpression::attribute));

    // make sure only 1 attribute is used
    if(byAttribute.keySet().size() != 1) {
      return;
    }

    List<String> allValues =
      byAttribute.values().stream().flatMap(fe -> fe.stream().map(FilterExpression::value)).toList();

    for(String v : allValues) {
      builder.name(v);
    }
  }

  private void buildResourceNameIdentifierDto(FilterExpression fex, ResourceNameIdentifierRequestDto.ResourceNameIdentifierRequestDtoBuilder builder) {
    if (fex.operator() != Ops.EQ) {
      return;
    }

    switch (fex.attribute()) {
      case "type" -> builder.type(fex.value());
      case "name" -> builder.name(fex.value());
      case "group" -> builder.group(fex.value());
      default -> { } //no-op
    }
  }

}
