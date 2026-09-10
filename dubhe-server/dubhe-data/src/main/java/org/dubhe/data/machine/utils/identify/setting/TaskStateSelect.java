

package org.dubhe.data.machine.utils.identify.setting;

import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.data.machine.constant.DataTeamSubtaskStateCodeConstant;
import org.dubhe.data.machine.enums.DataTeamTaskStateEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description 任务状态判断类
 * @date 2020-09-24
 */
@Component
public class TaskStateSelect {

    // 未标注 所有子任务均为未标注状态
    public DataTeamTaskStateEnum isInit(List<Integer> stateList) {
        if (stateList.size() == NumberConstant.NUMBER_1 && stateList.contains(DataTeamSubtaskStateCodeConstant.NOT_ANNOTATION_SUBTASK_STATE)){
            return DataTeamTaskStateEnum.NOT_ANNOTATION_TASK_STATE;
        }
        return null;
    }

    // 标注中 标注中，未标注，已提交（同时拥有3个状态中的两个以上） 或者 所有都是标注中
    public DataTeamTaskStateEnum isAnnotating(List<Integer> stateList) {
        if (stateList.size() > NumberConstant.NUMBER_1 || (stateList.size() == NumberConstant.NUMBER_1 && stateList.contains(DataTeamSubtaskStateCodeConstant.MANUAL_ANNOTATION_SUBTASK_STATE))) {
            return DataTeamTaskStateEnum.MANUAL_ANNOTATION_TASK_STATE;
        }
        return null;
    }

    // 已完成 全是已提交
    public DataTeamTaskStateEnum isFinished(List<Integer> stateList) {
        if (stateList.size() == NumberConstant.NUMBER_1 && stateList.contains(DataTeamSubtaskStateCodeConstant.FINISH_ANNOTATION_SUBTASK_STATE)){
            return DataTeamTaskStateEnum.FINISH_ANNOTATION_TASK_STATE;
        }
        return null;
    }

    // 已终止 （待定）
    public DataTeamTaskStateEnum isTerminated(List<Integer> stateList) {
        return null;
    }

}