

package org.dubhe.data.constant;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description 多人标注任务状态
 * @date 2020-07-01
 */
@Getter
public enum DataTeamTaskStatusEnum {

    /**
     * 未标注
     */
    INIT(0, "未标注"),
    /**
     * 标注中
     */
    ANNOTATING(1, "标注中"),
    /**
     * 标注完成
     */
    FINISHED(2, "标注完成"),
    /**
     * 标注终止
     */
    TERMINATED(3, "标注终止");


    DataTeamTaskStatusEnum(int value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    private int value;
    private String msg;


}
