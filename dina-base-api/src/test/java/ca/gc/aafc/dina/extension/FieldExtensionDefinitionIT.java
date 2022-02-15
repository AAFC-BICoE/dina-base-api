package ca.gc.aafc.dina.extension;

import ca.gc.aafc.dina.property.YamlPropertyLoaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.inject.Inject;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest( classes = FieldExtensionDefinitionIT.ExtensionTestConfiguration.class)
public class FieldExtensionDefinitionIT {

  @Inject
  private ExtensionTestConfiguration config;

  @Test
  public void testFieldExtensionLoading() {
    assertEquals(1, config.getExtension().size());
    assertTrue(config.getExtension().get("cfia_ppc").containsTerm("ppc-1"));
  }

  @Configuration
  @ConfigurationProperties
  @PropertySource(value = "classpath:extension/cfia_ppc.yml", factory  = YamlPropertyLoaderFactory.class)
  @EnableConfigurationProperties
  public static class ExtensionTestConfiguration {

    private final Map<String, FieldExtensionDefinition.Extension> extension;

    public ExtensionTestConfiguration(Map<String, FieldExtensionDefinition.Extension> extension) {
      this.extension = extension;
    }

    public Map<String, FieldExtensionDefinition.Extension> getExtension() {
      return extension;
    }

  }
}
