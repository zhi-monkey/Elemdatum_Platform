package org.dlut.adv.mineai.model.statusMachine.state;

import org.springframework.stereotype.Component;

/**
 * 自迭代训练子任务抽象状态类
 * <p>
 * 定义子任务状态机的所有事件方法，子类按需覆盖。
 * 状态流转：
 * CREATED → COLLECTING → AUTO_LABELING → (WAITING_REVIEW →) TRAINING → CONVERTING → PACKAGING → PENDING_DEPLOY → COMPLETED
 * 失败分支：各阶段均可转入对应的失败状态或 CANCELLED
 * </p>
 */
@Component
public abstract class AbstractSelfIterationJobState {

    /**
     * CREATED → COLLECTING：开始数据采集
     *
     * @param jobId 子任务 ID
     */
    public void jobStartCollectEvent(Long jobId) {
    }

    /**
     * COLLECTING → COLLECT_FAILED：数据采集失败
     *
     * @param jobId 子任务 ID
     */
    public void jobCollectFailedEvent(Long jobId) {
    }

    /**
     * COLLECTING → AUTO_LABELING：数据采集完成，开始自动标注
     *
     * @param jobId 子任务 ID
     */
    public void jobCollectDoneEvent(Long jobId) {
    }

    /**
     * AUTO_LABELING → WAITING_REVIEW：自动标注完成，需要人工审核
     * （触发时同步通知父任务进入 PAUSED）
     *
     * @param jobId 子任务 ID
     */
    public void jobLabelNeedReviewEvent(Long jobId) {
    }

    /**
     * AUTO_LABELING / WAITING_REVIEW → TRAINING：
     * 无需审核直接完成，或人工审核已确认，进入训练阶段
     *
     * @param jobId 子任务 ID
     */
    public void jobLabelDoneEvent(Long jobId) {
    }

    /**
     * AUTO_LABELING → ANNOTATE_FAILED：自动标注失败
     *
     * @param jobId 子任务 ID
     */
    public void jobLabelFailedEvent(Long jobId) {
    }

    /**
     * TRAINING → CONVERTING：训练完成，开始转换
     *
     * @param jobId 子任务 ID
     */
    public void jobTrainDoneEvent(Long jobId) {
    }

    /**
     * TRAINING / CONVERTING → TRAIN_FAILED：训练或转换失败
     *
     * @param jobId 子任务 ID
     */
    public void jobTrainFailedEvent(Long jobId) {
    }

    /**
     * CONVERTING → PACKAGING：转换完成，开始打包
     *
     * @param jobId 子任务 ID
     */
    public void jobConvertDoneEvent(Long jobId) {
    }

    /**
     * PACKAGING → PACKAGE_FAILED：打包失败
     *
     * @param jobId 子任务 ID
     */
    public void jobPackageFailedEvent(Long jobId) {
    }

    /**
     * PACKAGING → PENDING_DEPLOY：打包完成，进入待下发（预留）
     *
     * @param jobId 子任务 ID
     */
    public void jobPackageDoneEvent(Long jobId) {
    }

    /**
     * PENDING_DEPLOY → COMPLETED：下发完成，或跳过下发直接完成
     *
     * @param jobId 子任务 ID
     */
    public void jobCompleteEvent(Long jobId) {
    }

    /**
     * 任意活跃状态 → CANCELLED：手动取消本轮迭代
     *
     * @param jobId 子任务 ID
     */
    public void jobCancelEvent(Long jobId) {
    }
}
