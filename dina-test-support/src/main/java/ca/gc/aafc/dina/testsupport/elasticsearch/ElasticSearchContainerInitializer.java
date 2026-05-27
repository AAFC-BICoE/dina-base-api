package ca.gc.aafc.dina.testsupport.elasticsearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

/**
 * Initializes the ElasticSearch TestContainer.
 * Use this initializer in integration tests by adding this annotation to your test class:
 * <pre>
 * {@code @ContextConfiguration(initializers = { ElasticSearchContainerInitializer.class })}
 * </pre>
 *
 * ElasticSearchConfig can then be created from a ElasticSearchProperties since this initializer
 * will inject the data in properties.
 *
 * Note that the ICU plugin is always installed.
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
        esContainer = createContainer();
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

  @SuppressWarnings("resource")
  private static ElasticsearchContainer createContainer() {

    DockerImageName imageIdentifier = buildImageWithICUPlugin();

    return new ElasticsearchContainer(imageIdentifier)
      .withPassword(ES_TEST_PASSWORD)
      .withEnv(CLUSTER_NAME, ELASTIC_SEARCH)
      .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("elasticsearch")))
      .waitingFor(Wait.forLogMessage(".*started.*", 1));
  }

  private static DockerImageName buildImageWithICUPlugin() {
    ImageFromDockerfile image = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
        builder
          .from(ES_IMAGE)
          .run("bin/elasticsearch-plugin install --batch analysis-icu")
          .build()
      );

    // Get the image ID (this builds the image)
    String imageId = image.get();

    // Convert to DockerImageName and declare compatibility
    return DockerImageName.parse(imageId)
      .asCompatibleSubstituteFor(ES_IMAGE);
  }

  private static void extractCertificate() {
    synchronized (LOCK) {
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
