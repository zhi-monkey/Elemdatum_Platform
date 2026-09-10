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

@Component
public class TrainSucceededState extends AbstractModelJobState {

    @Autowired
    private ModelJobRepo modelJobRepo;

    /**
     * 训练成功   训练成功-->开始子任务转换事件->子任务转换中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobSonConvertEvent(Integer primaryKeyId) {
        System.out.println("开始子任务转换事件中:"+primaryKeyId+ ModelJobEnum.SON_CONVERTING.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.SON_CONVERTING.getCode());
        modelJobRepo.save(modelJob);
    }
    /**
     * 训练成功   训练成功-->开始发布事件-->已发布
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobTrainPublishedEvent(Integer primaryKeyId) {
        System.out.println("发布成功:"+primaryKeyId+ ModelJobEnum.PUBLISHED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.PUBLISHED.getCode());
        modelJobRepo.save(modelJob);
    }
}
