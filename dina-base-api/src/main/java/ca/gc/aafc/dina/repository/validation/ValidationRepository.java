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
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnProperty(value = "dina.validationEndpoint.enabled", havingValue = "true")
public class ValidationRepository extends ResourceRepositoryBase<ValidationDto, String> {

  public static final String ATTRIBUTES_KEY = "attributes";
  public static final String RELATIONSHIPS_KEY = "relationships";
  public static final String DATA_KEY = "data";
  private final ValidationRegistry validationRegistry;

  public ValidationRepository(
    @NonNull ValidationResourceConfiguration validationResourceConfiguration,
    @NonNull ObjectMapper crnkMapper
  ) {
    super(ValidationDto.class);
    this.validationRegistry = new ValidationRegistry(validationResourceConfiguration, crnkMapper);
  }

  @Override
  @SneakyThrows
  public <S extends ValidationDto> S create(S resource) {
    final String type = resource.getType();
    final JsonNode data = resource.getData();
    validateIncomingRequest(type, data);

    final ValidationResourceHandler<Object> validationEntry = validationRegistry.getEntryForType(type)
      .orElseThrow(ValidationRepository::getInvalidTypeException);

    validationEntry.validate(data);

    // Crnk requires a created resource to have an ID. Create one here if the client did not provide one.
    resource.setId(Optional.ofNullable(resource.getId()).orElse("N/A"));
    return resource;
  }

  private void validateIncomingRequest(String type, JsonNode data) {
    if (StringUtils.isBlank(type) || !validationRegistry.hasEntryForType(type)) {
      throw getInvalidTypeException();
    }

    if (isBlank(data) || !data.has(ATTRIBUTES_KEY) || isBlank(data.get(ATTRIBUTES_KEY))) {
      throw new BadRequestException("You must submit a valid data block");
    }
  }

  public static boolean isBlank(JsonNode data) {
    return data == null || data.isNull() || data.isEmpty();
  }

  private static BadRequestException getInvalidTypeException() {
    return new BadRequestException("You must submit a valid configuration type");
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
