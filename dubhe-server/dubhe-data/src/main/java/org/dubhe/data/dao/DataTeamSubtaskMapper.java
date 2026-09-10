package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dubhe.biz.base.annotation.DataPermission;
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

@DataPermission(ignoresMethod = {"listPage","selectByTaskId","selectById","updateStatus","selectActiveSubtasksByUser", "updateById"} )
public interface DataTeamSubtaskMapper extends BaseMapper<DataTeamSubtask> {

    // 根据任务ID查询所有子任务
    @Select("SELECT * FROM data_team_subtask " +
            "WHERE task_id = #{taskId} " +
            "AND deleted = 0 " +
            "ORDER BY user_id")
    List<DataTeamSubtask> selectByTaskId(@Param("taskId") Long taskId);

    // 分页获取子任务
    @Select("SELECT * FROM data_team_subtask ${ew.customSqlSegment}")
    Page<DataTeamSubtask> listPage(Page<DataTeamSubtask> page, @Param("ew") Wrapper<DataTeamSubtask> queryWrapper);

    // 更新子任务状态
    @Update("update data_team_subtask set status = #{status} where id = #{subtaskId}")
    void updateStatus(@Param("subtaskId") Long subtaskId, @Param("status") Integer status);

    @Select("SELECT s.* FROM data_team_subtask s " +
            "INNER JOIN data_team_task t ON s.task_id = t.id " +
            "WHERE s.user_id = #{userId} " +
            "AND s.deleted = 0 " +
            "AND t.status NOT IN (#{excludeStatuses[0]}, #{excludeStatuses[1]})")
    List<DataTeamSubtask> selectActiveSubtasksByUser(
            @Param("userId") Long userId,
            @Param("excludeStatuses") List<Integer> excludeStatuses
    );


}
