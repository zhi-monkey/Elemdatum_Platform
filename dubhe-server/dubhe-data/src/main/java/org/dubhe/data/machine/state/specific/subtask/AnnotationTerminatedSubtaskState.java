package org.dubhe.data.machine.state.specific.subtask;

import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.statemachine.exception.StateMachineException;
import org.dubhe.data.dao.DatasetMapper;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.machine.constant.ErrorMessageConstant;
import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.machine.state.AbstractDataState;
import org.dubhe.data.machine.state.AbstractDataTeamSubtaskState;
import org.dubhe.data.machine.statemachine.DataStateMachine;
import org.dubhe.data.machine.utils.StateIdentifyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @description 子任务标注终止状态类
 * @date 2020-08-27
 */
@Component
public class AnnotationTerminatedSubtaskState extends AbstractDataTeamSubtaskState {

}
