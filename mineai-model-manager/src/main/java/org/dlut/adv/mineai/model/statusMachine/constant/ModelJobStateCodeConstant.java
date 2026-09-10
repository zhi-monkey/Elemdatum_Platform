package org.dlut.adv.mineai.model.statusMachine.constant;

/**
 * @description 训练任务状态码
 * @date
 */
public class ModelJobStateCodeConstant {

    private ModelJobStateCodeConstant() {
    }

    /**
     * 训练任务创建
     */
    public static final Integer TRAIN_CREATE = 0;
    /**
     * 数据切分中
     */
    public static final Integer SPLITTING = 12;
    /**
     * 排队中
     */
    public static final Integer QUEUING = 11;
    /**
     * 训练中
     */
    public static final Integer TRAINING = 1;
    /**
     * 暂停
     */
    public static final Integer PAUSED = -1;
    /**
     * 取消
     */
    public static final Integer CANCELED = -10;
    /**
     * 训练失败
     */
    public static final Integer TRAIN_FAILED = -2;
    /**
     * 训练成功
     */
    public static final Integer TRAIN_SUCCEEDED = 2;
    /**
     * 子任务转换中
     */
    public static final Integer SON_CONVERTING = 5;
    /**
     * 已发布
     */
    public static final Integer PUBLISHED = 7;
    /**
     * 转换任务创建
     */
    public static final Integer CREATE_CONVERT = 10;
    /**
     * 转换中
     */
    public static final Integer CONVERTING = 1;
    /**
     * 转换失败
     */
    public static final Integer CONVERT_FAILED = -5;
    /**
     * 转换成功
     */
    public static final Integer CONVERT_SUCCEEDED = 6;

}
