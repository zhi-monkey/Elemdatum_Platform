package org.dlut.adv.mineai.model.statusMachine.constant;

/**
 * 自迭代训练父任务状态码常量
 *
 * @see org.dlut.adv.mineai.core.entity.SelfIterationTask
 */
public class SelfIterationTaskStatusConstant {

    private SelfIterationTaskStatusConstant() {
    }

    /** 空闲/初始化，未开始 */
    public static final Integer IDLE = 0;

    /** 运行中（某轮子任务进行中）*/
    public static final Integer RUNNING = 1;

    /** 暂停（等待人工审核）*/
    public static final Integer PAUSED = -1;

    /** 已完成（达到最大轮次或手动停止）*/
    public static final Integer COMPLETED = 2;

    /** 失败（子任务失败后父任务停止）*/
    public static final Integer FAILED = -2;

    /** 已取消 */
    public static final Integer CANCELLED = -10;
}
