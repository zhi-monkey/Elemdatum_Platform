package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.entity.DataTeamTask;

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

@DataPermission(ignoresMethod = {"selectById", "updateStatus", "tiedDataTaskSubtask", "getDataTeamSubtaskListByDataTeamTaskId","untieDataTaskSubtask"})
public interface DataTeamTaskMapper extends BaseMapper<DataTeamTask> {

    // 分页获取任务
    @Select("SELECT * FROM data_team_task ${ew.customSqlSegment}")
    Page<DataTeamTask> listPage(Page<DataTeamTask> page, @Param("ew") Wrapper<DataTeamTask> queryWrapper);

    // 绑定任务和子任务
    @Update("insert into data_team_task_subtask values (#{taskId}, #{subtaskId})")
    void tiedDataTaskSubtask(Long taskId, Long subtaskId);

    // 解绑任务和子任务
    @Delete("DELETE FROM data_team_task_subtask WHERE task_id = #{taskId} AND subtask_id = #{subtaskId}")
    void untieDataTaskSubtask(Long taskId, Long subtaskId);


    // 根据任务名查询
    @Select("select * from data_team_task where  name=#{name} and deleted = 0")
    @Results(id = "dataTeamTaskMapperResults",
            value = {
                    @Result(property = "id", column = "id"),
            })
    DataTeamTask findByDataTeamTaskName(String name);

    // 解绑任务关联子任务
    @Update("delete from data_team_task_subtask where task_id = #{taskId}")
    void untiedDataTeamTaskSubtaskByDataTeamTaskId(Long taskId);

    // 根据任务id查询子任务id
    @Select("select subtask_id from data_team_task_subtask where task_id = #{taskId}")
    List<Long> getDataTeamSubtaskListByDataTeamTaskId(Long taskId);


    // 更新任务状态
    @Update("update data_team_task set status = #{status} where id = #{taskId}")
    void updateStatus(@Param("taskId") Long taskId, @Param("status") Integer status);

    // 根据任务ID查询相关的所有dataset_id
    @Select("SELECT dataset_id FROM data_team_task WHERE deleted = 0")
    List<Long> getAllDatasetIdsByTaskId();

}
