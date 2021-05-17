package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ValidationRegistry {
  private final Map<String, ValidationEntry> typesToEntryMap = new HashMap<>();

  public ValidationRegistry(@NonNull Set<Class<?>> resources, @NonNull ValidationResourceConfiguration configuration) {
    if (CollectionUtils.isEmpty(resources)) {
      throw new IllegalArgumentException("You must supply a list of dina resources");
    }
    initMaps(resources, configuration);
  }

  private void initMaps(
    Set<Class<?>> resources,
    ValidationResourceConfiguration configuration
  ) {
    resources.forEach(r -> {
      RelatedEntity relatedEntity = r.getAnnotation(RelatedEntity.class);
      JsonApiResource jsonApiResource = r.getAnnotation(JsonApiResource.class);
      DinaMappingRegistry registry = new DinaMappingRegistry(r);
      DinaMapper<?, DinaEntity> mapper = new DinaMapper<>(r, registry);

      String type = jsonApiResource.type();
      DinaService<? extends DinaEntity> dinaService = configuration.getServiceForType(type);

      typesToEntryMap.put(type, ValidationEntry.builder()
        .typeName(type)
        .mappingRegistry(registry)
        .mapper(mapper)
        .entityClass(relatedEntity.value())
        .resourceClass(r)
        .dinaService(dinaService)
        .build());
    });
  }

  public ValidationEntry getEntryForType(String type) {
    return typesToEntryMap.get(type);
  }

  public boolean hasType(String type) {
    return typesToEntryMap.keySet().stream().anyMatch(e -> e.equalsIgnoreCase(type));
  }

  @Builder
  @Getter
  @Setter
  @RequiredArgsConstructor
  public static class ValidationEntry {
    private final String typeName;
    private final DinaMappingRegistry mappingRegistry;
    private final DinaMapper<?, DinaEntity> mapper;
    private final Class<? extends DinaEntity> entityClass;
    private final Class<?> resourceClass;
    private final DinaService<? extends DinaEntity> dinaService;
  }
}
