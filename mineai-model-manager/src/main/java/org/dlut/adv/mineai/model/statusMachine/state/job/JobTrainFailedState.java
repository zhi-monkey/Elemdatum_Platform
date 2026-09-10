package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：TRAIN_FAILED（训练或转换失败）
 * <p>终态（当前轮次结束，父任务转入 FAILED）。</p>
 */
@Component("jobTrainFailedState")
public class JobTrainFailedState extends AbstractSelfIterationJobState {
}
