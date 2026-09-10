package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：CREATED（已创建，待启动）
 * <p>可接受的事件：jobStartCollectEvent / jobCancelEvent</p>
 */
@Component("jobCreatedState")
public class JobCreatedState extends AbstractSelfIterationJobState {
}
