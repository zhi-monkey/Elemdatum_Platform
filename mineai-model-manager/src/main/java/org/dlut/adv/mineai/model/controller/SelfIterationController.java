package org.dlut.adv.mineai.model.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.entity.RequestInfoContext;
import org.dlut.adv.mineai.core.entity.RequestInfoContextHolder;
import org.dlut.adv.mineai.core.entity.UserContext;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dto.SelfIterationTaskDTO;
import org.dlut.adv.mineai.model.repository.SelfIterationJobRepo;
import org.dlut.adv.mineai.model.repository.SelfIterationTaskRepo;
import org.dlut.adv.mineai.model.service.SelfIterationCrudService;
import org.dlut.adv.mineai.model.service.SelfIterationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/selfIteration")
@Api(tags = "自迭代训练")
@Slf4j
public class SelfIterationController {

    @Autowired
    private SelfIterationService selfIterationService;

    @Autowired
    private SelfIterationCrudService crudService;

    @Autowired
    private SelfIterationTaskRepo taskRepo;

    @Autowired
    private SelfIterationJobRepo jobRepo;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/create")
    @ApiOperation("创建自迭代任务")
    @SystemControllerLog(description = "self_iteration_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public Msg<?> createTask(@RequestBody SelfIterationTaskDTO dto) {
        log.info("selfIteration create request hit, name={}, appName={}, deviceFirmware={}",
                dto != null ? dto.getTaskName() : null,
                dto != null ? dto.getApplicationName() : null,
                dto != null ? dto.getDeviceFirmware() : null);
        try {
            Object result = crudService.createTask(dto);
            log.info("selfIteration create success, resultClass={}", result != null ? result.getClass().getName() : "null");
            return new Msg<>(MsgCode.SUCCEED, result);
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @PutMapping("/update")
    @ApiOperation("更新自迭代任务")
    public Msg<?> updateTask(@RequestBody SelfIterationTaskDTO dto) {
        try {
            Object result = crudService.updateTask(dto);
            saveUpdateAuditLog(dto);
            return new Msg<>(MsgCode.SUCCEED, result);
        } catch (Exception e) {
            log.error("更新任务失败", e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    private void saveUpdateAuditLog(SelfIterationTaskDTO dto) {
        UserContext userContext = UserContextHolder.getUserContext();
        RequestInfoContext requestInfoContext = RequestInfoContextHolder.getRequestInfoContext();
        if (userContext == null || requestInfoContext == null) {
            log.warn("skip self_iteration_update audit log because context is missing, taskId={}",
                    dto != null ? dto.getId() : null);
            return;
        }

        AuditLogModel auditLogModel = new AuditLogModel();
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setOperationType(AuditLogConstants.OperationType.UPDATE);
        auditLogModel.setUid(userContext.getId());
        auditLogModel.setUname(userContext.getUsername());
        auditLogModel.setCreateDate(new java.util.Date());
        auditLogModel.setIp(requestInfoContext.getIpAddress());
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri());
        auditLogModel.setMethod(requestInfoContext.getRequestMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription("self_iteration_update");
        auditLogModel.setParams(buildUpdateAuditParams(dto));
        auditLogService.saveAuditLog(auditLogModel);
    }

    private String buildUpdateAuditParams(SelfIterationTaskDTO dto) {
        if (dto == null) {
            return null;
        }
        return "  taskId: " + dto.getId() + "  taskName: " + dto.getTaskName();
    }

    @DeleteMapping("/delete/{taskId}")
    @ApiOperation("删除自迭代任务")
    @SystemControllerLog(description = "self_iteration_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public Msg<String> deleteTask(@PathVariable Long taskId) {
        try {
            crudService.deleteTask(taskId);
            return new Msg<>(MsgCode.SUCCEED, "删除成功");
        } catch (Exception e) {
            log.error("删除任务失败，taskId={}", taskId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/detail/{taskId}")
    @ApiOperation("查询任务详情")
    public Msg<?> getTask(@PathVariable Long taskId) {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.getTask(taskId));
        } catch (Exception e) {
            log.error("查询任务失败，taskId={}", taskId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/page")
    @ApiOperation("分页查询任务列表")
    public Msg<?> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.listTasks(page, pageSize, status));
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/job/{jobId}")
    @ApiOperation("查询子任务详情")
    public Msg<?> getJob(@PathVariable Long jobId) {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.getJob(jobId));
        } catch (Exception e) {
            log.error("查询子任务失败：jobId={}", jobId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/formOptions")
    @ApiOperation("获取创建任务表单选项")
    public Msg<Object> getFormOptions() {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.getFormOptions());
        } catch (Exception e) {
            log.error("获取表单选项失败", e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/job/{jobId}/detail")
    @ApiOperation("获取子任务详细信息")
    public Msg<Object> getJobDetail(@PathVariable Long jobId) {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.getJobDetail(jobId));
        } catch (Exception e) {
            log.error("获取子任务详情失败：jobId={}", jobId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/job/{jobId}/detail-readonly")
    @ApiOperation("获取子任务详细信息（只读）")
    public Msg<Object> getJobDetailReadOnly(@PathVariable Long jobId) {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.getJobDetailReadOnly(jobId));
        } catch (Exception e) {
            log.error("获取子任务详情失败：jobId={}", jobId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/{taskId}/metrics")
    @ApiOperation("获取任务跨轮次指标对比")
    public Msg<Object> getTaskMetrics(@PathVariable Long taskId) {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.getTaskMetrics(taskId));
        } catch (Exception e) {
            log.error("获取任务指标失败，taskId={}", taskId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @PostMapping("/start/{taskId}")
    @ApiOperation("启动自迭代任务")
    public Msg<String> startTask(@PathVariable Long taskId,
                                 @RequestBody(required = false) StartTaskRequest request) {
        try {
            Boolean delRaw = request == null ? null : request.getDelRaw();
            selfIterationService.startTask(taskId, delRaw);
            return new Msg<>(MsgCode.SUCCEED, "任务已启动");
        } catch (IllegalStateException e) {
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    public static class StartTaskRequest {
        private Boolean delRaw;

        public Boolean getDelRaw() {
            return delRaw;
        }

        public void setDelRaw(Boolean delRaw) {
            this.delRaw = delRaw;
        }
    }

    @PostMapping("/job/{jobId}/retryCollect")
    @ApiOperation("子任务重采")
    public Msg<String> retryCollect(@PathVariable Long jobId) {
        try {
            selfIterationService.retryCollect(jobId);
            return new Msg<>(MsgCode.SUCCEED, "已触发重新采集");
        } catch (IllegalStateException e) {
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("子任务重采失败：jobId={}", jobId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @PostMapping("/cancel/{taskId}")
    @ApiOperation("取消自迭代任务")
    public Msg<String> cancelTask(@PathVariable Long taskId) {
        try {
            selfIterationService.cancelTask(taskId);
            return new Msg<>(MsgCode.SUCCEED, "任务已取消");
        } catch (Exception e) {
            log.error("取消任务失败，taskId={}", taskId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @PostMapping("/pause/{taskId}")
    @ApiOperation("暂停自迭代任务")
    public Msg<String> pauseTask(@PathVariable Long taskId) {
        try {
            selfIterationService.pauseTask(taskId);
            return new Msg<>(MsgCode.SUCCEED, "任务已暂停");
        } catch (IllegalStateException e) {
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("暂停任务失败，taskId={}", taskId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @PostMapping("/resume/{taskId}")
    @ApiOperation("恢复自迭代任务")
    public Msg<String> resumeTask(@PathVariable Long taskId) {
        try {
            selfIterationService.resumeTask(taskId);
            return new Msg<>(MsgCode.SUCCEED, "任务已恢复");
        } catch (IllegalStateException e) {
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("恢复任务失败，taskId={}", taskId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @PostMapping("/confirmReview/{taskId}")
    @ApiOperation("确认人工审核完成")
    public Msg<String> confirmReview(@PathVariable Long taskId) {
        try {
            selfIterationService.confirmReview(taskId);
            return new Msg<>(MsgCode.SUCCEED, "审核已确认，任务继续");
        } catch (IllegalStateException e) {
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/list")
    @ApiOperation("查询自迭代父任务列表")
    public Msg<?> list() {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.listAllTasks());
        } catch (Exception e) {
            log.error("查询任务列表失败", e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @GetMapping("/{taskId}/jobs")
    @ApiOperation("查询自迭代子任务列表")
    public Msg<?> listJobsVO(@PathVariable Long taskId) {
        try {
            return new Msg<>(MsgCode.SUCCEED, crudService.listJobs(taskId));
        } catch (Exception e) {
            log.error("查询子任务列表失败：taskId={}", taskId, e);
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        }
    }

    @PostMapping("/internal/testGpuDispatchPayload")
    @ApiOperation("内部测试GPU模型下发参数")
    public Msg<?> testGpuDispatchPayload(@RequestParam String packageTaskId,
                                         @RequestParam(required = false, defaultValue = "false") boolean execute,
                                         @RequestBody(required = false) List<Long> gpuUrlTargetIds) {
        try {
            return new Msg<>(MsgCode.SUCCEED,
                    selfIterationService.testGpuDispatchPayload(packageTaskId, gpuUrlTargetIds, execute));
        } catch (Exception e) {
            log.error("内部测试GPU模型下发参数失败，packageTaskId={}", packageTaskId, e);
            return new Msg<>(MsgCode.FAILED, "内部测试GPU模型下发参数失败");
        }
    }

    @PostMapping("/job/{jobId}/manualDispatch")
    @ApiOperation("手动下发自迭代模型包")
    public Msg<?> manualDispatch(@PathVariable Long jobId) {
        try {
            return new Msg<>(MsgCode.SUCCEED, selfIterationService.manualDispatchPackage(jobId));
        } catch (IllegalStateException e) {
            log.warn("手动下发自迭代模型包失败，jobId={}, message={}", jobId, e.getMessage());
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        } catch (Exception e) {
            log.error("手动下发自迭代模型包失败，jobId={}", jobId, e);
            return new Msg<>(MsgCode.FAILED, "手动下发失败，请检查平台地址和GPU地址配置");
        }
    }

    @PostMapping("/callback/captureComplete/{executionId}")
    public Msg<String> onCaptureComplete(@PathVariable Long executionId) {
        selfIterationService.onCaptureComplete(executionId);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PostMapping("/callback/captureFailed/{executionId}")
    public Msg<String> onCaptureFailed(@PathVariable Long executionId) {
        selfIterationService.onCaptureFailed(executionId);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PostMapping("/callback/autoLabelComplete")
    public Msg<String> onAutoLabelComplete(@RequestParam Long jobId, @RequestParam Long datasetId) {
        selfIterationService.onAutoLabelComplete(jobId, datasetId);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PostMapping("/callback/autoLabelFailed")
    public Msg<String> onAutoLabelFailed(@RequestParam Long jobId, @RequestParam Long datasetId) {
        selfIterationService.onAutoLabelFailed(jobId, datasetId);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PostMapping("/callback/trainComplete")
    public Msg<String> onTrainComplete(@RequestParam Long jobId, @RequestParam Long modelJobId) {
        selfIterationService.onTrainComplete(jobId, modelJobId);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PostMapping("/callback/trainFailed/{jobId}")
    public Msg<String> onTrainFailed(@PathVariable Long jobId) {
        selfIterationService.onTrainFailed(jobId);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PostMapping("/callback/convertComplete")
    public Msg<String> onConvertComplete(@RequestParam Long jobId, @RequestParam Long convertModelJobId) {
        selfIterationService.onConvertComplete(jobId, convertModelJobId);
        return new Msg<>(MsgCode.SUCCEED);
    }
}
