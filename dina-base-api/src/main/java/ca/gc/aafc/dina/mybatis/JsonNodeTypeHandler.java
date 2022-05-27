package ca.gc.aafc.dina.mybatis;

import org.postgresql.util.PGobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler that supports Jackson's JsonNode for Postgres jsonb fields.
 */
@MappedTypes(JsonNode.class)
public class JsonNodeTypeHandler extends BaseTypeHandler<JsonNode> {

  private static final ObjectMapper OM = new ObjectMapper();
  private static final ObjectReader JSON_NODE_READER;

  static {
    JSON_NODE_READER = OM.readerFor(JsonNode.class);
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter, JdbcType jdbcType)
          throws SQLException {
    if (ps != null) {
      PGobject jsonbObject = new PGobject();
      jsonbObject.setType("jsonb");
      try {
        jsonbObject.setValue(OM.writeValueAsString(parameter));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      ps.setObject(i, jsonbObject);
    }
  }

  @Override
  public JsonNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String jsonStr = rs.getString(columnName);
    if (jsonStr == null) {
      return null;
    }
    try {
      return JSON_NODE_READER.readTree(jsonStr);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JsonNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String jsonStr = rs.getString(columnIndex);
    if (jsonStr == null) {
      return null;
    }
    try {
      return JSON_NODE_READER.readTree(jsonStr);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JsonNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String jsonStr = cs.getString(columnIndex);
    if (jsonStr == null) {
      return null;
    }
    try {
      return JSON_NODE_READER.readTree(jsonStr);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
