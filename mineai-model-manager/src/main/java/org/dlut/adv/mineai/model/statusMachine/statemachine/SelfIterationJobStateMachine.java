package org.dlut.adv.mineai.model.statusMachine.statemachine;

import lombok.Data;
import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.stereotype.Component;

/**
 * 自迭代训练子任务状态机（一轮迭代）
 *
 * <p>负责子任务（SelfIterationJob）的所有状态事件派发。
 * 模式与 {@link ModelJobStateMachine} 保持一致。</p>
 *
 * <pre>
 * 状态流转：
 * CREATED (0)
 *   ↓ jobStartCollectEvent
 * COLLECTING (1)   ─ jobCollectFailedEvent → COLLECT_FAILED (11)
 *   ↓ jobCollectDoneEvent
 * AUTO_LABELING (2)
 *   ├─ jobLabelNeedReviewEvent → WAITING_REVIEW (21)  [父任务同步 PAUSED]
 *   │     ↓ jobLabelDoneEvent（审核确认）
 *   ├─ jobLabelDoneEvent（无需审核）
 *   └─ jobLabelFailedEvent → ANNOTATE_FAILED (22)
 *        ↓
 * TRAINING (3)     ─ jobTrainFailedEvent → TRAIN_FAILED (32)
 *   ↓ jobTrainDoneEvent
 * CONVERTING (31)  ─ jobTrainFailedEvent → TRAIN_FAILED (32)
 *   ↓ jobConvertDoneEvent
 * PACKAGING (4)    ─ jobPackageFailedEvent → PACKAGE_FAILED (41)
 *   ↓ jobPackageDoneEvent
 * PENDING_DEPLOY (5)  [预留]
 *   ↓ jobCompleteEvent
 * COMPLETED (6)
 * </pre>
 */
@Data
@Component("selfIterationJobStateMachine")
public class SelfIterationJobStateMachine extends AbstractSelfIterationJobState {

    /** 内存中维护的当前状态实例 */
    private AbstractSelfIterationJobState memoryState;

    /**
     * CREATED → COLLECTING：开始数据采集
     */
    @Override
    public void jobStartCollectEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobCreatedState");
        memoryState.jobStartCollectEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobCollectingState");
    }

    /**
     * COLLECTING → COLLECT_FAILED：数据采集失败
     */
    @Override
    public void jobCollectFailedEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobCollectingState");
        memoryState.jobCollectFailedEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobCollectFailedState");
    }

    /**
     * COLLECTING → AUTO_LABELING：数据采集完成，开始自动标注
     */
    @Override
    public void jobCollectDoneEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobCollectingState");
        memoryState.jobCollectDoneEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobAutoLabelingState");
    }

    /**
     * AUTO_LABELING → WAITING_REVIEW：自动标注完成，需要人工审核
     * （调用方需同步触发父任务的 taskPauseForReviewEvent）
     */
    @Override
    public void jobLabelNeedReviewEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobAutoLabelingState");
        memoryState.jobLabelNeedReviewEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobWaitingReviewState");
    }

    /**
     * AUTO_LABELING / WAITING_REVIEW → TRAINING：标注完成（无需审核或审核确认）
     */
    @Override
    public void jobLabelDoneEvent(Long jobId) {
        if (memoryState == null) {
            // Defensive fallback: review confirm may happen in a different request lifecycle.
            memoryState = SpringContextHolder.getBean("jobAutoLabelingState");
        }
        memoryState.jobLabelDoneEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobTrainingState");
    }

    /**
     * AUTO_LABELING → ANNOTATE_FAILED：自动标注失败
     */
    @Override
    public void jobLabelFailedEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobAutoLabelingState");
        memoryState.jobLabelFailedEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobAnnotateFailedState");
    }

    /**
     * TRAINING → CONVERTING：训练完成，开始转换
     */
    @Override
    public void jobTrainDoneEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobTrainingState");
        memoryState.jobTrainDoneEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobConvertingState");
    }

    /**
     * TRAINING / CONVERTING → TRAIN_FAILED：训练或转换失败
     */
    @Override
    public void jobTrainFailedEvent(Long jobId) {
        memoryState.jobTrainFailedEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobTrainFailedState");
    }

    /**
     * CONVERTING → PACKAGING：转换完成，开始打包
     */
    @Override
    public void jobConvertDoneEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobConvertingState");
        memoryState.jobConvertDoneEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobPackagingState");
    }

    /**
     * PACKAGING → PACKAGE_FAILED：打包失败
     */
    @Override
    public void jobPackageFailedEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobPackagingState");
        memoryState.jobPackageFailedEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobPackageFailedState");
    }

    /**
     * PACKAGING → PENDING_DEPLOY：打包完成，进入待下发（预留）
     */
    @Override
    public void jobPackageDoneEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobPackagingState");
        memoryState.jobPackageDoneEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobPendingDeployState");
    }

    /**
     * PENDING_DEPLOY → COMPLETED：完成（下发完成或跳过下发）
     */
    @Override
    public void jobCompleteEvent(Long jobId) {
        memoryState = SpringContextHolder.getBean("jobPendingDeployState");
        memoryState.jobCompleteEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobCompletedState");
    }

    /**
     * 任意活跃状态 → CANCELLED：手动取消本轮
     */
    @Override
    public void jobCancelEvent(Long jobId) {
        memoryState.jobCancelEvent(jobId);
        memoryState = SpringContextHolder.getBean("jobCancelledState");
    }
}
