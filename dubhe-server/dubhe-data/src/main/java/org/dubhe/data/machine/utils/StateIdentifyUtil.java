

package org.dubhe.data.machine.utils;

import org.dubhe.data.constant.DatasetModuleEnum;
import org.dubhe.data.domain.entity.DataTeam;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.machine.enums.DataTeamSubtaskStateEnum;
import org.dubhe.data.machine.enums.DataTeamTaskStateEnum;
import org.dubhe.data.machine.utils.identify.data.DataHub;
import org.dubhe.data.machine.utils.identify.data.DataTeamHub;
import org.dubhe.data.machine.utils.identify.setting.StateIdentifySetting;
import org.dubhe.data.machine.utils.identify.setting.StateSelect;
import org.dubhe.data.machine.utils.identify.setting.SubtaskStateSelect;
import org.dubhe.data.machine.utils.identify.setting.TaskStateSelect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * @description 状态判断实现类
 * @date 2020-09-24
 */
@Component
public class StateIdentifyUtil{


    /**
     * 数据查询处理
     */
    @Autowired
    private DataHub dataHub;

    @Autowired
    private DataTeamHub dataTeamHub;

    /**
     * 状态判断类
     */
    @Autowired
    private StateSelect stateSelect;

    @Autowired
    private SubtaskStateSelect subtaskStateSelect;

    @Autowired
    private TaskStateSelect taskStateSelect;

    /**
     * 状态判断中所有的自定义方法数组
     */
    private final Method[] method = ReflectionUtils.getDeclaredMethods(StateSelect.class);
    private final Method[] subtaskMethod = ReflectionUtils.getDeclaredMethods(SubtaskStateSelect.class);
    private final Method[] taskMethod = ReflectionUtils.getDeclaredMethods(TaskStateSelect.class);

    /**
     * 获取数据集状态(指定版本)
     *
     * @param datasetId               数据集id
     * @param versionName             数据集版本名称
     * @param needFileStateDoIdentify 是否需要查询文件状态判断
     * @return DatasetStatusEnum      数据集状态(指定版本)
     */
    public DataStateEnum getStatus(Long datasetId, String versionName, boolean needFileStateDoIdentify) {
        return needFileStateDoIdentify ? new IdentifyDatasetStateByFileState(datasetId, versionName, StateIdentifySetting.NEED_FILE_STATE_DO_IDENTIFY)
                .getStatus() : dataHub.getDatasetStatus(datasetId);
    }

    /**
     * 获取数据集状态(未指定版本)
     *
     * @param dataset                 数据集
     * @param needFileStateDoIdentify 是否需要查询文件状态判断
     * @return DatasetStatusEnum      数据集状态(指定版本)
     */
    public DataStateEnum getStatus(Dataset dataset, boolean needFileStateDoIdentify) {
        return needFileStateDoIdentify ? new IdentifyDatasetStateByFileState(dataset.getId(), dataset.getCurrentVersionName(), StateIdentifySetting.NEED_FILE_STATE_DO_IDENTIFY)
                .getStatus() : dataHub.getDatasetStatus(dataset.getId());
    }

    /**
     * 获取数据集状态(自动标注/目标跟踪回滚使用)
     *
     * @param datasetId   数据集id
     * @param versionName 数据集版本名称
     * @return DatasetStatusEnum    数据集状态(指定版本)
     */
    public DataStateEnum getStatusForRollback(Long datasetId, String versionName) {
        return new IdentifyDatasetStateByFileState(datasetId, versionName, StateIdentifySetting.ROLL_BACK_FOR_STATE).getStatus();
    }

    // 获取子任务状态
    public DataTeamSubtaskStateEnum getSubtaskStatus(Long subtaskId,boolean needFileStateDoIdentify){
        return needFileStateDoIdentify ? new IdetifySubtaskStateByFileState(subtaskId,  StateIdentifySetting.SUBTASK_NEED_FILE_STATE_DO_IDENTIFY)
                .getStatus() : dataTeamHub.getDataTeamSubtaskStatus(subtaskId);
    }

