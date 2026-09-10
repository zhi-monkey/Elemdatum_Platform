package org.dubhe.data.machine.statemachine;

import lombok.Data;
import org.dubhe.biz.base.utils.SpringContextHolder;
import org.dubhe.biz.statemachine.exception.StateMachineException;
import org.dubhe.data.dao.DataTeamSubtaskMapper;
import org.dubhe.data.domain.entity.DataTeamSubtask;
import org.dubhe.data.machine.constant.ErrorMessageConstant;
import org.dubhe.data.machine.enums.DataTeamSubtaskStateEnum;
import org.dubhe.data.machine.state.AbstractDataTeamSubtaskState;
import org.dubhe.data.machine.state.specific.subtask.AnnotationCompleteSubtaskState;
import org.dubhe.data.machine.state.specific.subtask.AnnotationTerminatedSubtaskState;
import org.dubhe.data.machine.state.specific.subtask.ManualAnnotationSubtaskState;
import org.dubhe.data.machine.state.specific.subtask.NotAnnotationSubtaskState;
import org.dubhe.data.machine.utils.StateIdentifyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @description 子任务状态机
 * @date 2020-08-27
 */
@Data
@Component
public class DataTeamSubtaskStateMachine extends AbstractDataTeamSubtaskState implements Serializable {

    /**
     * 未标注
     */
    @Autowired
    private NotAnnotationSubtaskState notAnnotationSubtaskState;

    /**
     * 标注中
     */
    @Autowired
    private ManualAnnotationSubtaskState manualAnnotationSubtaskState;

    /**
     * 已提交
     */
    @Autowired
    private AnnotationCompleteSubtaskState annotationCompleteSubtaskState;

    /**
     * 已终止
     */
    @Autowired
    private AnnotationTerminatedSubtaskState annotationTerminatedSubtaskState;


    @Autowired
    private DataTeamSubtaskMapper dataTeamSubtaskMapper;

    /**
     * 内存中的状态机
     */
    private AbstractDataTeamSubtaskState memorySubtaskState;


    @Autowired
    private StateIdentifyUtil stateIdentify;


    // 改变子任务的状态
    public void doStateChange(Long subtaskId, Integer status, AbstractDataTeamSubtaskState memorySubtaskState) {
        dataTeamSubtaskMapper.updateStatus(subtaskId, status);
        this.memorySubtaskState = memorySubtaskState;
    }


    // 初始化状态机的状态
    public DataTeamSubtask initMemoryDataState(Integer primaryKeyId) {
        if (primaryKeyId == null) {
            throw new StateMachineException("未找到业务ID");
        }
        DataTeamSubtask dataTeamSubtask = dataTeamSubtaskMapper.selectById(primaryKeyId);
        if (dataTeamSubtask == null || dataTeamSubtask.getStatus() == null) {
            throw new StateMachineException("未找到业务数据");
        }
        memorySubtaskState = SpringContextHolder.getBean(DataTeamSubtaskStateEnum.getStateMachine(dataTeamSubtask.getStatus()));
        return dataTeamSubtask;
    }


    // 手动标注保存 进行标注，点击保存-->标注中
    @Override
    public void finishManualSubtaskEvent(DataTeamSubtask subtask) {
        initMemoryDataState(subtask.getId().intValue());
        if (
                memorySubtaskState != notAnnotationSubtaskState &&
                        memorySubtaskState != manualAnnotationSubtaskState
        ) {
            throw new StateMachineException(ErrorMessageConstant.SUBTASK_CHANGE_ERR_MESSAGE);
        }
        memorySubtaskState.finishManualSubtaskEvent(subtask);
    }

    // 标注终止 未标注/标注中/已提交->已终止
    @Override
    public void annotationTerminateSubtaskEvent(DataTeamSubtask subtask) {
        initMemoryDataState(subtask.getId().intValue());
        if (memorySubtaskState == annotationTerminatedSubtaskState) {
            return;
        } else if (
                memorySubtaskState != notAnnotationSubtaskState &&
                        memorySubtaskState != manualAnnotationSubtaskState &&
                            memorySubtaskState != annotationCompleteSubtaskState
        ) {
            throw new StateMachineException(ErrorMessageConstant.SUBTASK_CHANGE_ERR_MESSAGE);
        }
        memorySubtaskState.annotationTerminateSubtaskEvent(subtask);
    }

    // 标注完成 标注中->已提交
    @Override
    public void annotationCompleteSubtaskEvent(DataTeamSubtask subtask) {
        initMemoryDataState(subtask.getId().intValue());
        if (memorySubtaskState == annotationCompleteSubtaskState) {
            return;
        } else if (
                memorySubtaskState != manualAnnotationSubtaskState
        ) {
            throw new StateMachineException(ErrorMessageConstant.SUBTASK_CHANGE_ERR_MESSAGE);
        }
        memorySubtaskState.annotationCompleteSubtaskEvent(subtask);
    }


}
