
package org.dubhe.data.machine.enums;

import org.dubhe.data.machine.constant.DataTeamSubtaskStateCodeConstant;

/**
 * @description 多人标注子任务状态类
 * @date 2020-08-28
 */
public enum DataTeamSubtaskStateEnum {
    /**
     * 未标注
     */
    NOT_ANNOTATION_SUBTASK_STATE(DataTeamSubtaskStateCodeConstant.NOT_ANNOTATION_SUBTASK_STATE, "notAnnotationSubtaskState","未标注"),
    /**
     * 手动标注中
     */
    MANUAL_ANNOTATION_SUBTASK_STATE(DataTeamSubtaskStateCodeConstant.MANUAL_ANNOTATION_SUBTASK_STATE, "manualAnnotationSubtaskState","标注中"),
    /**
     * 标注终止
     */
    TERMINATE_ANNOTATION_SUBTASK_STATE(DataTeamSubtaskStateCodeConstant.TERMINATE_ANNOTATION_SUBTASK_STATE, "annotationTerminatedSubtaskState","已终止"),
    /**
     * 标注完成
     */
    FINISH_ANNOTATION_SUBTASK_STATE(DataTeamSubtaskStateCodeConstant.FINISH_ANNOTATION_SUBTASK_STATE, "annotationCompleteSubtaskState","已提交");

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

    DataTeamSubtaskStateEnum(Integer code, String stateMachine , String description) {
        this.code = code;
        this.stateMachine = stateMachine;
        this.description = description;
    }

    /**
     * 根据CODE 获取 DESCRIPTION
     *
     * @param code 多人标注子任务状态编码
     * @return String
     */
    public static String getStateMachine(Integer code) {
        if (code != null) {
            for (DataTeamSubtaskStateEnum dataSubtaskStateEnum : DataTeamSubtaskStateEnum.values()) {
                if (dataSubtaskStateEnum.getCode().equals(code)) {
                    return dataSubtaskStateEnum.getStateMachine();
                }
            }
        }
        return null;
    }

    /**
     * 根据CODE 获取 DataSubtaskStateEnum
     *
     * @param code 多人标注任务状态编码
     * @return String
     */
    public static DataTeamSubtaskStateEnum getState(Integer code) {
        if (code != null) {
            for (DataTeamSubtaskStateEnum dataSubtaskStateEnum : DataTeamSubtaskStateEnum.values()) {
                if (dataSubtaskStateEnum.getCode().equals(code)) {
                    return dataSubtaskStateEnum;
                }
            }
        }
        return null;
    }

}
