package org.dlut.adv.mineai.model.statusMachine.state;

import org.dlut.adv.mineai.core.entity.SelfIterationTask;
import org.springframework.stereotype.Component;

/**
 * 自迭代训练父任务抽象状态类
 * <p>
 * 定义父任务状态机的所有事件方法，子类按需覆盖。
 * 状态流转：IDLE → RUNNING ⇄ PAUSED → COMPLETED / FAILED / CANCELLED
 * </p>
 */
@Component
public abstract class AbstractSelfIterationTaskState {

    /**
     * IDLE → RUNNING：手动触发第一轮迭代
     *
     * @param taskId 父任务 ID
     */
    public void taskStartEvent(Long taskId) {
    }

    /**
     * RUNNING → PAUSED：子任务进入等待人工审核状态
     *
     * @param taskId 父任务 ID
     */
    public void taskPauseForReviewEvent(Long taskId) {
    }

    /**
     * PAUSED → RUNNING：人工审核确认完成，继续执行当前轮次
     *
     * @param taskId 父任务 ID
     */
    public void taskResumeEvent(Long taskId) {
    }

    /**
     * RUNNING → COMPLETED：达到最大轮次或手动标记完成
     *
     * @param taskId 父任务 ID
     */
    public void taskCompleteEvent(Long taskId) {
    }

    /**
     * RUNNING/PAUSED → FAILED：子任务失败导致父任务终止
     *
     * @param taskId 父任务 ID
     */
    public void taskFailEvent(Long taskId) {
    }

    /**
     * 任意活跃状态 → CANCELLED：手动取消父任务
     *
     * @param taskId 父任务 ID
     */
    public void taskCancelEvent(Long taskId) {
    }
}
