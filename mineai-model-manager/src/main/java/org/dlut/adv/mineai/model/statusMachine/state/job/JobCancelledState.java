package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：CANCELLED（已取消）
 * <p>终态，任意活跃状态可通过 jobCancelEvent 转入此状态。</p>
 */
@Component("jobCancelledState")
public class JobCancelledState extends AbstractSelfIterationJobState {
}
