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
        .getHierarchy("61a75686-a964-451a-86de-b15a89cbcebd", "hierarchy_test_table", "uuid", "parent_id", "name");
        
    assertEquals("Hierarchy should have three elements", 3, hierarchy.size());
    
    HierarchicalObject first = hierarchy.get(0);
    HierarchicalObject second = hierarchy.get(1);
    HierarchicalObject third = hierarchy.get(2);
    
    assertEquals("61a75686-a964-451a-86de-b15a89cbcebd", first.getUuid().toString());
    assertEquals(Integer.valueOf(1),  first.getRank());
    assertEquals("a5", first.getName());
    
    assertEquals("c779eea7-3e6b-42a2-9ce6-88ca66857dad", second.getUuid().toString());
    
    assertEquals("7e0c2c3a-8113-427c-b8ed-83247a82ba43", third.getUuid().toString());
    assertEquals(Integer.valueOf(3), third.getRank());
    assertEquals("a1", third.getName());

  }

}