
package org.dubhe.data.machine.state.specific.subtask;

import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.statemachine.exception.StateMachineException;
import org.dubhe.data.dao.DataTeamSubtaskMapper;
import org.dubhe.data.dao.DatasetMapper;
import org.dubhe.data.domain.entity.DataTeamSubtask;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.machine.constant.ErrorMessageConstant;
import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.machine.enums.DataTeamSubtaskStateEnum;
import org.dubhe.data.machine.state.AbstractDataState;
import org.dubhe.data.machine.state.AbstractDataTeamSubtaskState;
import org.dubhe.data.machine.statemachine.DataStateMachine;
import org.dubhe.data.machine.statemachine.DataTeamSubtaskStateMachine;
import org.dubhe.data.machine.utils.StateIdentifyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @description 子任务未标注状态类
 * @date 2020-08-27
 */
@Component
public class NotAnnotationSubtaskState extends AbstractDataTeamSubtaskState {

    @Autowired
    @Lazy
    private DataTeamSubtaskStateMachine dataTeamSubtaskStateMachine;

    @Autowired
    private DataTeamSubtaskMapper dataTeamSubtaskMapper;

    @Autowired
    private StateIdentifyUtil stateIdentify;

    // 未标注->标注中
    @Override
    public void finishManualSubtaskEvent(DataTeamSubtask subtask){
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件前内存中状态机的状态 :{} ", dataTeamSubtaskStateMachine.getMemorySubtaskState());
        LogUtil.debug(LogEnum.STATE_MACHINE, " 接受参数： {} ", subtask);
        DataTeamSubtaskStateEnum status = stateIdentify.getSubtaskStatus(subtask.getId(),true);
        switch (status){
            case MANUAL_ANNOTATION_SUBTASK_STATE:
                //手动标注中
                dataTeamSubtaskStateMachine.doStateChange(subtask.getId(),DataTeamSubtaskStateEnum.MANUAL_ANNOTATION_SUBTASK_STATE.getCode(),dataTeamSubtaskStateMachine.getManualAnnotationSubtaskState());
                break;
            case NOT_ANNOTATION_SUBTASK_STATE:
                //未标注
                break;
            default:
                throw new StateMachineException(ErrorMessageConstant.SUBTASK_CHANGE_ERR_MESSAGE);
        }
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件后内存状态机的切换： {}", dataTeamSubtaskStateMachine.getMemorySubtaskState());
    }

    // 未标注->标注终止
    @Override
    public void annotationTerminateSubtaskEvent(DataTeamSubtask subtask){
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件前内存中状态机的状态 :{} ", dataTeamSubtaskStateMachine.getMemorySubtaskState());
        LogUtil.debug(LogEnum.STATE_MACHINE, " 接受参数： {} ", subtask);
        dataTeamSubtaskMapper.updateStatus(subtask.getId(), DataTeamSubtaskStateEnum.TERMINATE_ANNOTATION_SUBTASK_STATE.getCode());
        dataTeamSubtaskStateMachine.setMemorySubtaskState(dataTeamSubtaskStateMachine.getAnnotationTerminatedSubtaskState());
        LogUtil.debug(LogEnum.STATE_MACHINE, " 【未标注】 执行事件后内存状态机的切换： {}", dataTeamSubtaskStateMachine.getMemorySubtaskState());
    }

}
