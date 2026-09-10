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
 * @description 排队中类
 * @date
 */
@Component
public class QueueingState extends AbstractModelJobState {

    @Autowired
    private ModelJobRepo modelJobRepo;

    /**
     * 排队中   排队中-->排队成功（省略）-->开始训练事件-->训练中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobTrainEvent(Integer primaryKeyId) {
        System.out.println("训练中:"+primaryKeyId+ ModelJobEnum.TRAINING.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.TRAINING.getCode());
        modelJobRepo.save(modelJob);
    }
//    /**
//     * 排队中   排队中-->手动中止任务事件-->取消
//     *
//     * @param primaryKeyId 业务ID
//     */
//    @Override
//    public void modelJobTrainCanceledEvent(Integer primaryKeyId) {
//        System.out.println("任务取消:"+primaryKeyId+ ModelJobEnum.CANCELED.getCode());
//        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
//        modelJob.setStatus(ModelJobEnum.CANCELED.getCode());
//        modelJobRepo.save(modelJob);
//    }
//    /**
//     * 排队中   排队中-->训练失败-->训练失败
//     *
//     * @param primaryKeyId 业务ID
//     */
//    @Override
//    public void modelJobTrainFailedEvent(Integer primaryKeyId) {
//        System.out.println("训练失败:"+primaryKeyId+ ModelJobEnum.TRAIN_FAILED.getCode());
//        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
//        modelJob.setStatus(ModelJobEnum.TRAIN_FAILED.getCode());
//        modelJobRepo.save(modelJob);
//    }
}
