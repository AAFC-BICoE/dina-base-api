package ca.gc.aafc.dina.service;

import java.sql.Array;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

@Mapper
public interface PostgresHierarchichalDataService {
/**
* @param id         ID of the object for which we seek the hierarchy
* @param tableName Table name against which the search will be performed
* @param idColName
* @param nameColName
* @param parentIdColName
* @return
**/


/*
Current query as tested on db-fiddle.  To be implemented with ibatis

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
  
  @Select(
  "WITH RECURSIVE get_hierarchy(child_id, parent_id, depth, hierarchy) AS ("
      + "SELECT ${idColName}, ${parentIdColName}, 1, ARRAY[${idColName}] " + "FROM ${tableName} as t" + "UNION ALL"
      + "SELECT t.${idColName}, t.${parentIdColName}, depth + 1, hierarchy || t.${idColName} " + "FROM t"
      + "JOIN get_hierarchy on get_hierarchy.${parentIdColName} = t.child_id) " + "SELECT hierarchy FROM ("
      + "SELECT child_id, hierarchy, depth " + "FROM get_hierarchy where hierarchy[1] = ${id} "
      + "ORDER BY depth desc ) a "
      + "LIMIT 1;"
    )
  @Options(statementType = StatementType.CALLABLE)
  Array getHierarchy (
    @Param("id") String id,
    @Param("tableName") String tableName,
    @Param("idColumnName") String idColName,
    @Param("parentIdColumnName") String parentIdColName,
    @Param("nameColumnName") String nameColName
  );

  
  

  
}
