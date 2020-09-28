package ca.gc.aafc.dina;

import ca.gc.aafc.dina.repository.meta.ExternalResourceProvider;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class ExternalResourceProviderImplementation implements ExternalResourceProvider {

  @Override
  public String getRelationsForType(String type) {
    return "Something.com/api/v1/something";
  }

}
