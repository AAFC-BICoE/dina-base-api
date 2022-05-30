package ca.gc.aafc.dina.mybatis;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;

@Mapper
public interface TestTableDAO {

  @Select("SELECT id, jdata FROM dina_test_table where id = ${id}")
  @Options(statementType = StatementType.CALLABLE)
  @Result(property = "jdata", column = "jdata", typeHandler = TestDataTypeHandler.class)
  List<TestTableData> loadData(@Param("id") Integer id);
}
