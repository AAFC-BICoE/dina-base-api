package ca.gc.aafc.dina.mybatis;

import lombok.Data;

@Data
public class TestTableData {
  private Integer id;
  private TestTableJson jdata;

  @Data
  public static class TestTableJson {
    private String attr_01;
  }
}