package org.dlut.adv.mineai.model.statusMachine.enums;

import lombok.Data;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;

/**
 * @description 训练任务状态类
 * @date
 */
public enum ModelJobEnum {
    /**
     * 任务创建成功
     */
    CREATE_SUCCESS(ModelJobStateCodeConstant.TRAIN_CREATE, "createSuccessState","任务创建成功"),
    /**
     * 数据切分中
     */
    SPLITTING(ModelJobStateCodeConstant.SPLITTING, "splittingState","数据切分中"),
    /**
     * 排队中
     */
    QUEUING(ModelJobStateCodeConstant.QUEUING, "queueingState","排队中"),
    /**
     * 训练中
     */
    TRAINING(ModelJobStateCodeConstant.TRAINING, "queueSuccessState","训练中"),
    /**
     * 训练失败
     */
    TRAIN_FAILED(ModelJobStateCodeConstant.TRAIN_FAILED, "createSuccessState","训练失败"),
    /**
     * 训练成功
     */
    TRAIN_SUCCEEDED(ModelJobStateCodeConstant.TRAIN_SUCCEEDED, "createSuccessState","训练成功"),
    /**
     * 训练取消
     */
    CANCELED(ModelJobStateCodeConstant.CANCELED, "createSuccessState","训练取消"),
    /**
     * 已发布
     */
    PUBLISHED(ModelJobStateCodeConstant.PUBLISHED, "createSuccessState","已发布"),
    /**
     * 转换任务创建成功
     */
    CREATE_CONVERT_SUCCESS(ModelJobStateCodeConstant.CREATE_CONVERT, "createConvertState","转换任务创建成功"),
    /**
     * 子任务转换中/转换中
     */
    CONVERTING(ModelJobStateCodeConstant.CONVERTING, "createConvertState","转换中"),
    /**
     * 子任务转换中/转换中
     */
    SON_CONVERTING(ModelJobStateCodeConstant.SON_CONVERTING, "createConvertState","子任务转换中"),
    /**
     * 转换失败
     */
    CONVERT_FAILED(ModelJobStateCodeConstant.CONVERT_FAILED, "createConvertState","转换失败"),
    /**
     * 转换成功
     */
    CONVERT_SUCCEEDED(ModelJobStateCodeConstant.CONVERT_SUCCEEDED, "createConvertState","转换成功");

    /**
     * 编码
     */
    private Integer code;
    /**
     * 状态机
     */
    private String stateMachine;
    /**
     * 描述
     */
    private String description;

    ModelJobEnum(Integer code, String stateMachine , String description) {
        this.code = code;
        this.stateMachine = stateMachine;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(String stateMachine) {
        this.stateMachine = stateMachine;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 根据CODE 获取 DESCRIPTION
     *
     * @param code 训练任务状态编码
     * @return String
     */
    public static String getStateMachine(Integer code) {
        if (code != null) {
            for (ModelJobEnum modelJobEnum : ModelJobEnum.values()) {
                if (modelJobEnum.getCode().equals(code)) {
                    return modelJobEnum.getStateMachine();
                }
            }
        }
        return null;
    }

    /**
     * 根据CODE 获取 modelJobEnum
     *
     * @param code 训练任务状态编码
     * @return String
     */
    public static ModelJobEnum getState(Integer code) {
        if (code != null) {
            for (ModelJobEnum modelJobEnum : ModelJobEnum.values()) {
                if (modelJobEnum.getCode().equals(code)) {
                    return modelJobEnum;
                }
            }
        }
        return null;
    }

}
