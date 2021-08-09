package ca.gc.aafc.dina.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JaversDataService {

  @Select("select count(*) from jv_snapshot s join jv_commit c on s.commit_fk = c.commit_pk where 1=1;")
  Long getResourceCount();

}
