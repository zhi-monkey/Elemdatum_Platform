package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：PENDING_DEPLOY（待下发，预留）
 * <p>打包完成后进入此状态，等待下发逻辑实现后处理。</p>
 * <p>可接受的事件：jobCompleteEvent（跳过下发直接完成）/ jobCancelEvent</p>
 */
@Component("jobPendingDeployState")
public class JobPendingDeployState extends AbstractSelfIterationJobState {
}
