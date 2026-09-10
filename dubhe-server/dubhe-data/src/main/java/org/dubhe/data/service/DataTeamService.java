package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.DataTeam;
import org.dubhe.data.domain.vo.DataTeamTaskSubtaskAnnotationStatusVO;
import org.dubhe.data.domain.vo.DataTeamTaskSubtaskInfoVO;
import org.dubhe.data.domain.vo.DataTeamVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.service
 * @Project：mineai
 * @name：DataTeamService
 * @Date：2024/3/11 16:07
 * @Filename：DataTeamService
 * @Desc：
 */
public interface DataTeamService extends IService<DataTeam> {

    // 分页查询团队列表
    Object queryAll(DataTeamQueryDTO criteria, Page page);

    // 获取该用户所有创建的团队
    List<DataTeamVO> queryAllTeams();

    // 新增团队
    DataTeamVO create(DataTeamCreateDTO resources);

    // 修改团队
    DataTeamVO update(DataTeamUpdateDTO resources);

    // 删除团队
    void delete(Set<Long> ids);

    // 分页查询任务列表
    Object queryAllTasks(Page page, DataTeamTaskQueryDTO criteria);

    // 创建任务
    Object createTask(DataTeamTaskCreateDTO dataTeamTaskCreateDTO);

    // 删除任务
    void deleteTask(Set<Long> ids);

    @Transactional(rollbackFor = Exception.class)
    void deleteSubTask(Long subTaskId);

    // 重新分配子任务比例
    Object adjustTaskProportions(Long taskId, Map<Long, Integer> userAllocations);

    // 查询子任务
    Object queryAllSubtasks(Page page,DataTeamSubtaskQueryDTO criteria);

    // 查询任务状态
    Integer queryTaskStatus(Long id);

    // 查询任务状态
    Integer querySubtaskStatus(Long id);

    // 查询子任务对应图片标注状态
    List<Integer> getFileStatusListBySubtaskId(Long subtaskId);

    // 查询任务对应子任务状态
    List<Integer> getSubtaskStatusListByTaskId(Long taskId);

    // 多人标注子任务保存标注
    void saveAnnotation(Long taskId, Long subtaskId, Long fileId, Long datasetId, Long expectedStartOffset,
                        Long expectedEndOffset, AnnotationInfoCreateDTO annotationInfoCreateDTO);

    // 多人标注子任务提交
    void submitSubtask(Long taskId, Long subtaskId);

    // 多人标注终止任务
    void terminateTask(Long taskId);

    // 更新数据集多人标注状态
    void updateModuleByDatasetId(Long datasetId, Integer moduleStatus);

    // 查询任务关联数据集
    Long queryDatasetIdByTaskId(Long id);

    // 查询任务相关子任务信息
    DataTeamTaskSubtaskInfoVO querySubtaskInfo(Long taskId);

    // 查询子任务标注情况
    DataTeamTaskSubtaskAnnotationStatusVO querySubtaskAnnotationStatus(Long subtaskId);

    Boolean validateDatasetPicNum(Long datasetId, Long teamId);

    void removeUserFromAllTeams(Long userId);

    //用户删除后的后处理
    void handleUserRemoval(Long userId);

    DataTeamVO getTeamInfoById(Long id);

    // 检查子任务是否所有图片都已标注
    Map<String, Object> checkAllAnnotated(Long subtaskId);

    // 一键确认（批量提交子任务）
    void batchConfirm(Long taskId, Long subtaskId);
}
