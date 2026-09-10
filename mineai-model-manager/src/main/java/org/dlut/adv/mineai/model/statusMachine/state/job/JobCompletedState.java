package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：COMPLETED（本轮完成）
 * <p>终态（本轮迭代正常完成，父任务准备启动下一轮或进入 COMPLETED）。</p>
 */
@Component("jobCompletedState")
public class JobCompletedState extends AbstractSelfIterationJobState {
}
