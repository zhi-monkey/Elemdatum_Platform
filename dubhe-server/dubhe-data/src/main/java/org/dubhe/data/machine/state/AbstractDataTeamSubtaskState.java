
package org.dubhe.data.machine.state;

import org.dubhe.data.domain.entity.DataTeamSubtask;

/**
 * @description 多人标注子任务状态类
 * @date 2020-08-27
 */
public abstract class AbstractDataTeamSubtaskState {



    // 提交标注 标注中->已提交
    public void annotationCompleteSubtaskEvent(DataTeamSubtask subtask) {
    }


    // 标注终止 未标注/标注中/已提交->已终止
    public void annotationTerminateSubtaskEvent(DataTeamSubtask subtask) {
    }


    // 标注保存事件 进行标注，点击完成 未标注/标注中->标注中
    public void finishManualSubtaskEvent(DataTeamSubtask subtask){
    }




}
