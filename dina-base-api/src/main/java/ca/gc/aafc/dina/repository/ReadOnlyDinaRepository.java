package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.service.DinaService;
import io.crnk.core.exception.MethodNotAllowedException;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Optional;

/**
 * Read-only version of {@link DinaRepository}.
 * create, save and delete will throw a {@link MethodNotAllowedException}.
 * Authorization and auditing services is disabled.
 * @param <D>
 * @param <E>
 */
public class ReadOnlyDinaRepository<D, E extends DinaEntity> extends DinaRepository<D, E > {
  public ReadOnlyDinaRepository(DinaService<E> dinaService,
                                DinaMapper<D, E> dinaMapper, Class<D> resourceClass,
                                Class<E> entityClass, DinaFilterResolver filterResolver) {
    super(dinaService, Optional.empty(), Optional.empty() , dinaMapper, resourceClass, entityClass, filterResolver);
  }

  /**
   * Throws {@link MethodNotAllowedException}
   * Create not allowed on read-only
   * @param resource
   * @param <S>
   * @return
   */
  @Override
  public <S extends D> S create(S resource) {
    throw new MethodNotAllowedException("POST");
  }

  /**
   * Throws {@link MethodNotAllowedException}
   * Save not allowed on read-only
   * @param resource
   * @param <S>
   * @return
   */
  @Override
  public <S extends D> S save(S resource) {
    throw new MethodNotAllowedException("PUT/PATCH");
  }

  /**
   * Throws {@link MethodNotAllowedException}
   * Delete not allowed on read-only
   * @param id
   */
  @Override
  public void delete(Serializable id) {
    throw new MethodNotAllowedException("DELETE");
  }
}
