package ca.gc.aafc.dina.mybatis;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Based class used to create concrete MyBatis TypeHandler for fields that aer stored as Postgres jsonb.
 * A shared ObjectMapper is used to handle conversion between string and object for the jdbc driver.
 * @param <T>
 */
public abstract class JacksonBasedTypeHandler<T> extends BaseTypeHandler<T> {

  // shared ObjectMapper for all instances
  private static final ObjectMapper OM = JsonMapper.builder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

  protected abstract Class<T> getTypeHandlerClass();

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
          throws SQLException {
    if (ps != null) {
      PGobject jsonObject = new PGobject();
      jsonObject.setType("jsonb");
      try {
        jsonObject.setValue(OM.writeValueAsString(parameter));
      } catch (JacksonException e) {
        throw new RuntimeException(e);
      }
      ps.setObject(i, jsonObject);
    }
  }

  @Override
  public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
    if (rs.getString(columnName) == null) {
      return null;
    }
    try {
      return OM.readValue(rs.getString(columnName), getTypeHandlerClass());
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    if (rs.getString(columnIndex) == null) {
      return null;
    }
    try {
      return OM.readValue(rs.getString(columnIndex), getTypeHandlerClass());
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    if (cs.getString(columnIndex) == null) {
      return null;
    }
    try {
      return OM.readValue(cs.getString(columnIndex), getTypeHandlerClass());
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }
}
