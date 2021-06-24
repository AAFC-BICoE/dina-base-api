package ca.gc.aafc.dina.service;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PostgresHierarchichalDataService {
/**
  * @param tableName Table name against which the search will be performed
  * @param idColName
  * @param nameColName
  * @param parentIdColName
  * @return

  /*
  Current query as tested on db-fiddle.  To be implemented
  
  WITH RECURSIVE get_hierarchy(child_id, parent_id, depth, hierarchy) AS (
  SELECT child_id, parent_id, 1, ARRAY[child_id]
  	FROM hierarchy_test_table as ht
  UNION ALL
  SELECT ht.child_id, ht.parent_id, depth + 1, hierarchy || ht.child_id
  FROM  ht 
  JOIN get_hierarchy on get_hierarchy.parent_id = ht.child_id
  )
  
  select hierarchy from
  (SELECT child_id, pa, depth
  FROM get_hierarchy where hierarchy[1] = 3 order by depth desc
  ) a 
  limit 1
  */
  
}
