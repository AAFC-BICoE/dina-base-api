package ca.gc.aafc.dina.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = TestDinaBaseApp.class
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class PostgresHierarchichalDataServiceIT {


  
}