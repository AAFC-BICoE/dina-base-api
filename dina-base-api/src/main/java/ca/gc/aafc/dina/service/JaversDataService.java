package ca.gc.aafc.dina.service;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface JaversDataService {

  @Select({"<script>",
    "select count(*) from jv_snapshot s join jv_commit c on s.commit_fk = c.commit_pk where 1=1",
    "<if test='author != null'> and c.author = #{author} </if>",
    "<if test='id != null and type != null'> and global_id_fk = ",
      "(select global_id_pk from jv_global_id where local_id = #{id} and type_name = #{type}) </if>",
    "</script>"})
  Long getResourceCount(@Param("id") String id, @Param("type") String type, @Param("author") String author);

  @Delete({"<script>",
    "<foreach item='commitID' index='index' collection='commitIds'>",
      "delete from jv_snapshot where commit_fk = (select commit_pk from jv_commit where commit_id = #{commitID});",
      "delete from jv_commit where commit_id = #{commitID};",
      "delete from jv_commit_property where commit_fk = (select commit_pk from jv_commit where commit_id = #{commitID});",
    "</foreach>",
    "delete from jv_global_id where local_id = #{instanceId} and type_name = #{type};",
    "</script>"})
  void removeSnapshots(
    @Param("commitIds") List<BigDecimal> commitIds,
    @Param("instanceId") String instanceId,
    @Param("type") String type
  );
}
