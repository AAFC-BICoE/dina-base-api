package ca.gc.aafc.dina;

import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class BasePostgresItContext {
}
