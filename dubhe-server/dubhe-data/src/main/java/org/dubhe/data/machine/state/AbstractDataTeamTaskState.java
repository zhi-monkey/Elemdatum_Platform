
package org.dubhe.data.machine.state;

import org.dubhe.data.domain.entity.DataTeamTask;

/**
 * @description 多人标注任务状态类
 * @date 2020-08-27
 */
public abstract class AbstractDataTeamTaskState {


    // 提交标注 标注中->已提交
    public void annotationCompleteTaskEvent(DataTeamTask task) {
    }


    // 标注终止 未标注/标注中->已终止
    public void annotationTerminateTaskEvent(DataTeamTask task) {
    }


    // 标注保存事件 进行标注，点击完成 未标注/标注中->标注中
    public void finishManualTaskEvent(DataTeamTask task){
    }


}