    // 获取任务状态
    public DataTeamTaskStateEnum getTaskStatus(Long taskId,boolean needSubtaskStateDoIdentify){
        return needSubtaskStateDoIdentify ? new IdetifyTaskStateBySubtaskState(taskId,  StateIdentifySetting.TASK_NEED_FILE_STATE_DO_IDENTIFY)
                .getStatus() : dataTeamHub.getDataTeamTaskStatus(taskId);
    }


    class IdentifyDatasetStateByFileState {

        /**
         * 判断得到的数据集状态
         */
        public DataStateEnum state;

        /**
         * 会查询文件的状态去对数据集的状态做判断
         *
         * @param datasetId   数据集ID
         * @param versionName 数据集版本名称
         */
        public IdentifyDatasetStateByFileState(Long datasetId, String versionName, Set<DataStateEnum> dataStateEnums) {
            state = dataHub.getDatasetStatus(datasetId);
            if (dataStateEnums.contains(state)) {
                List<Integer> stateList = dataHub.getFileStatusListByDatasetAndVersion(datasetId, versionName);
                if (stateList == null || stateList.isEmpty()) {
                    state = DataStateEnum.NOT_ANNOTATION_STATE;
                    return;
                }
                for (Method stateSelectMethod : method) {
                    state = (DataStateEnum) ReflectionUtils.invokeMethod(stateSelectMethod, stateSelect, new Object[]{stateList});
                    if (state != null) {
                        return;
                    }
                }
            }
        }

        DataStateEnum getStatus() {
            return this.state;
        }
    }

    class IdetifySubtaskStateByFileState {

        // 判断子任务应处于的状态
        public DataTeamSubtaskStateEnum state;

        // 根据文件的状态判断子任务状态
        public IdetifySubtaskStateByFileState(Long subtaskId,Set<DataTeamSubtaskStateEnum> dataTeamSubtaskStateEnum){
            state = dataTeamHub.getDataTeamSubtaskStatus(subtaskId);
            if (dataTeamSubtaskStateEnum.contains(state)){
                // 获取当前子任务的文件状态列表
                List<Integer> stateList = dataTeamHub.getFileStatusListBySubtask(subtaskId);
                if (stateList == null || stateList.isEmpty()) {
                    state = DataTeamSubtaskStateEnum.NOT_ANNOTATION_SUBTASK_STATE;
                    return;
                }
                // 根据文件状态判断当前子任务状态
                for (Method method : subtaskMethod) {
                    state = (DataTeamSubtaskStateEnum) ReflectionUtils.invokeMethod(method, subtaskStateSelect, new Object[]{stateList});
                    if (state != null) {
                        return;
                    }
                }
            }
        }

        DataTeamSubtaskStateEnum getStatus() {
            return this.state;
        }

    }

    class IdetifyTaskStateBySubtaskState {

        // 判断任务应处于的状态
        public DataTeamTaskStateEnum state;

        // 根据子任务的状态判断任务状态
        public IdetifyTaskStateBySubtaskState(Long taskId,Set<DataTeamTaskStateEnum> dataTeamTaskStateEnums){
            state = dataTeamHub.getDataTeamTaskStatus(taskId);
            if (dataTeamTaskStateEnums.contains(state)){
                // 获取当前任务的子任务状态列表
                List<Integer> stateList = dataTeamHub.getSubtaskStatusListByTask(taskId);
                if (stateList == null || stateList.isEmpty()) {
                    state = DataTeamTaskStateEnum.NOT_ANNOTATION_TASK_STATE;
                    return;
                }
                // 根据子任务状态判断当前任务状态
                for (Method method : taskMethod) {
                    state = (DataTeamTaskStateEnum) ReflectionUtils.invokeMethod(method, taskStateSelect, new Object[]{stateList});
                    if (state != null) {
                        if (state == DataTeamTaskStateEnum.FINISH_ANNOTATION_TASK_STATE) {
                            // 如果任务应处于已完成状态，那么修改数据集的多人标注状态
                            dataTeamHub.updateModuleByTaskId(taskId, DatasetModuleEnum.NO_TEAM.getStatus());
                        }
                        return;
                    }
                }
            }
        }

        DataTeamTaskStateEnum getStatus() {
            return this.state;
        }

    }
}