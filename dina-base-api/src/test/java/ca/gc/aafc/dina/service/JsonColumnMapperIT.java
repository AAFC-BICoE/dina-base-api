package ca.gc.aafc.dina.service;

// Dina
import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.service.JsonColumnMapper;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
// springframework
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
// tests
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
// utils
import javax.inject.Inject;

@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class JsonColumnMapperIT {

    @Inject
    private JsonColumnMapper jsonColumnMapper;

    // testing against existing attribute from JSON
    @Test
    public void runTest_jsonMapperDataExist() {
        // Run a mapper that would only work in postgres:
        assertEquals(
                Integer.valueOf(1),
                jsonColumnMapper.countFirstLevelKeys(
                        "dina_test_table",
                        "jdata",
                        "attr_01"
                )
        );
    }

    // testing against NOT existing attribute from JSON
    @Test
    public void runTest_jsonMapperNoData() {
        // Run a mapper that would only work in postgres:
        assertEquals(
                Integer.valueOf(0),
                jsonColumnMapper.countFirstLevelKeys(
                        "dina_test_table",
                        "jdata",
                        "attr_03"
                )
        );
    }
}
