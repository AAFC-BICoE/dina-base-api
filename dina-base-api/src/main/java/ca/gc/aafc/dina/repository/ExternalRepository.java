package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ReadOnlyResourceRepositoryBase;
import io.crnk.core.resource.annotations.JsonApiExposed;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@JsonApiExposed(false)
public class ExternalRepository<D extends ExternalRelationDto>
  extends ReadOnlyResourceRepositoryBase<D, Serializable> {

  private final Class<D> resourceClass;

  public ExternalRepository(Class<D> resourceClass) {
    super(resourceClass);
    this.resourceClass = resourceClass;
  }

  @Override
  public ResourceList<D> findAll(QuerySpec querySpec) {
    List<D> dtoList = querySpec.getFilters()
      .stream()
      .map(this::newExternalType)
      .collect(Collectors.toList());
    return new DefaultResourceList<>(dtoList, null, null);
  }

  @SneakyThrows
  private D newExternalType(FilterSpec filterSpec) {
    D obj = resourceClass.getConstructor().newInstance();
    obj.setId(filterSpec.getValue());
    return obj;
  }
}
