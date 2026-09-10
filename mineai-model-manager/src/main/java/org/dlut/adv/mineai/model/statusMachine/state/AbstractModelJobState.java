package org.dlut.adv.mineai.model.statusMachine.state;

import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.enums.ModelJobEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @description 训练任务状态类
 * @date 2020-08-27
 */
@Component
public abstract class AbstractModelJobState {
    @Autowired
    protected ModelJobRepo modelJobRepo;

    public AbstractModelJobState() {
    }

    /**
     * 创建任务成功   任务创建成功-->数据集切分-->数据切分中
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobSplitEvent(Integer primaryKeyId) {
    }
    /**
     * 创建任务成功   任务创建成功-->排队事件-->排队中
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobDirectQueueEvent(Integer primaryKeyId) {
    }
    /**
     * 数据切分中   数据切分中-->切分成功（省略）-->排队事件-->排队中
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobQueueEvent(Integer primaryKeyId) {
    }
    /**
     * 数据切分中   数据切分中-->切分失败事件-->切分失败（省略）-->训练失败
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobSplitFailEvent(Integer primaryKeyId) {
    }
    /**
     * 排队成功   排队中-->训练-->训练中
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobTrainEvent(Integer primaryKeyId) {
    }
    /**
     * xx中   xx中-->手动中止任务事件-->取消
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobTrainCanceledEvent(Integer primaryKeyId) {
        System.out.println("任务取消:"+primaryKeyId+ ModelJobEnum.CANCELED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.CANCELED.getCode());
        modelJobRepo.save(modelJob);
    }
    /**
     * xx中   xx中-->手动中止任务事件-->取消
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobSonTrainCanceledEvent(Integer primaryKeyId) {
        System.out.println("子任务取消:"+primaryKeyId+ ModelJobEnum.CANCELED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.CANCELED.getCode());
        modelJobRepo.save(modelJob);
    }
    /**
     * xx中   xx中-->训练成功结束-->训练成功
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobTrainSucceededEvent(Integer primaryKeyId) {
    }
    /**
     * xx中   xx中-->训练失败-->训练失败
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobTrainFailedEvent(Integer primaryKeyId) {
        System.out.println("训练失败:"+primaryKeyId+ ModelJobEnum.TRAIN_FAILED.getCode());
        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
        modelJob.setStatus(ModelJobEnum.TRAIN_FAILED.getCode());
        modelJobRepo.save(modelJob);
    }
    /**
     * 训练成功   训练成功-->发布-->已发布
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobTrainPublishedEvent(Integer primaryKeyId) {
    }
    /**
     * 训练成功   训练成功-->转换开始事件->子任务转换中
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobSonConvertEvent(Integer primaryKeyId) {
    }
    /**
     * 转换任务创建成功   转换任务创建成功-->转换开始事件->转换中
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobConvertEvent(Integer primaryKeyId) {
    }
    /**
     * 转换中   转换中-->转换成功-->转换成功
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobConvertSucceededEvent(Integer primaryKeyId) {
    }
    /**
     * 转换中   转换中-->转换失败-->转换失败
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobConvertFailedEvent(Integer primaryKeyId) {
    }

}
