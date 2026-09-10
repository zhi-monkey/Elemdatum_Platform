package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：AUTO_LABELING（自动标注中）
 * <p>可接受的事件：
 * <ul>
 *   <li>jobLabelDoneEvent（无需审核，直接进入训练）</li>
 *   <li>jobLabelNeedReviewEvent（需人工审核）</li>
 *   <li>jobLabelFailedEvent（标注失败）</li>
 *   <li>jobCancelEvent</li>
 * </ul>
 * </p>
 */
@Component("jobAutoLabelingState")
public class JobAutoLabelingState extends AbstractSelfIterationJobState {
}
