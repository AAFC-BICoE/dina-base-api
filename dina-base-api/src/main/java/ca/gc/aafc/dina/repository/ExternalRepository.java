package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ReadOnlyResourceRepositoryBase;
import io.crnk.core.resource.annotations.JsonApiExposed;
import io.crnk.core.resource.list.ResourceList;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
@JsonApiExposed(false)
public class ExternalRepository extends ReadOnlyResourceRepositoryBase<ExternalRelationDto, Serializable> {

  protected ExternalRepository() {
    super(ExternalRelationDto.class);
  }

  @Override
  public ResourceList<ExternalRelationDto> findAll(QuerySpec querySpec) {
    return null;
  }
}
