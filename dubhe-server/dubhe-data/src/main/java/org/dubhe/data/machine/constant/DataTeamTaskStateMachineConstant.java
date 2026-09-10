
package org.dubhe.data.machine.constant;

/**
 * @description 多人标注任务状态机事件常量
 * @date 2020-08-27
 */
public class DataTeamTaskStateMachineConstant {

    private DataTeamTaskStateMachineConstant() {
    }


    /**
     * 标注任务状态
     */
    public static final String DATA_TEAM_TASK_STATE_MACHINE = "dataTeamTaskStateMachine";
    /**
     * 标注保存事件 进行标注，点击完成 未标注/标注中->标注中
     */
    public static final String DATA_TEAM_TASK_ANNOTATION_COMPLETE_EVENT = "annotationCompleteTaskEvent";
    /**
     * 标注终止 未标注/标注中->已终止
     */
    public static final String DATA_TEAM_TASK_ANNOTATION_TERMINATE_EVENT = "annotationTerminateTaskEvent";
    /**
     * 提交标注 标注中->已提交
     */
    public static final String DATA_TEAM_TASK_ANNOTATION_FINISH_EVENT = "finishManualTaskEvent";


}