package ca.gc.aafc.dina.vocabulary;

import ca.gc.aafc.dina.property.YamlPropertyLoaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(VocabularyConfigTest.VocabularySpringConfiguration.class)
public class VocabularyConfigTest {

  @Inject
  private VocabularySpringConfiguration configObj;

  @Test
  void testConfigurationLoadedFromConfigFile() {
    assertNotNull(configObj);
    assertNotNull(configObj.getVocabulary().get("testVocab"));
    assertNotNull(configObj.getVocabulary().get("testVocab").get(0).getMultilingualTitle());
    assertTrue(configObj.getVocabulary().get("testVocab").get(0).getMultilingualTitle().getTitles().size() >= 2);
  }

  @EnableConfigurationProperties
  @Configuration
  @PropertySource(value = "classpath:vocabulary/testVocabulary.yml", factory = YamlPropertyLoaderFactory.class)
  @ConfigurationProperties
  static class VocabularySpringConfiguration extends VocabularyConfiguration<VocabularyElementConfiguration> {

    public VocabularySpringConfiguration(Map<String, List<VocabularyElementConfiguration>> vocabulary) {
      super(vocabulary);
    }
  }
}
