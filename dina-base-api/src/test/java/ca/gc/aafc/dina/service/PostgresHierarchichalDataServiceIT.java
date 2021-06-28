package ca.gc.aafc.dina.service;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.ArrayList;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
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
    ArrayList<String> hierarchy = postgresHierarchichalDataService
        .getHierarchy("3", "hierarchy_test_table", "id", "parent_id", "name");

    assertEquals("Hierarchy should have three elements", 2, hierarchy.size());
    assertEquals("", "(3, 2)", hierarchy.get(0));

  }

}