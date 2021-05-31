package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.dto.RelatedEntity;
import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.repository.DinaRepository;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ValidationRegistry {
  private final Map<String, ValidationEntry> typesToEntryMap = new HashMap<>();

  public ValidationRegistry(@NonNull ValidationResourceConfiguration configuration) {
    if (CollectionUtils.isEmpty(configuration.getTypes())) {
      throw new IllegalArgumentException("The validation configuration must return a set of types, " +
        "if no types require validation consider using dina.validationEndpoint.enabled: false");
    }
    initMaps(configuration);
  }

  private void initMaps(ValidationResourceConfiguration configuration) {
    configuration.getTypes().forEach(r -> {

      RelatedEntity relatedEntity = r.getAnnotation(RelatedEntity.class);
      if (relatedEntity == null) {
        throw new IllegalArgumentException("The provided resource must have a valid related entity");
      }

      JsonApiResource jsonApiResource = r.getAnnotation(JsonApiResource.class);
      if (jsonApiResource == null || StringUtils.isBlank(jsonApiResource.type())) {
        throw new IllegalArgumentException(
          "The provided resource must have a valid JsonApiResource annotation");
      }

      String type = jsonApiResource.type();

      DinaRepository<?, ? extends DinaEntity> dinaRepo = configuration.getServiceForType(r);
      if (dinaRepo == null) {
        throw new IllegalArgumentException("The provided configuration must supply a dina repo for type: " + type);
      }

      DinaMappingRegistry registry = new DinaMappingRegistry(r);

      typesToEntryMap.put(type, ValidationEntry.builder()
        .typeName(type)
        .mappingRegistry(registry)
        .resourceClass(r)
        .dinaRepo(dinaRepo)
        .build());
    });
  }

  public Optional<ValidationEntry> getEntryForType(@NonNull String type) {
    return Optional.ofNullable(typesToEntryMap.get(type));
  }

  public boolean hasEntryForType(@NonNull String type) {
    return typesToEntryMap.containsKey(type);
  }

  @Builder
  @Getter
  @Setter
  @RequiredArgsConstructor
  public static class ValidationEntry {
    private final String typeName;
    private final DinaMappingRegistry mappingRegistry;
    private final Class<?> resourceClass;
    private final DinaRepository<?, ? extends DinaEntity> dinaRepo;
  }
}
