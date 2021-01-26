package ca.gc.aafc.dina.property;

import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class YamlPropertyLoaderFactoryTest {

  @Autowired
  private YamlConfig yamlConfig;

  @Test
  public void testConfig() {
    assertNotNull(yamlConfig.getTest());
  }

  /**
   * Nested configuration to avoid class scanning
   */
  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(YamlConfig.class)
  static class TestConfig{
  }

  @PropertySource(value = "classpath:yamlConfig.yml", factory = YamlPropertyLoaderFactory.class)
  @ConfigurationProperties
  @Data
  static class YamlConfig {
    private final String test;
  }
}
