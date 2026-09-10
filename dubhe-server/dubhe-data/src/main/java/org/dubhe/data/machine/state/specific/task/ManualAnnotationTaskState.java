
package org.dubhe.data.machine.state.specific.task;

import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.statemachine.exception.StateMachineException;
import org.dubhe.data.dao.DataTeamTaskMapper;
import org.dubhe.data.domain.entity.DataTeamTask;
import org.dubhe.data.machine.constant.ErrorMessageConstant;
import org.dubhe.data.machine.enums.DataTeamTaskStateEnum;
import org.dubhe.data.machine.state.AbstractDataTeamTaskState;
import org.dubhe.data.machine.statemachine.DataTeamTaskStateMachine;
import org.dubhe.data.machine.utils.StateIdentifyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @description 任务手动标注中状态类
 * @date 2020-08-27
 */
@Component
public class ManualAnnotationTaskState extends AbstractDataTeamTaskState {

    @Autowired
    @Lazy
    private DataTeamTaskStateMachine dataTeamTaskStateMachine;

    @Autowired
    private DataTeamTaskMapper dataTeamTaskMapper;

    @Autowired
    private StateIdentifyUtil stateIdentify;

    // 标注中->标注中/未标注
    @Override
    public void finishManualTaskEvent(DataTeamTask task){
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件前内存中状态机的状态 :{} ", dataTeamTaskStateMachine.getMemoryTaskState());
        LogUtil.debug(LogEnum.STATE_MACHINE, " 接受参数： {} ", task);
        DataTeamTaskStateEnum status = stateIdentify.getTaskStatus(task.getId(),true);
        switch (status){
            case MANUAL_ANNOTATION_TASK_STATE:
                //手动标注中
                break;
            case NOT_ANNOTATION_TASK_STATE:
                //未标注
                dataTeamTaskStateMachine.doStateChange(task.getId(),DataTeamTaskStateEnum.NOT_ANNOTATION_TASK_STATE.getCode(),dataTeamTaskStateMachine.getNotAnnotationTaskState());
                break;
            default:
                throw new StateMachineException(ErrorMessageConstant.TASK_CHANGE_ERR_MESSAGE);
        }
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件后内存状态机的切换： {}", dataTeamTaskStateMachine.getMemoryTaskState());
    }

    // 标注中->标注终止
    @Override
    public void annotationTerminateTaskEvent(DataTeamTask task){
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件前内存中状态机的状态 :{} ", dataTeamTaskStateMachine.getMemoryTaskState());
        LogUtil.debug(LogEnum.STATE_MACHINE, " 接受参数： {} ", task);
        dataTeamTaskMapper.updateStatus(task.getId(), DataTeamTaskStateEnum.TERMINATE_ANNOTATION_TASK_STATE.getCode());
        dataTeamTaskStateMachine.setMemoryTaskState(dataTeamTaskStateMachine.getAnnotationTerminatedTaskState());
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件后内存状态机的切换： {}", dataTeamTaskStateMachine.getMemoryTaskState());
    }

    // 标注中->标注完成
    @Override
    public void annotationCompleteTaskEvent(DataTeamTask task){
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件前内存中状态机的状态 :{} ", dataTeamTaskStateMachine.getMemoryTaskState());
        LogUtil.debug(LogEnum.STATE_MACHINE, " 接受参数： {} ", task);
        DataTeamTaskStateEnum status = stateIdentify.getTaskStatus(task.getId(),true);
        switch (status){
            case MANUAL_ANNOTATION_TASK_STATE:
                //手动标注中
                break;
            case FINISH_ANNOTATION_TASK_STATE:
                //已标注
                dataTeamTaskStateMachine.doStateChange(task.getId(),DataTeamTaskStateEnum.FINISH_ANNOTATION_TASK_STATE.getCode(),dataTeamTaskStateMachine.getAnnotationCompleteTaskState());
                break;
            default:
                throw new StateMachineException(ErrorMessageConstant.TASK_CHANGE_ERR_MESSAGE);
        }
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件后内存状态机的切换： {}", dataTeamTaskStateMachine.getMemoryTaskState());
    }
}