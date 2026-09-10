
package org.dubhe.data.machine.constant;

/**
 * @description 多人标注子任务状态机事件常量
 * @date 2020-08-27
 */
public class DataTeamSubtaskStateMachineConstant {

    private DataTeamSubtaskStateMachineConstant() {
    }


    /**
     * 标注子任务状态
     */
    public static final String DATA_TEAM_SUBTASK_STATE_MACHINE = "dataTeamSubtaskStateMachine";
    /**
     * 标注保存事件 进行标注，点击完成 未标注/标注中->标注中
     */
    public static final String DATA_TEAM_SUBTASK_ANNOTATION_COMPLETE_EVENT = "annotationCompleteSubtaskEvent";
    /**
     * 标注终止 未标注/标注中->已终止
     */
    public static final String DATA_TEAM_SUBTASK_ANNOTATION_TERMINATE_EVENT = "annotationTerminateSubtaskEvent";
    /**
     * 提交标注 标注中->已提交
     */
    public static final String DATA_TEAM_SUBTASK_ANNOTATION_FINISH_EVENT = "finishManualSubtaskEvent";


}