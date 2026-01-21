package ca.gc.aafc.dina.testsupport.elasticsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
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

  private static final String ES_TEST_USERNAME = "elastic";
  private static final String ES_TEST_PASSWORD = "s3cretPassword";
  private static final String ES_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:8.19.8";

  private static final Object LOCK = new Object();

  private static ElasticsearchContainer esContainer = null;
  private static Path tmpCertFile;

  @Override
  public void initialize(ConfigurableApplicationContext ctx) {
    ConfigurableEnvironment env = ctx.getEnvironment();

    synchronized (LOCK) {
      if (esContainer == null) {
        esContainer = createContainer(env);
        esContainer.start();
      }
    }

    extractCertificate();

    TestPropertyValues.of(
      "elasticsearch.host=" + esContainer.getHost(),
      "elasticsearch.port=" + esContainer.getMappedPort(9200),
      "elasticsearch.username=" + ES_TEST_USERNAME,
      "elasticsearch.password=" + ES_TEST_PASSWORD,
      "elasticsearch.certPath=" + tmpCertFile.toString()
    ).applyTo(env);

    // Register cleanup on context close
    ctx.addApplicationListener(event -> {
      if (event instanceof ContextClosedEvent) {
        cleanup();
      }
    });
  }

  /**
   * Reset container between test classes
   */
  public static void reset() {
    synchronized (LOCK) {
      cleanup();
      esContainer = null;
      tmpCertFile = null;
    }
  }

  private static ElasticsearchContainer createContainer(ConfigurableEnvironment env) {
    ElasticsearchContainer container = new ElasticsearchContainer(ES_IMAGE)
      .withPassword(ES_TEST_PASSWORD)
      .withEnv(CLUSTER_NAME, ELASTIC_SEARCH)
      .waitingFor(Wait.forLogMessage(".*started.*", 1));

    // Install plugins BEFORE starting
    if (BooleanUtils.toBoolean(env.getProperty("elasticsearch.icu.enabled", "false"))) {
      container.withCommand(
        "/bin/bash",
        "-c",
        "elasticsearch-plugin install --batch analysis-icu && " +
          "/usr/local/bin/docker-entrypoint.sh elasticsearch"
      );
    }
    return container;
  }

  private static void extractCertificate() {
    try {
      if (tmpCertFile == null) {
        tmpCertFile = Files.createTempFile("escert", ".crt");
        Optional<byte[]> cert = esContainer.caCertAsBytes();
        if (cert.isPresent()) {
          Files.write(tmpCertFile, cert.get());
        } else {
          throw new RuntimeException("Failed to extract ES certificate");
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to handle certificate file", e);
    }
  }

  private static void cleanup() {
    if (esContainer != null && esContainer.isRunning()) {
      esContainer.stop();
    }

    try {
      if (tmpCertFile != null && Files.exists(tmpCertFile)) {
        Files.delete(tmpCertFile);
      }
    } catch (IOException e) {
      System.err.println("Failed to delete certificate file: " + e.getMessage());
    }
  }
}
