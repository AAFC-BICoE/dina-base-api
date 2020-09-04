package ca.gc.aafc.dina.testsupport;

import java.util.Objects;
import java.util.Optional;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.PostgreSQLContainer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Initializes the Postgres TestContainer if the "embedded.postgresql.enabled" property is true.
 * 
 * Use this initializer in integration tests by adding this annotation to your test class:
 * <pre>{@code
 * @ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
 * }</pre>
 */
@SuppressFBWarnings({"LI_LAZY_INIT_UPDATE_STATIC", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"})
public class PostgresTestContainerInitializer
  implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private static PostgreSQLContainer<?> sqlContainer = null;

  @Override
  public void initialize(ConfigurableApplicationContext ctx) {
    ConfigurableEnvironment env = ctx.getEnvironment();

    // If there is a postgres container enabled, point Spring's datasource properties to it:
    if (!Objects.equals(env.getProperty("embedded.postgresql.enabled"), "false")) {
      if (sqlContainer == null) {
        sqlContainer = new PostgreSQLContainer<>(
          Optional.ofNullable(env.getProperty("embedded.postgresql.image"))
              .orElse("postgres"))
          .withDatabaseName(
            Optional.ofNullable(env.getProperty("embedded.postgresql.database"))
              .orElse("integration-tests-db"))
          .withUrlParam("currentSchema",
            Optional.ofNullable(env.getProperty("embedded.postgresql.schema"))
              .orElse("public"))
          .withUsername("sa")
          .withPassword("sa");
        sqlContainer.withInitScript(env.getProperty("embedded.postgresql.init-script-file"));
        sqlContainer.start();
      }

      TestPropertyValues.of(
        "spring.datasource.url=" + sqlContainer.getJdbcUrl()
      ).applyTo(env);

    }

  }

}
