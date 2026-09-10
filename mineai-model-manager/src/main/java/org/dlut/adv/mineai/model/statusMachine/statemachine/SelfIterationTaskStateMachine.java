package org.dlut.adv.mineai.model.statusMachine.statemachine;

import lombok.Data;
import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationTaskState;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.stereotype.Component;

/**
 * 自迭代训练父任务状态机
 *
 * <p>负责父任务（SelfIterationTask）的所有状态事件派发。
 * 模式与 {@link ModelJobStateMachine} 保持一致：
 * 每次事件先将 memoryState 设置为对应的源状态 Bean，执行事件方法，再切换到目标状态 Bean。</p>
 *
 * <pre>
 * 状态流转：
 * IDLE (0)
 *   ↓ taskStartEvent
 * RUNNING (1)
 *   ├─ taskPauseForReviewEvent → PAUSED (-1)
 *   │     ↓ taskResumeEvent
 *   │   RUNNING (1)
 *   ├─ taskCompleteEvent → COMPLETED (2)
 *   ├─ taskFailEvent     → FAILED (-2)
 *   └─ taskCancelEvent   → CANCELLED (-10)
 * </pre>
 */
@Data
@Component("selfIterationTaskStateMachine")
public class SelfIterationTaskStateMachine extends AbstractSelfIterationTaskState {

    /** 内存中维护的当前状态实例 */
    private AbstractSelfIterationTaskState memoryState;

    /**
     * IDLE → RUNNING：手动触发第一轮迭代
     */
    @Override
    public void taskStartEvent(Long taskId) {
        memoryState = SpringContextHolder.getBean("taskIdleState");
        memoryState.taskStartEvent(taskId);
        memoryState = SpringContextHolder.getBean("taskRunningState");
    }

    /**
     * RUNNING → PAUSED：子任务进入等待人工审核
     */
    @Override
    public void taskPauseForReviewEvent(Long taskId) {
        memoryState = SpringContextHolder.getBean("taskRunningState");
        memoryState.taskPauseForReviewEvent(taskId);
        memoryState = SpringContextHolder.getBean("taskPausedState");
    }

    /**
     * PAUSED → RUNNING：人工审核确认完成，继续当前轮次
     */
    @Override
    public void taskResumeEvent(Long taskId) {
        memoryState = SpringContextHolder.getBean("taskPausedState");
        memoryState.taskResumeEvent(taskId);
        memoryState = SpringContextHolder.getBean("taskRunningState");
    }

    /**
     * RUNNING → COMPLETED：达到最大轮次或手动停止
     */
    @Override
    public void taskCompleteEvent(Long taskId) {
        memoryState = SpringContextHolder.getBean("taskRunningState");
        memoryState.taskCompleteEvent(taskId);
        memoryState = SpringContextHolder.getBean("taskCompletedState");
    }

    /**
     * RUNNING/PAUSED → FAILED：子任务失败导致父任务终止
     */
    @Override
    public void taskFailEvent(Long taskId) {
        memoryState.taskFailEvent(taskId);
        memoryState = SpringContextHolder.getBean("taskFailedState");
    }

    /**
     * 任意活跃状态 → CANCELLED：手动取消
     */
    @Override
    public void taskCancelEvent(Long taskId) {
        memoryState.taskCancelEvent(taskId);
        memoryState = SpringContextHolder.getBean("taskCancelledState");
    }
}
