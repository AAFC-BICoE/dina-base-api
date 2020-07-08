package ca.gc.aafc.dina.testsupport.crnk;

import java.util.Collections;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepositoryBase;
import io.crnk.core.resource.list.DefaultResourceList;
import io.crnk.core.resource.list.ResourceList;

@Component
public class TestCrnkRepository extends ResourceRepositoryBase<CrnkTestData, UUID> {

  public TestCrnkRepository() {
    super(CrnkTestData.class);
  }

  @Override
  public ResourceList<CrnkTestData> findAll(QuerySpec querySpec) {
    CrnkTestData testData = new CrnkTestData();
    return new DefaultResourceList<CrnkTestData>(Collections.singletonList(testData), null, null);
  }

}