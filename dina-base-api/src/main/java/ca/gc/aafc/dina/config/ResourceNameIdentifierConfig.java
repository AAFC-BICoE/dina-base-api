package ca.gc.aafc.dina.config;

import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Singular;

/**
 * This class allows to configure the fields name for classes that may be used for
 * ResourceNameIdentifier service.
 */
@Builder
public class ResourceNameIdentifierConfig {

  public static final ResourceNameConfig DEFAULT_CONFIG =
    new ResourceNameConfig("name", "group");

  @Singular
  private Map<Class<?>, ResourceNameConfig> configs;

  /**
   * Get the {@link ResourceNameConfig} for the provided class or {@link Optional#empty()}
   * @param clazz
   * @return @link ResourceNameConfig} for the provided class or {@link Optional#empty()}
   */
  public Optional<ResourceNameConfig> getResourceNameConfig(Class<?> clazz) {
    return Optional.ofNullable(configs.get(clazz));
  }

  public record ResourceNameConfig(String nameColumn, String groupColumn) {
  }
}
