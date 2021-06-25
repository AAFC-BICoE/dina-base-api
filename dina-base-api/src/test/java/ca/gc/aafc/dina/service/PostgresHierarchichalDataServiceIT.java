package ca.gc.aafc.dina.service;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;


@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class PostgresHierarchichalDataServiceIT {

  @Inject
  private PostgresHierarchichalDataService postgresHierarchichalDataService;

  @Test
  public void getHierarchy_validEntry_hierarchyRetrieved() throws SQLException {
    String[] hierarchy = (String[])postgresHierarchichalDataService.getHierarchy(
      "3", 
      "hierarchy_test_table",
      "id",
      "parent_id",
      "name"
    ).getArray();
    
    assertEquals("Hierarchy should have three elements", 3, hierarchy.length);

  }
  
}