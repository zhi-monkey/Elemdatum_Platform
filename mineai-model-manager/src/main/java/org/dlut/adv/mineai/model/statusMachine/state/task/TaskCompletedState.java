package org.dlut.adv.mineai.model.statusMachine.state.task;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationTaskState;
import org.springframework.stereotype.Component;

/**
 * 父任务状态：COMPLETED（已完成）
 * <p>终态，不接受进一步事件。</p>
 */
@Component("taskCompletedState")
public class TaskCompletedState extends AbstractSelfIterationTaskState {
}
