package ca.gc.aafc.dina.service;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;


import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;

@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class PostgresHierarchicalDataServiceIT{

  @Inject
  private PostgresHierarchicalDataService postgresHierarchicalDataService;

  @Test
  public void getHierarchy_validEntry_hierarchyRetrieved() throws SQLException {
    List<HierarchicalObject> hierarchy = postgresHierarchicalDataService
        .getHierarchy("5", "hierarchy_test_table", "id", "parent_id", "name");
    
    HierarchicalObject first = hierarchy.get(0);
    HierarchicalObject second = hierarchy.get(1);
    HierarchicalObject third = hierarchy.get(2);
    
    assertEquals("Hierarchy should have three elements", 3, hierarchy.size());
    
    assertEquals("", Integer.valueOf(5), first.getId());
    assertEquals("", Integer.valueOf(1),  first.getRank());
    assertEquals("", "a5", first.getName());
    
    assertEquals("", Integer.valueOf(2), second.getId());
    
    assertEquals("", Integer.valueOf(1), third.getId());
    assertEquals("", Integer.valueOf(3), third.getRank());
    assertEquals("", "a1", third.getName());

  }

}