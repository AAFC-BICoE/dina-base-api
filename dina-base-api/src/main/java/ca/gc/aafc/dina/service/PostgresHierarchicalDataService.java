package ca.gc.aafc.dina.service;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

@Mapper
public interface PostgresHierarchicalDataService {
/**
* @param uuid         ID of the object for which we seek the hierarchy
* @param tableName  Table name against which the search will be performed
* @param idColumnName     Name of the column containing the object id
* @param uuidColumnName
* @param nameColumnName   Name of the column containing the object name
* @param parentIdColumnName Name of the column containing the parent id
**/

  
@Select(
"WITH RECURSIVE get_hierarchy (id, parent_id, name, rank) AS ( "
    + "SELECT initial_t.${uuidColumnName}, initial_t.${parentIdColumnName}, initial_t.${nameColumnName}, 1 "
    + "FROM ${tableName} AS initial_t where initial_t.uuid = ${uuid} " + "UNION ALL "
    + "SELECT node.${uuidColumnName}, node.${parentIdColumnName}, node.${nameColumnName}, gh.rank + 1 "
    + "FROM get_hierarchy gh, ${tableName} AS node " + "WHERE node.${uuidColumnName} = gh.${parentIdColumnName}) "
+ "SELECT id, name, rank FROM get_hierarchy;"
)
  @Options(statementType = StatementType.CALLABLE)
  List<HierarchicalObject> getHierarchy (
    @Param("uuid") String uuid,
    @Param("tableName") String tableName,
    @Param("uuidColumnName") String uuidColumnName,
    @Param("parentIdColumnName") String parentIdColumnName,
    @Param("nameColumnName") String nameColumnName
  );
  
}
