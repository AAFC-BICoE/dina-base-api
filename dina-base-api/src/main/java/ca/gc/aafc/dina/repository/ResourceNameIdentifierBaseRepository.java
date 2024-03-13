package ca.gc.aafc.dina.repository;

import java.util.Map;
import java.util.UUID;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.dto.ResourceNameIdentifierDto;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.FilterExpression;
import ca.gc.aafc.dina.filter.FilterGroup;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.service.ResourceNameIdentifierService;

/**
 * Base repository that can be subclassed to support find-by-name using a query string.
 */
public class ResourceNameIdentifierBaseRepository {

  private final ResourceNameIdentifierService resourceNameIdentifierService;
  private final Map<String, Class< ? extends DinaEntity>> typeToEntity;

  public ResourceNameIdentifierBaseRepository(ResourceNameIdentifierService resourceNameIdentifierService,
                                              Map<String, Class<? extends DinaEntity>> typeToEntity) {

    this.resourceNameIdentifierService = resourceNameIdentifierService;
    this.typeToEntity = typeToEntity;
  }

  public UUID findOne(String queryString) {

    QueryComponent queryComponents = QueryStringParser.parse(queryString);

    FilterGroup fg = queryComponents.getFilterGroup().orElseThrow();
    ResourceNameIdentifierDto.ResourceNameIdentifierDtoBuilder builder = ResourceNameIdentifierDto.builder();
    for (FilterComponent fc : fg.getComponents()) {
      if (fc instanceof FilterExpression fex) {
        buildResourceNameIdentifierDto(fex, builder);
      }
    }

    ResourceNameIdentifierDto resourceNameIdentifierDto = builder.build();
    return resourceNameIdentifierService
      .findByName(typeToEntity.get(resourceNameIdentifierDto.getType()), resourceNameIdentifierDto.getName(), resourceNameIdentifierDto.getGroup());
  }


  private void buildResourceNameIdentifierDto(FilterExpression fex, ResourceNameIdentifierDto.ResourceNameIdentifierDtoBuilder builder) {
    if (fex.operator() != Ops.EQ ) {
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
