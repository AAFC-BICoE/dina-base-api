package ca.gc.aafc.dina.repository.external;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.UntypedResourceRepository;
import io.crnk.core.resource.annotations.JsonApiExposed;
import io.crnk.core.resource.list.ResourceList;

import java.util.Collection;

/**
 * Internal Repository required to mimic relations for ExternalRelationDto's usage. External
 * relations are not backed by a real resource.
 */
@JsonApiExposed(false)
public class ExternalRepository implements UntypedResourceRepository<ExternalRelationDto, String> {

  private final String resourceType;

  public ExternalRepository(String resourceType) {
    this.resourceType = resourceType;
  }

  @Override
  public ExternalRelationDto findOne(String id, QuerySpec querySpec) {
    return ExternalRelationDto.builder().type(resourceType).id(id).build();
  }

  @Override
  public ResourceList<ExternalRelationDto> findAll(QuerySpec querySpec) {
    throw new MethodNotAllowedException("External Types only use findOne");
  }

  @Override
  public ResourceList<ExternalRelationDto> findAll(
      Collection<String> collection, QuerySpec querySpec
  ) {
    throw new MethodNotAllowedException("External Types only use findOne");
  }

  @Override
  public <S extends ExternalRelationDto> S save(S s) {
    throw new MethodNotAllowedException("External Types cannot be patched");
  }

  @Override
  public <S extends ExternalRelationDto> S create(S s) {
    throw new MethodNotAllowedException("External Types cannot be created");
  }

  @Override
  public void delete(String serializable) {
    throw new MethodNotAllowedException("External Types cannot be deleted");
  }

  @Override
  public String getResourceType() {
    return this.resourceType;
  }

  @Override
  public Class<ExternalRelationDto> getResourceClass() {
    return ExternalRelationDto.class;
  }

}
