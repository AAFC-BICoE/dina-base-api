package ca.gc.aafc.dina;

import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Set;

@TestConfiguration
public class ExternalResourceProviderImplementation implements ExternalResourceProvider {

  public static final Map<String, String> typeToReferenceMap = ImmutableMap.of(
    "agent", "Agent/api/v1/agent",
    "author", "Author/api/v1/author");

  @Override
  public String getReferenceForType(String type) {
    return typeToReferenceMap.get(type);
  }

  @Override
  public Set<String> getTypes() {
    return typeToReferenceMap.keySet();
  }

}
