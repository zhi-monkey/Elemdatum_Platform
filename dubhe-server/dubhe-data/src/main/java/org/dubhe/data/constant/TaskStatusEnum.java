

package org.dubhe.data.constant;

import lombok.Getter;

/**
 * @description 任务状态
 * @date 2020-04-10
 */
@Getter
public enum TaskStatusEnum {

    /**
     * 未处理
     */
    INIT(0, "未处理"),
    /**
     * 进行中
     */
    ING(1, "进行中"),
    /**
     * 已完成
     */
    FINISHED(2, "已完成"),
    /**
     * 失败
     */
    FAIL(3, "失败"),
    ;

    TaskStatusEnum(int value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    private int value;
    private String msg;

}
