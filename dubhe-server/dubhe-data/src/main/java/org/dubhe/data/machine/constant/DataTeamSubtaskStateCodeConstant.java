
package org.dubhe.data.machine.constant;

/**
 * @description 多人标注子任务状态码
 * @date 2020-09-03
 */
public class DataTeamSubtaskStateCodeConstant {

    private DataTeamSubtaskStateCodeConstant() {
    }

    /**
     * 未标注
     */
    public static final Integer NOT_ANNOTATION_SUBTASK_STATE = 0;
    /**
     * 标注中
     */
    public static final Integer MANUAL_ANNOTATION_SUBTASK_STATE = 1;
    /**
     * 已提交
     */
    public static final Integer FINISH_ANNOTATION_SUBTASK_STATE = 2;
    /**
     * 已终止
     */
    public static final Integer TERMINATE_ANNOTATION_SUBTASK_STATE = 3;


}
