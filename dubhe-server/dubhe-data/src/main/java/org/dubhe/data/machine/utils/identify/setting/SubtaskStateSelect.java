package org.dubhe.data.machine.utils.identify.setting;

import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.data.machine.constant.FileStateCodeConstant;
import org.dubhe.data.machine.enums.DataTeamSubtaskStateEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description 子任务状态判断类
 * @date 2020-09-24
 */
@Component
public class SubtaskStateSelect {

    // 未标注 所有文件均为未标注状态
    public DataTeamSubtaskStateEnum isInit(List<Integer> stateList) {
        if (stateList.size() == NumberConstant.NUMBER_1 && stateList.contains(FileStateCodeConstant.NOT_ANNOTATION_FILE_STATE)) {
            return DataTeamSubtaskStateEnum.NOT_ANNOTATION_SUBTASK_STATE;
        }
        return null;
    }

    // 标注中 既有标注完成又有未标注 或 只有已标注 （子任务已提交是手动提交的）
    public DataTeamSubtaskStateEnum isAnnotating(List<Integer> stateList) {
        if ((stateList.size() > 1 && stateList.contains(FileStateCodeConstant.NOT_ANNOTATION_FILE_STATE))||(stateList.size() == 1 && stateList.contains(FileStateCodeConstant.ANNOTATION_COMPLETE_FILE_STATE))) {
            return DataTeamSubtaskStateEnum.MANUAL_ANNOTATION_SUBTASK_STATE;
        }
        return null;
    }

    // 已完成 全是已标注
//    public DataTeamSubtaskStateEnum isFinished(List<Integer> stateList) {
//        if (stateList.size() == NumberConstant.NUMBER_1 && stateList.contains(FileStateCodeConstant.ANNOTATION_COMPLETE_FILE_STATE)){
//            return DataTeamSubtaskStateEnum.FINISH_ANNOTATION_SUBTASK_STATE;
//        }
//        return null;
//    }

    // 已完成->已提交 （手动确认）
    public DataTeamSubtaskStateEnum isFinished(List<Integer> stateList) {
        return null;
    }

    // 已终止 （待定）
    public DataTeamSubtaskStateEnum isTerminated(List<Integer> stateList) {
        return null;
    }

}