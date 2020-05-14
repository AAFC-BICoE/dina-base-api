package ca.gc.aafc.dina.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.DinaService;
import ca.gc.aafc.dina.mapper.DinaMapper;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.registry.ResourceRegistryAware;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.core.resource.list.ResourceList;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DinaRepository<D, E extends DinaEntity>
    implements ResourceRepository<D, Serializable>, ResourceRegistryAware {

  @NonNull
  private final DinaService<E> dinaService;

  @NonNull
  private final DinaMapper<D, E> dinaMapper;

  @Getter
  @NonNull
  private final Class<D> resourceClass;

  @NonNull
  private final Class<E> entityClass;

  @Getter
  @Setter(onMethod_ = @Override)
  private ResourceRegistry resourceRegistry;

  @Override
  public D findOne(Serializable id, QuerySpec querySpec) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceList<D> findAll(QuerySpec querySpec) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceList<D> findAll(Collection<Serializable> ids, QuerySpec querySpec) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <S extends D> S save(S resource) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @SneakyThrows
  public <S extends D> S create(S resource) {
    E entity = entityClass.newInstance();

    ResourceInformation resourceInformation =
        this.resourceRegistry.findEntry(resourceClass).getResourceInformation();

    Set<String> attributes = resourceInformation.getAttributeFields().stream()
        .map(af -> af.getUnderlyingName()).collect(Collectors.toSet());
    attributes.add(resourceInformation.getIdField().getUnderlyingName());

    Map<Class<?>, Set<String>> fieldsPerClass = new HashMap<>();
    fieldsPerClass.put(resourceClass, attributes);

    List<ResourceField> relationFields = resourceInformation.getRelationshipFields();
    for (ResourceField relationField : relationFields) {
      //TODO
    }

    dinaMapper.applyDtoToEntity(resource, entity, fieldsPerClass, new HashSet<>());

    dinaService.create(entity);

    return null;
  }

  @Override
  public void delete(Serializable id) {
    // TODO Auto-generated method stub

  }

}
