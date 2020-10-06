package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.UntypedResourceRepository;
import io.crnk.core.resource.annotations.JsonApiExposed;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@JsonApiExposed(false)
public class ExternalRepository
  implements UntypedResourceRepository<ExternalRelationDto, String> {

  private final String resourceType;

  public ExternalRepository(String resourceType) {
    this.resourceType = resourceType;
  }

  @Override
  public ResourceList<ExternalRelationDto> findAll(QuerySpec querySpec) {
    List<ExternalRelationDto> dtoList = querySpec.getFilters()
      .stream()
      .map(filterSpec -> newExternalType(filterSpec.getValue()))
      .collect(Collectors.toList());
    return new DefaultResourceList<>(dtoList, null, null);
  }

  @Override
  public ResourceList<ExternalRelationDto> findAll(
    Collection<String> collection, QuerySpec querySpec
  ) {
    return findAll(querySpec);
  }

  @Override
  public <S extends ExternalRelationDto> S save(S s) {
    return s;
  }

  @Override
  public <S extends ExternalRelationDto> S create(S s) {
    return s;
  }

  @Override
  public void delete(String serializable) {

  }

  @SneakyThrows
  private ExternalRelationDto newExternalType(String id) {
    return ExternalRelationDto.builder().type(resourceType).id(id).build();
  }

  @Override
  public String getResourceType() {
    return this.resourceType;
  }

  @Override
  public Class<ExternalRelationDto> getResourceClass() {
    return ExternalRelationDto.class;
  }

  @Override
  public ExternalRelationDto findOne(
    String serializable, QuerySpec querySpec
  ) {
    return newExternalType(serializable);
  }
}
