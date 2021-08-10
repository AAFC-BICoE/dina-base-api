package ca.gc.aafc.dina.service;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * JaversDataService provides additional support for operations with the javers database.
 */
@Mapper
public interface JaversDataService {

  /**
   * Returns the total resource count by a given Author and/or (id + type). Parameters may be null if you do
   * not want to filter by the given parameter. Id and type must be submitted together to be.
   *
   * @param id     id of the resource
   * @param type   type of resource
   * @param author author of the resource
   * @return Returns the total resource count by a given Author and/or (id + type).
   */
  @Select({"<script>",
    "select count(*) from jv_snapshot s join jv_commit c on s.commit_fk = c.commit_pk where 1=1",
    "<if test='author != null'> and c.author = #{author} </if>",
    "<if test='id != null and type != null'> and global_id_fk = ",
      "(select global_id_pk from jv_global_id where local_id = #{id} and type_name = #{type}) </if>",
    "</script>"})
  Long getResourceCount(@Param("id") String id, @Param("type") String type, @Param("author") String author);

  /**
   * Removes snapshots, commits, commit properties, and global id from the javers database. Based on the given
   * parameters. Javers does not support the removal of snapshots by default, this may be used as an
   * alternative.
   *
   * @param commitIds  commit ids of the commits
   * @param instanceId instance id of the snapshot
   * @param type       type of resource
   */
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
