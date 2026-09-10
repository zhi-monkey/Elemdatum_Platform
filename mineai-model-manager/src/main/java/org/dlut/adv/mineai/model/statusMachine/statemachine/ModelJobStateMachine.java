package org.dlut.adv.mineai.model.statusMachine.statemachine;

import lombok.Data;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.enums.ModelJobEnum;
import org.dlut.adv.mineai.model.statusMachine.state.specific.*;
import org.dlut.adv.mineai.model.statusMachine.state.AbstractModelJobState;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @description 训练任务状态机，实现所有激发事件，负责状态切换，参数传递（建议去掉AbstractModelJobState）
 * @date
 */
@Data
@Component
public class ModelJobStateMachine extends AbstractModelJobState implements Serializable {
    private StateMachineFactory stateMachineFactory;
//    /**
//     * 创建任务成功
//     */
////    @Autowired
//    private CreateSuccessState createSuccessState;
//    /**
//     * 数据切分中类
//     */
////    @Autowired
//    private SplittingState splittingState;
//    /**
//     * 排队中类
//     */
////    @Autowired
//    private QueueingState queueingState;
//    /**
//     * 训练中类
//     */
////    @Autowired
//    private TrainingState trainingState;
//    /**
//     * 训练成功类
//     */
////    @Autowired
//    private TrainSucceededState trainSucceededState;
//    /**
//     * 子任务转换中类
//     */
////    @Autowired
//    private SonConvertingstate sonConvertingstate;
//    /**
//     * 创建转换任务类
//     */
////    @Autowired
//    private CreateConvertState createConvertState;
//    /**
//     * 转换中类
//     */
////    @Autowired
//    private ConvertingState convertingState;

    /**
     * 内存中的状态机
     */
    private AbstractModelJobState memoryModelJobState;

//    @Autowired
    private ModelJobRepo modelJobRepo;

//    /**
//     * 初始化训练任务状态机的状态
//     *
//     * @param primaryKeyId 业务ID
//     * @return ModelJob 训练任务详情
//     */
//    public ModelJob initMemoryDataState(Integer primaryKeyId) {
//        if (primaryKeyId == null) {
//            throw new RuntimeException("未找到业务ID");
//        }
//        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
//        if (modelJob == null || modelJob.getStatus() == null) {
//            throw new RuntimeException("未找到业务数据");
//        }
//        memoryModelJobState = SpringContextHolder.getBean(ModelJobEnum.getStateMachine(0));
//        return modelJob;
//    }

//    /**
//     * 初始化转换任务的状态
//     *
//     * @param primaryKeyId 业务ID
//     * @return ModelJob 训练任务详情
//     */
//    public ModelJob initConvertMemoryDataState(Integer primaryKeyId) {
//        if (primaryKeyId == null) {
//            throw new RuntimeException("未找到业务ID");
//        }
//        ModelJob modelJob = modelJobRepo.findModelJobById(primaryKeyId);
//        if (modelJob == null || modelJob.getStatus() == null) {
//            throw new RuntimeException("未找到业务数据");
//        }
//        memoryModelJobState = SpringContextHolder.getBean(ModelJobEnum.getStateMachine(10));
//        return modelJob;
//    }

