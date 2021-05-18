package ca.gc.aafc.dina.service;

// Dina
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.service.PostgresJsonbService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
// springframework
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
// tests
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
// utils
import javax.inject.Inject;

@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class PostgresJsonbServiceIT {

    @Inject
    private PostgresJsonbService postgresJsonbService;

    @DisplayName("Test countFirstLevelKeys when key exist in JSONb column")
    @Test
    public void runTest_countFirstLevelKeysKeyExistInJson() {
        // Run a mapper that would only work in postgres
        assertEquals(
                // attr_01 does exist in one record in jdata column
                Integer.valueOf(1),
                postgresJsonbService.countFirstLevelKeys(
                        "dina_test_table",
                        "jdata",
                        "attr_01"
                )
        );
    }

    @DisplayName("Test countFirstLevelKeys when key NOT exist in JSONb column")
    @Test
    public void runTest_countFirstLevelKeysKeyNotExistInJson() {
        // Run a mapper that would only work in postgres
        assertEquals(
                // attr_03 does not exist in any records in jdata column
                Integer.valueOf(0),
                postgresJsonbService.countFirstLevelKeys(
                        "dina_test_table",
                        "jdata",
                        "attr_03"
                )
        );
    }
}
