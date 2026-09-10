
package org.dubhe.data.machine.constant;

/**
 * @description 状态机异常量类
 * @date 2020-09-01
 */
public class ErrorMessageConstant {

    private ErrorMessageConstant(){}

    /**
     * 数据状态异常信息
     */
    public static final String FILE_CHANGE_ERR_MESSAGE = "当前文件状态不可变更";

    public static final String DATASET_CHANGE_ERR_MESSAGE = "当前数据集状态不可变更";

    public static final String SUBTASK_CHANGE_ERR_MESSAGE = "当前子任务状态不可变更";
    public static final String TASK_CHANGE_ERR_MESSAGE = "当前任务状态不可变更";

}