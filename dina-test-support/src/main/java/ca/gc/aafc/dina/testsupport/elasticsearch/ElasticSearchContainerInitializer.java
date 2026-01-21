package ca.gc.aafc.dina.testsupport.elasticsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Initializes the ElasticSearch TestContainer.
 * Use this initializer in integration tests by adding this annotation to your test class:
 * <pre>
 * {@code @ContextConfiguration(initializers = { ElasticSearchContainerInitializer.class })}
 * </pre>
 *
 * ElasticSearchConfig can then be created from a ElasticSearchProperties since this initializer
 * will inject the data in properties.
 */
public class ElasticSearchContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final String CLUSTER_NAME = "cluster_name";
  private static final String ELASTIC_SEARCH = "elasticsearch";

  private static ElasticsearchContainer esContainer = null;
  private static final String ES_TEST_USERNAME = "elastic";
  private static final String ES_TEST_PASSWORD = "s3cretPassword";

  @Override
  public void initialize(ConfigurableApplicationContext ctx) {
    ConfigurableEnvironment env = ctx.getEnvironment();

    if(esContainer == null) {
      esContainer = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.19.8")
        .withPassword(ES_TEST_PASSWORD)
        .withEnv(CLUSTER_NAME, ELASTIC_SEARCH)
        .waitingFor(Wait.forLogMessage(".*started.*", 1));
    }

    Path tmpCertFile;
    esContainer.start();

    try {
      tmpCertFile = Files.createTempFile("escert", ".crt");
      Optional<byte[]> cert = esContainer.caCertAsBytes();
      Path finalTmpCertFile = tmpCertFile;
      cert.ifPresent(c -> {
        try {
          Files.write(finalTmpCertFile, c);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    TestPropertyValues.of(
      "elasticsearch.host=" + esContainer.getHost(),
      "elasticsearch.port=" + esContainer.getMappedPort(9200),
      "elasticsearch.username=" + ES_TEST_USERNAME,
      "elasticsearch.password=" + ES_TEST_PASSWORD,
      "elasticsearch.certPath=" + tmpCertFile.toString()
    ).applyTo(env);
  }
}
