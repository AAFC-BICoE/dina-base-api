package ca.gc.aafc.dina.repository;

import java.util.function.Consumer;

import ca.gc.aafc.dina.dto.JsonApiDto;
import ca.gc.aafc.dina.exception.ResourceGoneException;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;

/**
 * Defines the method signature for the repository layer.
 */
public interface DinaRepositoryLayer<I,D> {

  JsonApiDto<D> getOne(I identifier, String queryString) throws ResourceNotFoundException,
      ResourceGoneException;

  DinaRepositoryV2.PagedResource<JsonApiDto<D>> getAll(QueryComponent queryComponents);

  JsonApiDto<D> create(JsonApiDocument docToCreate, Consumer<D> dtoCustomizer);

  JsonApiDto<D> update(JsonApiDocument patchDto) throws ResourceNotFoundException, ResourceGoneException;

  void delete(I identifier) throws ResourceNotFoundException, ResourceGoneException;

}