    /**
     * 训练任务创建  训练任务创建-->开始切分事件-->数据切分中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobSplitEvent(Integer primaryKeyId) {
//        initMemoryDataState(primaryKeyId);
//        if (!"CreateSuccessState".equals(memoryModelJobState.getClass().getSimpleName())) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("trainCreateState");
        memoryModelJobState.modelJobSplitEvent(primaryKeyId);
        memoryModelJobState = SpringContextHolder.getBean("splittingState");
    }

    /**
     * 训练任务创建  训练任务创建-->开始直接排队事件-->排队中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobDirectQueueEvent(Integer primaryKeyId) {
//        if (!memoryModelJobState.getClass().getSimpleName().equals("CreateSuccessState")) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("trainCreateState");
        memoryModelJobState.modelJobDirectQueueEvent(primaryKeyId);
        memoryModelJobState = SpringContextHolder.getBean("queueingState");
    }

    /**
     * 数据切分中   数据切分中-->切分成功（省略）-->开始排队事件-->排队中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobQueueEvent(Integer primaryKeyId) {
//        initMemoryDataState(primaryKeyId);
//        if (!memoryModelJobState.getClass().getSimpleName().equals("SplittingState")) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("splittingState");
        memoryModelJobState.modelJobQueueEvent(primaryKeyId);
        memoryModelJobState = SpringContextHolder.getBean("queueingState");
    }

    /**
     * 数据切分中   数据切分中-->切分失败事件-->切分失败（省略）-->训练失败
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobSplitFailEvent(Integer primaryKeyId) {
//        initMemoryDataState(primaryKeyId);
        if (!memoryModelJobState.getClass().getSimpleName().equals("SplittingState")) {
            throw new RuntimeException("状态机异常");
        }
        memoryModelJobState.modelJobSplitFailEvent(primaryKeyId);
    }

    /**
     * 排队中   排队中-->排队成功（省略）-->开始训练事件-->训练中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobTrainEvent(Integer primaryKeyId) {
//        if (!memoryModelJobState.getClass().getSimpleName().equals("QueueingState")) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("queueingState");
        memoryModelJobState.modelJobTrainEvent(primaryKeyId);
        memoryModelJobState = SpringContextHolder.getBean("trainingState");
    }

//    /**
//     * xx中   xx中-->手动中止任务事件-->取消
//     *
//     * @param primaryKeyId 业务ID
//     */
//    @Override
//    public void modelJobTrainCanceledEvent(Integer primaryKeyId) {
////        if (memoryModelJobState != trainingState) {
////            throw new RuntimeException("状态机异常");
////        }
//        memoryModelJobState.modelJobTrainCanceledEvent(primaryKeyId);
//    }
    /**
     * xx中   xx中-->手动中止任务事件-->取消
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobSonTrainCanceledEvent(Integer primaryKeyId) {
        memoryModelJobState = SpringContextHolder.getBean("sonConvertingstate");
        memoryModelJobState.modelJobSonTrainCanceledEvent(primaryKeyId);
    }
    /**
     * 训练中   训练中-->训练成功结束事件-->训练成功
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobTrainSucceededEvent(Integer primaryKeyId) {
//        if (!memoryModelJobState.getClass().getSimpleName().equals("TrainingState")) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("trainingState");
        memoryModelJobState.modelJobTrainSucceededEvent(primaryKeyId);
        memoryModelJobState = SpringContextHolder.getBean("trainSucceededState");
    }

//    /**
//     * xx中   xx中-->训练失败-->训练失败
//     *
//     * @param primaryKeyId 业务ID
//     */
//    @Override
//    public void modelJobTrainFailedEvent(Integer primaryKeyId) {
////        if (memoryModelJobState != trainingState) {
////            throw new RuntimeException("状态机异常");
////        }
//        memoryModelJobState.modelJobTrainFailedEvent(primaryKeyId);
//    }

    /**
     * 训练成功   训练成功-->开始子任务转换事件->子任务转换中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobSonConvertEvent(Integer primaryKeyId) {
//        if (!memoryModelJobState.getClass().getSimpleName().equals("TrainSucceededState")) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("trainSucceededState");
        memoryModelJobState.modelJobSonConvertEvent(primaryKeyId);
        memoryModelJobState = SpringContextHolder.getBean("sonConvertingstate");
    }

    /**
     * 训练成功   训练成功-->开始发布事件-->已发布
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobTrainPublishedEvent(Integer primaryKeyId) {
//        if (!memoryModelJobState.getClass().getSimpleName().equals("TrainSucceededState")) {
//            throw new RuntimeException("状态机异常");
//        }
        // 任务状态机可能丢失
        memoryModelJobState = SpringContextHolder.getBean("trainSucceededState");
        memoryModelJobState.modelJobTrainPublishedEvent(primaryKeyId);
    }

    /**
     * 创建转换任务   创建转换任务-->转换开始事件->转换中
     *
     * @param primaryKeyId 业务ID
     */
    @Override
    public void modelJobConvertEvent(Integer primaryKeyId) {
//        if (!memoryModelJobState.getClass().getSimpleName().equals("CreateConvertState")) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("createConvertState");
        memoryModelJobState.modelJobConvertEvent(primaryKeyId);
        memoryModelJobState = SpringContextHolder.getBean("convertingState");
    }

    /**
     * 转换中   转换中-->转换成结束事件功-->转换成功
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobConvertSucceededEvent(Integer primaryKeyId) {
//        if (!memoryModelJobState.getClass().getSimpleName().equals("ConvertingState")) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("convertingState");
        memoryModelJobState.modelJobConvertSucceededEvent(primaryKeyId);
    }

    /**
     * 转换中   转换中-->转换失败结束事件-->转换失败
     *
     * @param primaryKeyId 业务ID
     */
    public void modelJobConvertFailedEvent(Integer primaryKeyId) {
//        if (!memoryModelJobState.getClass().getSimpleName().equals("ConvertingState")) {
//            throw new RuntimeException("状态机异常");
//        }
        memoryModelJobState = SpringContextHolder.getBean("convertingState");
        memoryModelJobState.modelJobConvertFailedEvent(primaryKeyId);
    }

}
