package ca.gc.aafc.dina.testsupport.crnk;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.ResourceList;

/**
 * Crnk test Repository exposing {@link CrnkTestData}
 */
@Component
public class TestCrnkRepository extends ResourceRepositoryBase<CrnkTestData, UUID> {

  private final Map<UUID, CrnkTestData> data = new ConcurrentHashMap<>();

  public TestCrnkRepository() {
    super(CrnkTestData.class);
  }

  @Override
  public <S extends CrnkTestData> S save(S resource) {
    UUID uuid = UUID.randomUUID();
    data.put(uuid, resource);
    resource.setId(uuid);
    return resource;
  }

  @Override
  public CrnkTestData findOne(UUID id, QuerySpec querySpec) {
    CrnkTestData testData = data.get(id);
    if (testData == null) {
      throw new ResourceNotFoundException("Testdata not found!");
    }
    return testData;
  }

  @Override
  public ResourceList<CrnkTestData> findAll(QuerySpec querySpec) {
    return querySpec.apply(data.values());
  }

  @Override
  public void delete(UUID id) {
    data.remove(id);
  }

}