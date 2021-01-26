package ca.gc.aafc.dina.property;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = YamlPropertyLoaderFactoryTest.TestConfig.class)
public class YamlPropertyLoaderFactoryTest {

  @Inject
  private TestConfig testConfig;

  @Test
  public void testConfig() {
    assertNotNull(testConfig.getYaml().getTest());
    assertEquals("test1", testConfig.getYaml().getTest());
  }

  /**
   * Nested configuration to avoid class scanning
   */
  @Configuration
  @ConfigurationProperties
  @PropertySource(value = "classpath:yamlConfig.yml", factory = YamlPropertyLoaderFactory.class)
  @Getter
  @Setter
  @EnableConfigurationProperties
  static class TestConfig {
    private YamlConfig yaml;
  }

  @ConstructorBinding
  @RequiredArgsConstructor
  @Getter
  static class YamlConfig {
    private final String test;
  }
}
