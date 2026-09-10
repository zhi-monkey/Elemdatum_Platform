package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.entity.DataTeam;
import org.dubhe.data.domain.entity.DataTeamSubtask;

import java.util.List;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.dao
 * @Project：mineai
 * @name：DataTeamMapper
 * @Date：2024/3/11 15:59
 * @Filename：DataTeamMapper
 * @Desc： DataTeamMapper接口
 */

@DataPermission(ignoresMethod = {"getSubtaskStatusListByTaskId", "updateAnnotationCountBySubtaskId", "tiedDataTeamUser", "getUserListByDataTeamId", "untiedDataTeamUserByDataTeamId","selectTeamIdsByUserId","deleteUserFromAllTeams", "selectById"})
public interface DataTeamMapper extends BaseMapper<DataTeam> {

    // 分页获取团队
    // 分页获取团队（添加canModify字段）
    @Select("SELECT dt.*, " +
            "(CASE WHEN EXISTS (SELECT 1 FROM data_team_task " +
            "WHERE team_id = dt.id AND (status = 0 OR status = 1)  AND (deleted IS NULL OR deleted = 0)) " +
            "THEN 0 ELSE 1 END) AS canModify " +
            "FROM data_team dt ${ew.customSqlSegment}")
    Page<DataTeam> listPage(Page<DataTeam> page, @Param("ew") Wrapper<DataTeam> queryWrapper);

    // 获取该用户创建的所有团队
    @Select("SELECT * FROM data_team where deleted = 0")
    List<DataTeam> queryAllTeams();

    // 绑定团队和用户
    @Update("insert into data_team_user values (#{dataTeamId}, #{userId})")
    void tiedDataTeamUser(Long dataTeamId, Long userId);

    // 根据团队名查询
    @Select("select * from data_team where  name=#{name} and deleted = 0")
    @Results(id = "dataTeamMapperResults",
            value = {
                    @Result(property = "id", column = "id"),
            })
    DataTeam findByDataTeamName(String name);

    // 解绑团队和用户
    @Update("delete from data_team_user where data_team_id = #{dataTeamId}")
    void untiedDataTeamUserByDataTeamId(Long dataTeamId);

    // 根据团队id查询团队成员id
    @Select("select user_id from data_team_user where  data_team_id=#{dataTeamId}")
    List<Long> getUserListByDataTeamId(Long dataTeamId);

    // 根据任务id查询子任务状态
    List<Integer> getSubtaskStatusListByTaskId(Long taskId);

    // 根据任务id查询子任务
    List<DataTeamSubtask> getSubtaskListByTaskId(Long taskId);

    // 子任务已标注图片数量增加
    @Update("update data_team_subtask set current_offset = current_offset + #{addCount} where id = #{subtaskId}")
    void updateAnnotationCountBySubtaskId(Long subtaskId, Long addCount);


    @Select("SELECT data_team_id FROM data_team_user WHERE user_id = #{userId}")
    List<Long> selectTeamIdsByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM data_team_user WHERE user_id = #{userId}")
    void deleteUserFromAllTeams(@Param("userId") Long userId);

}
