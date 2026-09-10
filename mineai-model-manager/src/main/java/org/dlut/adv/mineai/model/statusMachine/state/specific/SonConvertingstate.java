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
 * @description 子任务转换中类
 * @date
 */
@Component
public class SonConvertingstate extends AbstractModelJobState {
//    @Autowired
//    @Lazy
//    private StateMachineFactory stateMachineFactory;

    @Autowired
    private ModelJobRepo modelJobRepo;

//    public ModelJobStateMachine getStateMachine(Integer primaryKeyId) {
//        return stateMachineFactory.getStateMachine(String.valueOf(primaryKeyId));
//    }

    /**
     * xx中   xx中-->手动中止任务事件-->取消
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobSonTrainCanceledEvent(Integer primaryKeyId) {
        System.out.println("子任务取消:"+primaryKeyId+ ModelJobEnum.TRAIN_SUCCEEDED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.TRAIN_SUCCEEDED.getCode());
        modelJobRepo.save(modelJob);
    }
    /**
     * 子任务转换中   子任务转换中-->训练成功结束-->训练成功
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobTrainSucceededEvent(Integer primaryKeyId) {
        System.out.println("子任务转换成功:"+primaryKeyId+ ModelJobEnum.TRAIN_SUCCEEDED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.TRAIN_SUCCEEDED.getCode());
        modelJobRepo.save(modelJob);
    }
//    /**
//     * 子任务转换中   子任务转换中-->训练失败-->训练失败
//     *
//     * @param primaryKeyId 业务ID
//     */
//    @Override
//    public void modelJobTrainFailedEvent(Integer primaryKeyId) {
//        System.out.println("子任务转换失败:"+primaryKeyId+ ModelJobEnum.TRAIN_FAILED.getCode());
//        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
//        modelJob.setStatus(ModelJobEnum.TRAIN_FAILED.getCode());
//        modelJobRepo.save(modelJob);
//    }
}
