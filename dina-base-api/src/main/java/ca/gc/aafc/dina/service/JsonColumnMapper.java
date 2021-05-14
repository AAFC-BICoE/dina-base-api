package ca.gc.aafc.dina.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Base Managed Json Column Mapper Service based on mybatis (supports only PostgreSQL 9+)
 * The service checks every key in all nested objects from the json column
 * based on recursive CTE(https://www.postgresql.org/docs/9.4/queries-with.html)
 * Returns total count of records which use the key in json column
 */
@Mapper
public interface JsonColumnMapper {
    // This query is checking every key in all nested objects from the json column
    String postgreSql = "SELECT COUNT(${columnName}) FROM ${tableName}" +
            " WHERE #{keyName} IN (" +
            " WITH RECURSIVE t(k,j) as (" +
            " SELECT jsonb_object_keys(${columnName}), ${columnName}" +
            " UNION ALL" +
            " SELECT jsonb_object_keys(t.j->t.k), t.j->t.k" +
            " FROM t WHERE jsonb_typeof(t.j->t.k) = 'object'" +
            " )" +
            " SELECT k FROM t)";

    /**
     * Return number of records in the table which have specified key name in JSONb column
     *
     * @param tableName Table name against which the search will be performed
     * @param colName Column name against which the search will be performed
     * @param keyName Key name which will be searched
     * @return Integer, number of records containing the key in the specified table with specified column
     */
    @Select(postgreSql)
    Integer countKeys (
            @Param("tableName") String tableName,
            @Param("columnName") String colName,
            @Param("keyName") String keyName
    );
}