package ca.gc.aafc.dina;

import ca.gc.aafc.dina.dto.AgentExternalDTO;
import ca.gc.aafc.dina.dto.AuthorExternalDTO;
import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.repository.meta.ExternalResourceProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

@TestConfiguration
public class ExternalResourceProviderImplementation implements ExternalResourceProvider {

  public static final Map<String, String> typeToReferenceMap = ImmutableMap.of(
    "Agent", "Agent/api/v1/agent",
    "Author", "Author/api/v1/author");

  @Override
  public String getReferenceForType(String type) {
    return typeToReferenceMap.get(type);
  }

  @Override
  public Set<Class<? extends ExternalRelationDto>> getClasses() {
    return ImmutableSet.of(AgentExternalDTO.class, AuthorExternalDTO.class);
  }

}
