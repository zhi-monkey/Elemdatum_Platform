package org.dubhe.data.machine.utils.identify.setting;

import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.machine.enums.DataTeamSubtaskStateEnum;
import org.dubhe.data.machine.enums.DataTeamTaskStateEnum;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * @description 状态判断设置类
 * @date 2020-09-24
 */
@Component
public class StateIdentifySetting {

    /**
     * 回退状态（自动标注失败/目标跟踪失败）使用
     */
    public static final Set<DataStateEnum> ROLL_BACK_FOR_STATE = new HashSet<DataStateEnum>() {{
        //自动标注中
        add(DataStateEnum.AUTOMATIC_LABELING_STATE);
        //目标跟踪中
        add(DataStateEnum.TARGET_FOLLOW_STATE);
        //增强中
        add(DataStateEnum.STRENGTHENING_STATE);
        //采样中
        add(DataStateEnum.SAMPLING_STATE);
        //导入中
        add(DataStateEnum.IN_THE_IMPORT_STATE);
    }};


    /**
     * 数据集状态需要使用文件状态去判断的
     */
    public static final Set<DataStateEnum> NEED_FILE_STATE_DO_IDENTIFY = new HashSet<DataStateEnum>() {{
        //未标注
        add(DataStateEnum.NOT_ANNOTATION_STATE);
        //手动标注中
        add(DataStateEnum.MANUAL_ANNOTATION_STATE);
        //自动标注完成
        add(DataStateEnum.AUTO_TAG_COMPLETE_STATE);
        //自动标注中
        add(DataStateEnum.AUTOMATIC_LABELING_STATE);
        //标注完成
        add(DataStateEnum.ANNOTATION_COMPLETE_STATE);
        //目标跟踪完成
        add(DataStateEnum.TARGET_COMPLETE_STATE);
        //导入中
        add(DataStateEnum.IN_THE_IMPORT_STATE);
        //采样中
        add(DataStateEnum.SAMPLING_STATE);
        //压缩包导入中
        add(DataStateEnum.ZIP_IMPORT_STATE);
    }};

    // 子任务状态需要用文件状态去判断的
    public static final Set<DataTeamSubtaskStateEnum> SUBTASK_NEED_FILE_STATE_DO_IDENTIFY = new HashSet<DataTeamSubtaskStateEnum>() {
        {
            // 未标注
            add(DataTeamSubtaskStateEnum.NOT_ANNOTATION_SUBTASK_STATE);
            // 标注中
            add(DataTeamSubtaskStateEnum.MANUAL_ANNOTATION_SUBTASK_STATE);
        }
    };

    // 任务状态需要用子任务状态去判断的
    public static final Set<DataTeamTaskStateEnum> TASK_NEED_FILE_STATE_DO_IDENTIFY = new HashSet<DataTeamTaskStateEnum>() {
        {
            // 未标注
            add(DataTeamTaskStateEnum.NOT_ANNOTATION_TASK_STATE);
            // 标注中
            add(DataTeamTaskStateEnum.MANUAL_ANNOTATION_TASK_STATE);
            // 已完成
            add(DataTeamTaskStateEnum.FINISH_ANNOTATION_TASK_STATE);
        }
    };

}
