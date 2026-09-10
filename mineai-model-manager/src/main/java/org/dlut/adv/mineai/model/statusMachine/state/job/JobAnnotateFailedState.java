package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：ANNOTATE_FAILED（标注失败）
 * <p>终态（当前轮次结束，父任务转入 FAILED）。</p>
 */
@Component("jobAnnotateFailedState")
public class JobAnnotateFailedState extends AbstractSelfIterationJobState {
}
