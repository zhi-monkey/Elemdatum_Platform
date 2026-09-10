package org.dubhe.data.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum NotificationOperationTypeEnum {

    // 标准化训练相关
    TRAIN_SUCCESS("MSG_TRAIN_SUCCESS", "训练成功", "TrainSuccessFormatter"),
    TRAIN_FAILED("MSG_TRAIN_FAILED", "训练失败", "TrainFailedFormatter"),

    // 引导式训练相关
    GUIDED_TRAIN_SUCCESS("MSG_GUIDED_TRAIN_SUCCESS", "引导式训练成功", "GuidedTrainSuccessFormatter"),
    GUIDED_TRAIN_FAILED("MSG_GUIDED_TRAIN_FAILED", "引导式训练失败", "GuidedTrainFailedFormatter"),

    // 标准化转换相关
    CONVERT_SUCCESS("MSG_CONVERT_SUCCESS", "转换成功", "ConvertSuccessFormatter"),
    CONVERT_FAILED("MSG_CONVERT_FAILED", "转换失败", "ConvertFailedFormatter"),

    // 引导式转换相关
    GUIDED_CONVERT_SUCCESS("MSG_GUIDED_CONVERT_SUCCESS", "引导式转换成功", "GuidedConvertSuccessFormatter"),
    GUIDED_CONVERT_FAILED("MSG_GUIDED_CONVERT_FAILED", "引导式转换失败", "GuidedConvertFailedFormatter"),


    // 数据集相关
    DATASET_IMPORT_SUCCESS("MSG_DATASET_IMPORT_SUCCESS", "数据集导入成功", "DatasetImportSuccessFormatter"),
    DATASET_IMPORT_FAILED("MSG_DATASET_IMPORT_FAILED", "数据集导入失败", "DatasetImportFailedFormatter"),

    DATASET_ENHANCE_FINISH("MSG_DATASET_ENHANCE_FINISH", "数据集数据增强完成", "DatasetEnhanceFinishFormatter"),

    // 自动标注相关
    AUTO_LABEL_SUCCESS("MSG_AUTO_LABEL_SUCCESS", "自动标注成功", "AutoLabelSuccessFormatter"),
    AUTO_LABEL_FAILED("MSG_AUTO_LABEL_FAILED", "自动标注失败", "AutoLabelFailedFormatter"),

    // 标注相关
    ANNOTATION_TASK_COMPLETED("MSG_ANNOTATION_TASK_COMPLETED", "标注任务完成", "AnnotationTaskCompletedFormatter"),
    ANNOTATION_TASK_ASSIGNED("MSG_ANNOTATION_TASK_ASSIGNED", "标注任务分配", "AnnotationTaskAssignedFormatter"),


    // 用户相关
    USER_REGISTERED("MSG_USER_REGISTERED", "新用户注册", "UserRegisteredFormatter"),

    // 系统通知
    SYSTEM_ANNOUNCEMENT("MSG_SYSTEM_ANNOUNCEMENT", "系统公告", "SystemAnnouncementFormatter");

    /**
     * 操作类型代码
     */
    private final String code;

    /**
     * 操作类型描述
     */
    private final String description;

    /**
     * 对应的格式化器类名（可选，用于自动匹配）
     */
    private final String formatterClass;

    /**
     * 根据code获取枚举
     */
    public static NotificationOperationTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NotificationOperationTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断code是否有效
     */
    public static boolean isValid(String code) {
        return fromCode(code) != null;
    }
}
