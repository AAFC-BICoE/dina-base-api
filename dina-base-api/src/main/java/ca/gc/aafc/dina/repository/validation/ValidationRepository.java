package ca.gc.aafc.dina.repository.validation;

import ca.gc.aafc.dina.dto.ValidationDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.crnk.core.exception.BadRequestException;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.ResourceList;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(value = "dina.validationEndpoint.enabled", havingValue = "true")
public class ValidationRepository extends ResourceRepositoryBase<ValidationDto, String> {

  private final List<ValidationResourceHandler<?>> validators;
  private final ObjectMapper crnkMapper;

  public ValidationRepository(
    @NonNull ValidationResourceConfiguration validationResourceConfiguration,
    @NonNull ObjectMapper crnkMapper
  ) {
    super(ValidationDto.class);
    if (CollectionUtils.isEmpty(validationResourceConfiguration.getValidationHandlers())) {
      throw new IllegalArgumentException("The validation configuration must return a set of types, " +
        "if no types require validation consider using dina.validationEndpoint.enabled: false");
    }
    this.validators = validationResourceConfiguration.getValidationHandlers();
    this.crnkMapper = crnkMapper;
  }

  @Override
  @SneakyThrows
  public <S extends ValidationDto> S create(S resource) {
    final String type = resource.getType();
    final JsonNode data = resource.getData();
    validateIncomingRequest(type, data);

    validators.forEach(validator -> {
      if (validator.isSupported(type)) {
        validator.validate(data, crnkMapper);
      }
    });

    // Crnk requires a created resource to have an ID. Create one here if the client did not provide one.
    resource.setId(Optional.ofNullable(resource.getId()).orElse("N/A"));
    return resource;
  }

  private void validateIncomingRequest(String type, JsonNode data) {
    if (StringUtils.isBlank(type) || hasNoSupportedType(type)) {
      throw new BadRequestException("You must submit a valid configuration type");
    }

    if (isInvalidDataBlock(data)) {
      throw new BadRequestException("You must submit a valid data block");
    }
  }

  private static boolean isInvalidDataBlock(JsonNode data) {
    return ValidationNodeHelper.isBlank(data) || !data.has(ValidationNodeHelper.ATTRIBUTES_KEY)
      || ValidationNodeHelper.isBlank(data.get(ValidationNodeHelper.ATTRIBUTES_KEY));
  }

  private boolean hasNoSupportedType(String type) {
    return validators.stream().noneMatch(v -> v.isSupported(type));
  }

  @Override
  public <S extends ValidationDto> S save(S resource) {
    throw new MethodNotAllowedException("PUT/PATCH");
  }

  @Override
  public void delete(String id) {
    throw new MethodNotAllowedException("DELETE");
  }

  @Override
  public ResourceList<ValidationDto> findAll(QuerySpec arg0) {
    throw new MethodNotAllowedException("GET");
  }

}
