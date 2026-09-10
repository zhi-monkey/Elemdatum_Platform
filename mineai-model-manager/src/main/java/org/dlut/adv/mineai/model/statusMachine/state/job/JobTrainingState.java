package org.dlut.adv.mineai.model.statusMachine.state.job;

import org.dlut.adv.mineai.model.statusMachine.state.AbstractSelfIterationJobState;
import org.springframework.stereotype.Component;

/**
 * 子任务状态：TRAINING（训练中）
 * <p>底层对应一个真正的 ModelJob（jobType=TRAIN）。</p>
 * <p>可接受的事件：jobTrainDoneEvent / jobTrainFailedEvent / jobCancelEvent</p>
 */
@Component("jobTrainingState")
public class JobTrainingState extends AbstractSelfIterationJobState {
}
