package org.dlut.adv.mineai.model.statusMachine.state.task;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationTaskState;
import org.springframework.stereotype.Component;

/**
 * 父任务状态：IDLE（空闲/初始化）
 * <p>可接受的事件：taskStartEvent</p>
 */
@Component("taskIdleState")
public class TaskIdleState extends AbstractSelfIterationTaskState {
}
