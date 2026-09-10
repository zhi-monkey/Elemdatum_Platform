package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.cloud.authconfig.audit.AuditLogHelper;
import org.dubhe.cloud.authconfig.audit.SystemControllerLog;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.vo.DataTeamVO;
import org.dubhe.data.service.DataTeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.rest
 * @Project：mineai
 * @name：DataTeamController
 * @Date：2024/3/11 16:07
 * @Filename：DataTeamController
 * @Desc：多人标注
 */

@Api(tags = "数据处理：多人标注")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets/team")
public class DataTeamController {

    // 多人标注服务
    @Autowired
    private DataTeamService dataTeamService;
    @Autowired
    private AuditLogHelper auditLogHelper;

    @ApiOperation("查询团队")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getTeams(Page page, DataTeamQueryDTO criteria) {
        return new DataResponseBody(dataTeamService.queryAll(criteria, page));
    }
    @ApiOperation("根据id查询唯一团队")
    @GetMapping(value = "/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DataTeamVO> getTeamInfoById(@PathVariable(value = "id") Long id) {
        return new DataResponseBody(dataTeamService.getTeamInfoById(id));
    }

    @ApiOperation("新增团队")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "data_team_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody create(@Valid @RequestBody DataTeamCreateDTO dataTeamCreateDTO) {
        return new DataResponseBody(dataTeamService.create(dataTeamCreateDTO));
    }

    @ApiOperation("修改团队")
    @PutMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody update(@Valid @RequestBody DataTeamUpdateDTO dataTeamUpdateDTO) {
        Object result = dataTeamService.update(dataTeamUpdateDTO);
        auditLogHelper.saveUpdateAuditLog("data_team_update",
                "  dataTeamId: " + dataTeamUpdateDTO.getId() + "  name: " + dataTeamUpdateDTO.getName());
        return new DataResponseBody(result);
    }

    @ApiOperation("删除团队")
    @DeleteMapping
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "data_team_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody delete(@Valid @RequestBody DataTeamDeleteDTO dataTeamDeleteDTO) {
        dataTeamService.delete(dataTeamDeleteDTO.getIds());
        return new DataResponseBody();
    }

    @ApiOperation("查询该用户所有创建的团队")
    @GetMapping(value = "/all")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getTeams() {
        return new DataResponseBody(dataTeamService.queryAllTeams());
    }

    @ApiOperation("查询任务")
    @GetMapping(value = "/task")
    public DataResponseBody getTasks(Page page, DataTeamTaskQueryDTO criteria) {
        return new DataResponseBody(dataTeamService.queryAllTasks(page, criteria));
    }

    @ApiOperation("新增任务")
    @PostMapping(value = "/task")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "data_team_task_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody createTask(@Valid @RequestBody DataTeamTaskCreateDTO dataTeamTaskCreateDTO) {
        return new DataResponseBody(dataTeamService.createTask(dataTeamTaskCreateDTO));
    }

    @ApiOperation("删除任务")
    @DeleteMapping(value = "/task")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "data_team_task_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody deleteTask(@Valid @RequestBody DataTeamTaskDeleteDTO dataTeamTaskDeleteDTO) {
        dataTeamService.deleteTask(dataTeamTaskDeleteDTO.getIds());
        return new DataResponseBody();
    }

    @ApiOperation("查询子任务")
    @GetMapping(value = "/subtask")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getSubtasks(Page page, DataTeamSubtaskQueryDTO criteria) {
        return new DataResponseBody(dataTeamService.queryAllSubtasks(page, criteria));
    }

    @ApiOperation("调整任务比例")
    @PutMapping(value = "/task/adjust")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody adjustTaskProportions(@Valid @RequestBody DataTeamTaskAdjustDTO dataTeamTaskAdjustDTO) {
        Object result = dataTeamService.adjustTaskProportions(
                dataTeamTaskAdjustDTO.getTaskId(),
                dataTeamTaskAdjustDTO.getUserAllocations()
        );
        auditLogHelper.saveUpdateAuditLog("data_team_task_update",
                "  taskId: " + dataTeamTaskAdjustDTO.getTaskId());
        return new DataResponseBody(result);
    }

    @ApiOperation("查询任务相关子任务信息")
    @GetMapping(value = "/{taskId}/subtask/info")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody querySubtaskInfo(@PathVariable(value = "taskId") Long taskId) {
        return new DataResponseBody(dataTeamService.querySubtaskInfo(taskId));
    }

    @ApiOperation("查询子任务标注情况")
    @GetMapping(value = "/{subtaskId}/subtask/annotation/status")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody querySubtaskAnnotationStatus(@PathVariable(value = "subtaskId") Long subtaskId) {
        return new DataResponseBody(dataTeamService.querySubtaskAnnotationStatus(subtaskId));
    }

    @ApiOperation("子任务内保存标注")
    @PostMapping(value = "/{datasetId}/{fileId}/{taskId}/{subtaskId}/annotations")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody saveAnnotation(@PathVariable(value = "datasetId") Long datasetId,
                                           @PathVariable(value = "fileId") Long fileId,
                                           @PathVariable(value = "taskId") Long taskId,
                                           @PathVariable(value = "subtaskId") Long subtaskId,
                                           @RequestParam(value = "expectedStartOffset", required = false) Long expectedStartOffset,
                                           @RequestParam(value = "expectedEndOffset", required = false) Long expectedEndOffset,
                                           @RequestBody AnnotationInfoCreateDTO annotationInfoCreateDTO) {
        try {
            dataTeamService.saveAnnotation(taskId, subtaskId, fileId, datasetId, expectedStartOffset, expectedEndOffset,
                    annotationInfoCreateDTO);
        } catch (Exception e) {
            return new DataResponseBody<>(ResponseCode.ERROR, e.getMessage(), null);
        }
        return new DataResponseBody();
    }

    @ApiOperation("子任务手动提交")
    @PostMapping(value = "/{taskId}/{subtaskId}/subtask/submit")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody submitSubtask(
            @PathVariable(value = "taskId") Long taskId,
            @PathVariable(value = "subtaskId") Long subtaskId) {
        dataTeamService.submitSubtask(taskId, subtaskId);
        return new DataResponseBody();
    }

    @ApiOperation("任务终止")
    @PostMapping(value = "/{taskId}/task/terminate")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody terminateTask(@PathVariable(value = "taskId") Long taskId) {
        dataTeamService.terminateTask(taskId);
        return new DataResponseBody();
    }

    @ApiOperation("校验数据集图片数量与人员数量")
    @GetMapping(value = "/validateDatasetPicNum/{datasetId}/{teamId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody validateDatasetPicNum(@PathVariable(value = "datasetId") Long datasetId, @PathVariable(value = "teamId") Long teamId) {
        return new DataResponseBody(dataTeamService.validateDatasetPicNum(datasetId, teamId));
    }

    @ApiOperation("删除用户的后处理")
    @PostMapping(value = "/handleUserRemoval")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody handleUserRemoval(@RequestParam long userId) {
        dataTeamService.handleUserRemoval(userId);
        return new DataResponseBody();
    }

    @ApiOperation("检查子任务是否所有图片都已标注")
    @GetMapping(value = "/{subtaskId}/check-all-annotated")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody checkAllAnnotated(@PathVariable(value = "subtaskId") Long subtaskId) {
        return new DataResponseBody(dataTeamService.checkAllAnnotated(subtaskId));
    }

    @ApiOperation("一键确认（批量提交子任务）")
    @PostMapping(value = "/{taskId}/{subtaskId}/batch-confirm")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody batchConfirm(
            @PathVariable(value = "taskId") Long taskId,
            @PathVariable(value = "subtaskId") Long subtaskId) {
        dataTeamService.batchConfirm(taskId, subtaskId);
        return new DataResponseBody();
    }


}

