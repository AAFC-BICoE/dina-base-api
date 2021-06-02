package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;
import ca.gc.aafc.dina.repository.DinaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ValidationRegistry {
  private final Map<String, ValidationResourceHandler<Object>> typesToEntryMap = new HashMap<>();

  public ValidationRegistry(
    @NonNull ValidationResourceConfiguration configuration,
    @NonNull ObjectMapper crnkMapper
  ) {
    if (CollectionUtils.isEmpty(configuration.getTypes())) {
      throw new IllegalArgumentException("The validation configuration must return a set of types, " +
        "if no types require validation consider using dina.validationEndpoint.enabled: false");
    }
    initMaps(configuration, crnkMapper);
  }

  private void initMaps(
    ValidationResourceConfiguration configuration,
    ObjectMapper crnkMapper
  ) {
    configuration.getTypes().forEach(r -> {
      JsonApiResource jsonApiResource = r.getAnnotation(JsonApiResource.class);
      if (jsonApiResource == null || StringUtils.isBlank(jsonApiResource.type())) {
        throw new IllegalArgumentException(
          "The provided resource must have a valid JsonApiResource annotation");
      }

      String type = jsonApiResource.type();

      DinaRepository<?, ? extends DinaEntity> dinaRepo = configuration.getRepoForType(r);
      if (dinaRepo == null) {
        throw new IllegalArgumentException("The provided configuration must supply a dina repo for type: " + type);
      }

      DinaMappingRegistry registry = new DinaMappingRegistry(r);

      typesToEntryMap.put(type, ValidationResourceHandler.builder()
        .mappingRegistry(registry)
        .resourceClass((Class<Object>) r)
        .dinaRepo((DinaRepository<Object, ? extends DinaEntity>) dinaRepo)
        .crnkMapper(crnkMapper)
        .build());
    });
  }

  public Optional<ValidationResourceHandler<Object>> getEntryForType(@NonNull String type) {
    return Optional.ofNullable(typesToEntryMap.get(type));
  }

  public boolean hasEntryForType(@NonNull String type) {
    return typesToEntryMap.containsKey(type);
  }

}
