package ca.gc.aafc.dina.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = TestConfiguration.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class ExampleDbBackedIntegrationTest {

  @Inject
  private EntityManager em;

  @Test
  public void runTest_whenPostgresTestContainerUsed_postgresQueryIsAvailable() {
    // Run a query that would only work in postgres:
    Object oneCount = em
        .createNativeQuery("select count(*) from pg_catalog.pg_type where typname = 'int2';")
        .getSingleResult();

    assertEquals(BigInteger.valueOf(1), oneCount);
  }

}
