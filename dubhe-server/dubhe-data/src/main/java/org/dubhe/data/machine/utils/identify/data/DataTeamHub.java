package org.dubhe.data.machine.utils.identify.data;

import org.dubhe.data.machine.enums.DataTeamSubtaskStateEnum;
import org.dubhe.data.machine.enums.DataTeamTaskStateEnum;
import org.dubhe.data.service.DataTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.machine.utils.identify.data
 * @Project：mineai
 * @name：DataTeamHub
 * @Date：2024/3/18 16:37
 * @Filename：DataTeamHub
 * @Desc： 查询多人标注任务，子任务状态
 */

@Component
public class DataTeamHub {

    @Autowired
    DataTeamService dataTeamService;

    // 获取任务状态
    public DataTeamTaskStateEnum getDataTeamTaskStatus(Long id) {
        return DataTeamTaskStateEnum.getState(dataTeamService.queryTaskStatus(id));
    }

    // 获取子任务状态
    public DataTeamSubtaskStateEnum getDataTeamSubtaskStatus(Long id) {
        return DataTeamSubtaskStateEnum.getState(dataTeamService.querySubtaskStatus(id));
    }

    // 根据子任务id获取文件状态列表
    public List<Integer> getFileStatusListBySubtask(Long subtaskId) {
        return dataTeamService.getFileStatusListBySubtaskId(subtaskId);
//        return null;
    }

    // 根据任务id获取子任务状态列表
    public List<Integer> getSubtaskStatusListByTask(Long taskId) {
        return dataTeamService.getSubtaskStatusListByTaskId(taskId);
//        return null;
    }

    // 更新数据集多人标注状态
    public void updateModuleByTaskId(Long id, Integer moduleStatus) {
        Long datasetId = dataTeamService.queryDatasetIdByTaskId(id);
        dataTeamService.updateModuleByDatasetId(datasetId, moduleStatus);
    }
}
