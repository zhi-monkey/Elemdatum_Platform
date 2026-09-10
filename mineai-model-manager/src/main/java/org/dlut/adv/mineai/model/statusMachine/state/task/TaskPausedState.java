package org.dlut.adv.mineai.model.statusMachine.state.task;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationTaskState;
import org.springframework.stereotype.Component;

/**
 * 父任务状态：PAUSED（暂停，等待人工审核）
 * <p>可接受的事件：taskResumeEvent / taskCancelEvent</p>
 */
@Component("taskPausedState")
public class TaskPausedState extends AbstractSelfIterationTaskState {
}
