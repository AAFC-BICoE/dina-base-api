package ca.gc.aafc.dina.testsupport;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Initializes the Postgres TestContainer if the "embedded.postgresql.enabled" property is true.
 * 
 * Use this initializer in integration tests by adding this annotation to your test class:
 * <pre>
 * {@code @ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })}
 * </pre>
 */
@SuppressFBWarnings({"LI_LAZY_INIT_UPDATE_STATIC", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"})
public class PostgresTestContainerInitializer
  implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static final String DUMP_SCHEMA_CMD = "pg_dump";

  private static final String DUMP_SCHEMA_PATH_OPTION = "embedded.postgresql.dump_schema_path";
  private static final String MAX_CONNECTION_OPTION = "embedded.postgresql.max_connection";

  private static PostgreSQLContainer<?> sqlContainer = null;

  @Override
  public void initialize(ConfigurableApplicationContext ctx) {
    ConfigurableEnvironment env = ctx.getEnvironment();

    // If there is a postgres container enabled, point Spring's datasource properties to it:
    if (!Objects.equals(env.getProperty("embedded.postgresql.enabled"), "false")) {
      if (sqlContainer == null) {
        DockerImageName myImage = DockerImageName
          .parse(Optional.ofNullable(env.getProperty("embedded.postgresql.image")).orElse("postgres"))
          .asCompatibleSubstituteFor("postgres");

        sqlContainer = new PostgreSQLContainer<>(myImage)
          .withDatabaseName(
            Optional.ofNullable(env.getProperty("embedded.postgresql.database"))
              .orElse("integration-tests-db"))
          .withUrlParam("currentSchema",
            Optional.ofNullable(env.getProperty("embedded.postgresql.schema"))
              .orElse("public"))
          .withUsername("sa")
          .withPassword("sa");

        Optional.ofNullable(env.getProperty(MAX_CONNECTION_OPTION))
          .ifPresent(max -> sqlContainer.setCommand("postgres", "-c", "max_connections=" + max));

        Optional.ofNullable(env.getProperty(DUMP_SCHEMA_PATH_OPTION))
          .ifPresent(value -> dumpSchemaOnContextClosedEvent(ctx, value));

        sqlContainer.withInitScript(env.getProperty("embedded.postgresql.init-script-file"));

        sqlContainer.start();
      }

      TestPropertyValues.of(
        "spring.datasource.url=" + sqlContainer.getJdbcUrl()
      ).applyTo(env);

    }
  }

  private void dumpSchemaOnContextClosedEvent(ConfigurableApplicationContext ctx, String dumpLocation) {
    ctx.addApplicationListener(event -> {
      if (event instanceof ContextClosedEvent) {
        try {
          var containerCmdResult = sqlContainer.execInContainer(
            DUMP_SCHEMA_CMD,
            "-U", sqlContainer.getUsername(),
            "--schema-only", sqlContainer.getDatabaseName());

          Path p = Path.of(dumpLocation);
          Files.writeString(p, containerCmdResult.getStdout());

          if (containerCmdResult.getExitCode() != 0) {
            System.out.print(containerCmdResult.getStderr());
          }
        } catch (IOException | InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

}
