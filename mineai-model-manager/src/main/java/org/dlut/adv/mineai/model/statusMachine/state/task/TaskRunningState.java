package org.dlut.adv.mineai.model.statusMachine.state.task;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationTaskState;
import org.springframework.stereotype.Component;

/**
 * 父任务状态：RUNNING（运行中）
 * <p>可接受的事件：taskPauseForReviewEvent / taskCompleteEvent / taskFailEvent / taskCancelEvent</p>
 */
@Component("taskRunningState")
public class TaskRunningState extends AbstractSelfIterationTaskState {
}
