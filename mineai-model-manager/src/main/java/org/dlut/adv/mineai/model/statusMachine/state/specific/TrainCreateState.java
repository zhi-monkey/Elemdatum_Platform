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
 * @description 创建训练任务类
 * @date
 */
@Component
public class TrainCreateState extends AbstractModelJobState {

    @Autowired
    private ModelJobRepo modelJobRepo;

    /**
     * 训练任务创建  训练任务创建-->开始切分事件-->数据切分中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobSplitEvent(Integer primaryKeyId) {
        System.out.println("数据集切分中:"+primaryKeyId+ ModelJobEnum.SPLITTING.getCode());
        System.out.println("primaryKeyId = " + primaryKeyId);
        long id = primaryKeyId.longValue();
        System.out.println("id = " + id);
        ModelJob modelJob = null;
        try {
            modelJob = modelJobRepo.findModelJobById(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("modelJob = " + modelJob);
        modelJob.setStatus(ModelJobEnum.SPLITTING.getCode());
        modelJobRepo.save(modelJob);
    }

    /**
     * 训练任务创建  训练任务创建-->开始直接排队事件-->排队中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobDirectQueueEvent(Integer primaryKeyId) {
        System.out.println("直接排队中:"+primaryKeyId+ ModelJobEnum.QUEUING.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.QUEUING.getCode());
        modelJobRepo.save(modelJob);
    }
}
