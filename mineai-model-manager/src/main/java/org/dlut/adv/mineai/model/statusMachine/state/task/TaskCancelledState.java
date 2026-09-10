package org.dlut.adv.mineai.model.statusMachine.state.task;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationTaskState;
import org.springframework.stereotype.Component;

/**
 * 父任务状态：CANCELLED（已取消）
 * <p>终态，不接受进一步事件。</p>
 */
@Component("taskCancelledState")
public class TaskCancelledState extends AbstractSelfIterationTaskState {
}
