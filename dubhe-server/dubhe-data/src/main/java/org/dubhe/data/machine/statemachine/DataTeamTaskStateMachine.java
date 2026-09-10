package org.dubhe.data.machine.statemachine;

import lombok.Data;
import org.dubhe.biz.base.utils.SpringContextHolder;
import org.dubhe.biz.statemachine.exception.StateMachineException;
import org.dubhe.data.dao.DataTeamTaskMapper;
import org.dubhe.data.domain.entity.DataTeamTask;
import org.dubhe.data.machine.constant.ErrorMessageConstant;
import org.dubhe.data.machine.enums.DataTeamTaskStateEnum;
import org.dubhe.data.machine.state.AbstractDataTeamTaskState;
import org.dubhe.data.machine.state.specific.task.AnnotationCompleteTaskState;
import org.dubhe.data.machine.state.specific.task.AnnotationTerminatedTaskState;
import org.dubhe.data.machine.state.specific.task.ManualAnnotationTaskState;
import org.dubhe.data.machine.state.specific.task.NotAnnotationTaskState;
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
public class DataTeamTaskStateMachine extends AbstractDataTeamTaskState implements Serializable {

    /**
     * 未标注
     */
    @Autowired
    private NotAnnotationTaskState notAnnotationTaskState;

    /**
     * 标注中
     */
    @Autowired
    private ManualAnnotationTaskState manualAnnotationTaskState;

    /**
     * 已完成
     */
    @Autowired
    private AnnotationCompleteTaskState annotationCompleteTaskState;

    /**
     * 已终止
     */
    @Autowired
    private AnnotationTerminatedTaskState annotationTerminatedTaskState;


    @Autowired
    private DataTeamTaskMapper dataTeamTaskMapper;

    /**
     * 内存中的状态机
     */
    private AbstractDataTeamTaskState memoryTaskState;


    @Autowired
    private StateIdentifyUtil stateIdentify;


    // 改变任务的状态
    public void doStateChange(Long taskId, Integer status, AbstractDataTeamTaskState memoryTaskState) {
        dataTeamTaskMapper.updateStatus(taskId, status);
        this.memoryTaskState = memoryTaskState;
    }


    // 初始化状态机的状态
    public DataTeamTask initMemoryDataState(Integer primaryKeyId) {
        if (primaryKeyId == null) {
            throw new StateMachineException("未找到业务ID");
        }
        DataTeamTask dataTeamTask = dataTeamTaskMapper.selectById(primaryKeyId);
        if (dataTeamTask == null || dataTeamTask.getStatus() == null) {
            throw new StateMachineException("未找到业务数据");
        }
        memoryTaskState = SpringContextHolder.getBean(DataTeamTaskStateEnum.getStateMachine(dataTeamTask.getStatus()));
        return dataTeamTask;
    }


    // 手动标注保存 进行标注，点击保存-->标注中
    @Override
    public void finishManualTaskEvent(DataTeamTask task) {
        initMemoryDataState(task.getId().intValue());
        if (
                memoryTaskState != notAnnotationTaskState &&
                        memoryTaskState != manualAnnotationTaskState
        ) {
            throw new StateMachineException(ErrorMessageConstant.SUBTASK_CHANGE_ERR_MESSAGE);
        }
        memoryTaskState.finishManualTaskEvent(task);
    }

    // 标注终止 未标注/标注中->已终止
    @Override
    public void annotationTerminateTaskEvent(DataTeamTask task) {
        initMemoryDataState(task.getId().intValue());
        if (memoryTaskState == annotationTerminatedTaskState) {
            return;
        } else if (
                memoryTaskState != notAnnotationTaskState &&
                        memoryTaskState != manualAnnotationTaskState
        ) {
            throw new StateMachineException(ErrorMessageConstant.SUBTASK_CHANGE_ERR_MESSAGE);
        }
        memoryTaskState.annotationTerminateTaskEvent(task);
    }

    // 标注完成 标注中->已提交
    @Override
    public void annotationCompleteTaskEvent(DataTeamTask task) {
        initMemoryDataState(task.getId().intValue());
        if (memoryTaskState == annotationCompleteTaskState) {
            return;
        } else if (
                memoryTaskState != manualAnnotationTaskState
        ) {
            throw new StateMachineException(ErrorMessageConstant.SUBTASK_CHANGE_ERR_MESSAGE);
        }
        memoryTaskState.annotationCompleteTaskEvent(task);
    }


}
