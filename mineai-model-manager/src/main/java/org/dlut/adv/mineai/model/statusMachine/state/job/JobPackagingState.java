package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：PACKAGING（打包中）
 * <p>底层调用 mineai-package-manager 的 TaskService。</p>
 * <p>可接受的事件：jobPackageDoneEvent / jobPackageFailedEvent / jobCancelEvent</p>
 */
@Component("jobPackagingState")
public class JobPackagingState extends AbstractSelfIterationJobState {
}
