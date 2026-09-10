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
 * @description 创建转换任务类
 * @date
 */
@Component
public class CreateConvertState extends AbstractModelJobState {

    @Autowired
    private ModelJobRepo modelJobRepo;

    /**
     * 创建转换任务   创建转换任务-->转换开始事件->转换中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobConvertEvent(Integer primaryKeyId) {
        System.out.println("转换开始事件中:"+primaryKeyId+ ModelJobEnum.TRAINING.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.TRAINING.getCode());
        modelJobRepo.save(modelJob);
    }
}
