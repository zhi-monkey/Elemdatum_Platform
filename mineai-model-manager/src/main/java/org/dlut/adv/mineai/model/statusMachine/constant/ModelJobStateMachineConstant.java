package org.dlut.adv.mineai.model.statusMachine.constant;

/**
 * @description 状态机事件常量
 * @date
 */
public class ModelJobStateMachineConstant {

    private ModelJobStateMachineConstant() {
    }


    // 所有训练任务的状态描述
    /**
     * 训练任务状态
     */
    public static final String MODELJOB_STATE_MACHINE = "modelJobStateMachine";




    /**
     * 训练任务创建  训练任务创建-->开始切分事件-->数据切分中
     */
    public static final String MODELJOB_SETSPLIT_EVENT = "modelJobSplitEvent";

    /**
     * 训练任务创建  训练任务创建-->开始直接排队事件-->排队中
     */
    public static final String MODELJOB_DIRECT_QUEUE_EVENT = "modelJobDirectQueueEvent";

    /**
     * 数据切分中   数据切分中-->切分成功（省略）-->开始排队事件-->排队中
     */
    public static final String MODELJOB_QUEUE_EVENT = "modelJobQueueEvent";

    /**
     * 排队中   排队中-->排队成功（省略）-->开始训练事件-->训练中
     */
    public static final String MODELJOB_TRAIN_EVENT = "modelJobTrainEvent";

    /**
     * 训练中   训练中-->训练成功结束事件-->训练成功
     */
    public static final String MODELJOB_TRAIN_SUCCEEDED_EVENT = "modelJobTrainSucceededEvent";

    /**
     * 训练成功   训练成功-->开始子任务转换事件->子任务转换中
     */
    public static final String MODELJOB_SON_CONVERT_EVENT = "modelJobSonConvertEvent";

    /**
     * xx中   xx中-->手动中止任务事件-->取消
     */
    public static final String MODELJOB_TRAIN_CANCELED_EVENT = "modelJobTrainCanceledEvent";

    /**
     * xx中   xx中-->训练失败结束事件-->训练失败
     */
    public static final String MODELJOB_TRAIN_FAILED_EVENT = "modelJobTrainFailedEvent";

    /**
     * 训练成功   训练成功-->开始发布事件-->已发布
     */
    public static final String MODELJOB_TRAIN_PUBLISHED_EVENT = "modelJobTrainPublishedEvent";



    /**
     * 创建转换任务   创建转换任务-->转换开始事件->转换中
     */
    public static final String MODELJOB_CONVERT_EVENT = "modelJobConvertEvent";
    /**
     * 转换中   转换中-->转换成结束事件功-->转换成功
     */
    public static final String MODELJOB_CONVERT_SUCCEEDED_EVENT = "modelJobConvertSucceededEvent";

    /**
     * 转换中   转换中-->转换失败结束事件-->转换失败
     */
    public static final String MODELJOB_CONVERT_FAILED_EVENT = "modelJobConvertFailedEvent";

}
