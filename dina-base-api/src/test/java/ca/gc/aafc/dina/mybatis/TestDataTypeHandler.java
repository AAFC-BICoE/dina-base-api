package ca.gc.aafc.dina.mybatis;

public class TestDataTypeHandler extends JacksonBasedTypeHandler<TestTableData.TestTableJson>{
  @Override
  protected Class<TestTableData.TestTableJson> getTypeHandlerClass() {
    return TestTableData.TestTableJson.class;
  }
}
