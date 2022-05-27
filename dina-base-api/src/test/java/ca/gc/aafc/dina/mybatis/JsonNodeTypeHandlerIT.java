package ca.gc.aafc.dina.mybatis;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test using a test dao ({@link TestTableDAO} to load a jsonb field into a JsonNode.
 */
@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class JsonNodeTypeHandlerIT {

  @Inject
  private TestTableDAO dao;

  @Test
  public void testJsonNodeTypeHandler () {
    List<TestTableData> testData = dao.loadData(1);
    assertFalse(testData.isEmpty());
    assertEquals("val_01", testData.get(0).getJdata().get("attr_01").asText());
  }

}
