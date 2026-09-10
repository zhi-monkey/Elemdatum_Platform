package org.dlut.adv.mineai.model.statusMachine.state.specific;

import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.enums.ModelJobEnum;
import org.dlut.adv.mineai.model.statusMachine.state.AbstractModelJobState;
import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.statusMachine.statemachine.StateMachineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @description 转换中类
 * @date
 */
@Component
public class ConvertingState extends AbstractModelJobState {

    @Autowired
    private ModelJobRepo modelJobRepo;

    /**
     * 转换中   转换中-->手动中止任务事件-->取消
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
    /**
     * 转换中   转换中-->转换成功-->转换成功
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobConvertSucceededEvent(Integer primaryKeyId) {
        System.out.println("转换成功:"+primaryKeyId+ ModelJobEnum.CONVERT_SUCCEEDED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.CONVERT_SUCCEEDED.getCode());
        modelJobRepo.save(modelJob);
    }
    /**
     * 转换中   转换中-->转换失败-->转换失败
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobConvertFailedEvent(Integer primaryKeyId) {
        System.out.println("转换失败:"+primaryKeyId+ ModelJobEnum.CONVERT_FAILED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.CONVERT_FAILED.getCode());
        modelJobRepo.save(modelJob);
    }
}
