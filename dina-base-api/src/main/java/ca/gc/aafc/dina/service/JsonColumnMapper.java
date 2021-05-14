package ca.gc.aafc.dina.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Base Managed Json Column Mapper Service based on mybatis (supports only PostgreSQL 9+)
 * The service check if key exist as a top-level key within the JSON value
 * Returns total count of records which use the key in json column
 */
@Mapper
public interface JsonColumnMapper {
    // This query is checking if the the key name exist as a top-level key within the JSON value
    String postgreSql = "SELECT COUNT(${columnName}) FROM ${tableName}" +
            " WHERE ${columnName} ?? #{keyName}";
    // RE: WHERE ${columnName} ?? #{keyName}
    // we need to keep such question marks in a SQL statement from being interpreted as positional parameters
    // that's why we have to use two question marks (??) as escape sequence

    /**
     * Return number of records in the table if specified key exist as a top-level key within the JSON value
     *
     * @param tableName Table name against which the search will be performed
     * @param colName Column name against which the search will be performed
     * @param keyName Key name which will be searched
     * @return Integer, number of records containing specified key exist as a top-level key within the JSON value
     */
    @Select(postgreSql)
    Integer countFirstLevelKeys (
            @Param("tableName") String tableName,
            @Param("columnName") String colName,
            @Param("keyName") String keyName
    );
}
