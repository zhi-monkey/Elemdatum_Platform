
package org.dubhe.data.machine.enums;

import org.dubhe.data.machine.constant.DataTeamTaskStateCodeConstant;

/**
 * @description 多人标注任务状态类
 * @date 2020-08-28
 */
public enum DataTeamTaskStateEnum {
    /**
     * 未标注
     */
    NOT_ANNOTATION_TASK_STATE(DataTeamTaskStateCodeConstant.NOT_ANNOTATION_TASK_STATE, "notAnnotationTaskState","未标注"),
    /**
     * 手动标注中
     */
    MANUAL_ANNOTATION_TASK_STATE(DataTeamTaskStateCodeConstant.MANUAL_ANNOTATION_TASK_STATE, "manualAnnotationTaskState","标注中"),
    /**
     * 标注终止
     */
    TERMINATE_ANNOTATION_TASK_STATE(DataTeamTaskStateCodeConstant.TERMINATE_ANNOTATION_TASK_STATE, "terminateAnnotationTaskState","已终止"),
    /**
     * 标注完成
     */
    FINISH_ANNOTATION_TASK_STATE(DataTeamTaskStateCodeConstant.FINISH_ANNOTATION_TASK_STATE, "finishAnnotationTaskState","已完成");

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

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStateMachine() {
        return stateMachine;
    }

    public void setStateMachine(String stateMachine) {
        this.stateMachine = stateMachine;
    }

    DataTeamTaskStateEnum(Integer code, String stateMachine , String description) {
        this.code = code;
        this.stateMachine = stateMachine;
        this.description = description;
    }

    /**
     * 根据CODE 获取 DESCRIPTION
     *
     * @param code 多人标注任务状态编码
     * @return String
     */
    public static String getStateMachine(Integer code) {
        if (code != null) {
            for (DataTeamTaskStateEnum dataTaskStateEnum : DataTeamTaskStateEnum.values()) {
                if (dataTaskStateEnum.getCode().equals(code)) {
                    return dataTaskStateEnum.getStateMachine();
                }
            }
        }
        return null;
    }

    /**
     * 根据CODE 获取 DataTaskStateEnum
     *
     * @param code 多人标注任务状态编码
     * @return String
     */
    public static DataTeamTaskStateEnum getState(Integer code) {
        if (code != null) {
            for (DataTeamTaskStateEnum dataTaskStateEnum : DataTeamTaskStateEnum.values()) {
                if (dataTaskStateEnum.getCode().equals(code)) {
                    return dataTaskStateEnum;
                }
            }
        }
        return null;
    }

}
