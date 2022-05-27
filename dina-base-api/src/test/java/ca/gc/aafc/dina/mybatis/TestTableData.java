package ca.gc.aafc.dina.mybatis;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class TestTableData {
  private Integer id;
  private JsonNode jdata;
}
