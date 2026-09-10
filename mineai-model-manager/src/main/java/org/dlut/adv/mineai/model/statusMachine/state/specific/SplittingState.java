package org.dlut.adv.mineai.model.statusMachine.state.specific;

import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.enums.ModelJobEnum;
import org.dlut.adv.mineai.model.statusMachine.state.AbstractModelJobState;
import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.statusMachine.statemachine.StateMachineFactory;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @description 数据切分中类
 * @date
 */
@Component
public class SplittingState extends AbstractModelJobState {

    @Autowired
    private ModelJobRepo modelJobRepo;

    /**
     * 数据切分中   数据切分中-->切分成功（省略）-->开始排队事件-->排队中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobQueueEvent(Integer primaryKeyId) {
        System.out.println("排队中:"+primaryKeyId+ ModelJobEnum.QUEUING.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.QUEUING.getCode());
        modelJobRepo.save(modelJob);
    }

    /**
     * 数据切分中   数据切分中-->切分失败事件-->切分失败（省略）-->训练失败
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobSplitFailEvent(Integer primaryKeyId) {
        System.out.println("切分失败:"+primaryKeyId+ ModelJobEnum.TRAIN_FAILED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.TRAIN_FAILED.getCode());
        modelJobRepo.save(modelJob);
//        modelJobStateMachine.setMemoryModelJobState(modelJobStateMachine.getQueueSuccessState());
    }

    /**
     * 数据切分中   数据切分中-->手动中止任务事件-->取消
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobTrainCanceledEvent(Integer primaryKeyId) {
        System.out.println("任务取消:"+primaryKeyId+ ModelJobEnum.CANCELED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.CANCELED.getCode());
        modelJobRepo.save(modelJob);
    }
}
