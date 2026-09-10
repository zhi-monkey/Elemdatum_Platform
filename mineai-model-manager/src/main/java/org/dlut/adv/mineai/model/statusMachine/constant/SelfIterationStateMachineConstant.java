package org.dlut.adv.mineai.model.statusMachine.constant;

/**
 * 自迭代训练状态机事件名称常量
 *
 * <p>事件命名规则：SELFIT_{主体}_{动作}_EVENT</p>
 */
public class SelfIterationStateMachineConstant {

    private SelfIterationStateMachineConstant() {
    }

    // ===================== 父任务（Task）事件 =====================

    /** 父任务状态机 Bean 名称 */
    public static final String SELF_ITERATION_TASK_STATE_MACHINE = "selfIterationTaskStateMachine";

    /**
     * IDLE → RUNNING：手动触发第一轮迭代
     */
    public static final String TASK_START_EVENT = "taskStartEvent";

    /**
     * RUNNING → PAUSED：子任务进入等待人工审核
     */
    public static final String TASK_PAUSE_FOR_REVIEW_EVENT = "taskPauseForReviewEvent";

    /**
     * PAUSED → RUNNING：人工审核确认完成，继续执行
     */
    public static final String TASK_RESUME_EVENT = "taskResumeEvent";

    /**
     * RUNNING → COMPLETED：达到最大轮次或手动标记完成
     */
    public static final String TASK_COMPLETE_EVENT = "taskCompleteEvent";

    /**
     * RUNNING/PAUSED → FAILED：子任务失败导致父任务终止
     */
    public static final String TASK_FAIL_EVENT = "taskFailEvent";

    /**
     * 任意活跃状态 → CANCELLED：手动取消
     */
    public static final String TASK_CANCEL_EVENT = "taskCancelEvent";

    // ===================== 子任务（Job）事件 =====================

    /** 子任务状态机 Bean 名称 */
    public static final String SELF_ITERATION_JOB_STATE_MACHINE = "selfIterationJobStateMachine";

    /**
     * CREATED → COLLECTING：开始数据采集
     */
    public static final String JOB_START_COLLECT_EVENT = "jobStartCollectEvent";

    /**
     * COLLECTING → COLLECT_FAILED：数据采集失败
     */
    public static final String JOB_COLLECT_FAILED_EVENT = "jobCollectFailedEvent";

    /**
     * COLLECTING → AUTO_LABELING：数据采集完成，开始自动标注
     */
    public static final String JOB_COLLECT_DONE_EVENT = "jobCollectDoneEvent";

    /**
     * AUTO_LABELING → WAITING_REVIEW：自动标注完成，需要人工审核（requireManualReview=true）
     */
    public static final String JOB_LABEL_NEED_REVIEW_EVENT = "jobLabelNeedReviewEvent";

    /**
     * AUTO_LABELING / WAITING_REVIEW → TRAINING：自动标注完成（无需审核）或审核确认完成
     */
    public static final String JOB_LABEL_DONE_EVENT = "jobLabelDoneEvent";

    /**
     * AUTO_LABELING → ANNOTATE_FAILED：自动标注失败
     */
    public static final String JOB_LABEL_FAILED_EVENT = "jobLabelFailedEvent";

    /**
     * TRAINING → CONVERTING：训练完成，开始转换
     */
    public static final String JOB_TRAIN_DONE_EVENT = "jobTrainDoneEvent";

    /**
     * TRAINING / CONVERTING → TRAIN_FAILED：训练或转换失败
     */
    public static final String JOB_TRAIN_FAILED_EVENT = "jobTrainFailedEvent";

    /**
     * CONVERTING → PACKAGING：转换完成，开始打包
     */
    public static final String JOB_CONVERT_DONE_EVENT = "jobConvertDoneEvent";

    /**
     * PACKAGING → PACKAGE_FAILED：打包失败
     */
    public static final String JOB_PACKAGE_FAILED_EVENT = "jobPackageFailedEvent";

    /**
     * PACKAGING → PENDING_DEPLOY：打包完成，进入待下发（预留）
     */
    public static final String JOB_PACKAGE_DONE_EVENT = "jobPackageDoneEvent";

    /**
     * PENDING_DEPLOY → COMPLETED：下发完成（或跳过下发直接完成）
     */
    public static final String JOB_COMPLETE_EVENT = "jobCompleteEvent";

    /**
     * 任意活跃状态 → CANCELLED：手动取消本轮
     */
    public static final String JOB_CANCEL_EVENT = "jobCancelEvent";
}
