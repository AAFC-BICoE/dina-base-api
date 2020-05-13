package ca.gc.aafc.dina.repository;

import java.io.Serializable;
import java.util.Collection;

import javax.inject.Inject;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.DinaService;
import ca.gc.aafc.dina.mapper.DinaMapper;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.core.resource.list.ResourceList;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DinaRepository<D, E extends DinaEntity> implements ResourceRepository<D, Serializable> {

  @NonNull
  private final DinaService<E> dinaService;

  @NonNull
  private final DinaMapper<D, E> dinaMapper;

  @Getter
  @NonNull 
  private final Class<D> resourceClass;

  @NonNull
  private final Class<E> entityClass;

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
  public <S extends D> S create(S resource) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(Serializable id) {
    // TODO Auto-generated method stub

  }

}
