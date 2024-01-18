package ca.gc.aafc.dina.search.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import javax.inject.Inject;

import ca.gc.aafc.dina.search.config.ElasticSearchProperties;

/**
 * This test is making sure we can load 2 sets of properties for 2 different queues
 */
@SpringBootTest
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ConfigLoadingIT {

  @Inject
  private ElasticSearchProperties elasticSearchProperties;

  @Test
  public void validateConfig() {
    assertEquals("localhost", elasticSearchProperties.getHost());
    assertEquals(9200, elasticSearchProperties.getPort());
  }

}
