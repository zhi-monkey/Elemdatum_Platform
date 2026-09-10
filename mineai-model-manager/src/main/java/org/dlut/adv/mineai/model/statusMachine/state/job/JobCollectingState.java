package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：COLLECTING（数据采集中）
 * <p>可接受的事件：jobCollectDoneEvent / jobCollectFailedEvent / jobCancelEvent</p>
 */
@Component("jobCollectingState")
public class JobCollectingState extends AbstractSelfIterationJobState {
}
