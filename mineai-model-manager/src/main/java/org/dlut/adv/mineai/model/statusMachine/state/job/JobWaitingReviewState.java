package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：WAITING_REVIEW（等待人工审核）
 * <p>此状态下父任务同步处于 PAUSED。</p>
 * <p>可接受的事件：jobLabelDoneEvent（审核确认完成）/ jobCancelEvent</p>
 */
@Component("jobWaitingReviewState")
public class JobWaitingReviewState extends AbstractSelfIterationJobState {
}
