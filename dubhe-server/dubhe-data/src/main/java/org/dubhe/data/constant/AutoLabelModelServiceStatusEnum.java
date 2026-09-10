

package org.dubhe.data.constant;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum AutoLabelModelServiceStatusEnum {


    STARTING(101, "启动中"),

    RUNNING(102, "运行中"),

    START_FAILED(103, "启动失败"),

    STOPING(104, "停止中"),

    STOPED(105, "已停止")
    ;

    AutoLabelModelServiceStatusEnum(int value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    private int value;
    private String msg;

    /**
     * 检查当前模型服务状态是否可用
     * @param value 当前模型服务状态
     * @return
     */
    public static boolean checkAvailable(int value) {
        if (Arrays.asList(AutoLabelModelServiceStatusEnum.RUNNING.value).contains(value)) {
            return true;
        }
        return false;
    }

}
