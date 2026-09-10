package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：CONVERTING（转换中）
 * <p>底层对应一个真正的 ModelJob（jobType=CONVERT）。</p>
 * <p>可接受的事件：jobConvertDoneEvent / jobTrainFailedEvent / jobCancelEvent</p>
 */
@Component("jobConvertingState")
public class JobConvertingState extends AbstractSelfIterationJobState {
}
