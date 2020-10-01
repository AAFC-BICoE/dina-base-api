package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ReadOnlyResourceRepositoryBase;
import io.crnk.core.resource.annotations.JsonApiExposed;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@JsonApiExposed(false)
public class ExternalRepository extends ReadOnlyResourceRepositoryBase<ExternalRelationDto, Serializable> {

  protected ExternalRepository() {
    super(ExternalRelationDto.class);
  }

  @Override
  public ResourceList<ExternalRelationDto> findAll(QuerySpec querySpec) {
    List<ExternalRelationDto> dtos = querySpec.getFilters()
      .stream()
      .map(filterSpec -> ExternalRelationDto.builder().id(filterSpec.getValue()).build())
      .collect(Collectors.toList());
    return new DefaultResourceList<>(dtos, null, null);
  }
}
