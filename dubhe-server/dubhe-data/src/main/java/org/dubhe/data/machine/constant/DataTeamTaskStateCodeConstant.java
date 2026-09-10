
package org.dubhe.data.machine.constant;

/**
 * @description 多人标注任务状态码
 * @date 2020-09-03
 */
public class DataTeamTaskStateCodeConstant {

    private DataTeamTaskStateCodeConstant() {
    }

    /**
     * 未标注
     */
    public static final Integer NOT_ANNOTATION_TASK_STATE = 0;
    /**
     * 标注中
     */
    public static final Integer MANUAL_ANNOTATION_TASK_STATE = 1;
    /**
     * 已完成
     */
    public static final Integer FINISH_ANNOTATION_TASK_STATE = 2;
    /**
     * 已终止
     */
    public static final Integer TERMINATE_ANNOTATION_TASK_STATE = 3;


}
