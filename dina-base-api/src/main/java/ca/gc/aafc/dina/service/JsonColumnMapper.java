package ca.gc.aafc.dina.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Base Managed Json Column Mapper Service that checks JSONB column values against the key.
 * Returns total count of records as JSONB column which use the key
 */
@Mapper
public interface JsonColumnMapper {
    //
    String sql = "SELECT COUNT(${columnName}) FROM ${tableName}" +
            " WHERE #{keyName} IN ("+
            " WITH RECURSIVE t(k,j) as (" +
            " SELECT jsonb_object_keys(${columnName}), ${columnName}"+
            " UNION ALL"+
            " SELECT jsonb_object_keys(t.j->t.k), t.j->t.k"+
            " FROM t WHERE jsonb_typeof(t.j->t.k) = 'object'"+
            " )"+
            " SELECT k FROM t)";

    /**
     * Return number of records in the table which have specified key name in JSONb column
     *
     * @param tableName Table name against which the search will be performed
     * @param colName Column name against which the search will be performed
     * @param keyName Key name which will be searched
     * @return Integer, number of records containing the key in the specified table with specified column
     */
    @Select(sql)
    Integer countKeys (
            @Param("tableName") String tableName,
            @Param("columnName") String colName,
            @Param("keyName") String keyName
    );
}