package ca.gc.aafc.dina;

import ca.gc.aafc.dina.repository.meta.ExternalResourceProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;

@TestConfiguration
public class ExternalResourceProviderImplementation implements ExternalResourceProvider {

  public static final Map<String, String> map = ImmutableMap.of(
    "Agent", "Agent/api/v1/agent",
    "Author", "Author/api/v1/author");

  @Override
  public String getReferenceForType(String type) {
    return map.get(type);
  }

}
