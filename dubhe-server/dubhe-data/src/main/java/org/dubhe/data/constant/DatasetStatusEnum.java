

package org.dubhe.data.constant;

import lombok.Getter;

/**
 * @description 数据集状态
 * @date 2020-04-10
 */
@Getter
public enum DatasetStatusEnum {

    /**
     * 文件全部未标注
     */
    INIT(0, " 未标注"),
    /**
     * 非自动标注中
     * 且存在标注中的文件
     */
    MANUAL_ANNOTATING(1, "手动标注中"),
    /**
     * 只由标注接口触发转移
     */
    AUTO_ANNOTATING(2, "自动标注中"),
    /**
     * 只由自动标注转移
     */
    AUTO_FINISHED(3, "自动标注完成"),
    /**
     * 文件全部标注完成
     */
    FINISHED(4, "已标注完成"),
    /**
     * 文件全部标注完成
     */
    NOT_SAMPLE(5, "未采样"),
    /**
     * 目标跟踪完成
     */
    FINISHED_TRACK(6, "目标跟踪完成"),
    /**
     * 文件采样中
     */
    SAMPLING(7, "采样中"),
    /**
     * 数据增强中
     */
    ENHANCING(8, "增强中"),
    /**
     * 采样失败
     */
    SAMPLE_FAILED(9, "采样失败");

    DatasetStatusEnum(int value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    private int value;
    private String msg;

}
