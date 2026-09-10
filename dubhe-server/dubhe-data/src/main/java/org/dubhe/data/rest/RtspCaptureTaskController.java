package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.entity.RtspCaptureTask;
import org.dubhe.data.service.RtspCaptureTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "数据处理：数据回流任务")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/rtsp-capture-tasks")
public class RtspCaptureTaskController {

    @Autowired
    private RtspCaptureTaskService rtspCaptureTaskService;

    @ApiOperation(value = "回流任务分页查询")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Page<RtspCaptureTask>> page(Page<RtspCaptureTask> page, RtspCaptureTask query) {
        return new DataResponseBody<>(rtspCaptureTaskService.page(page, query));
    }

    @ApiOperation(value = "回流任务详情")
    @GetMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<RtspCaptureTask> detail(@PathVariable Long id) {
        return new DataResponseBody<>(rtspCaptureTaskService.detail(id));
    }

    @ApiOperation(value = "创建回流任务")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> create(@RequestBody CaptureTaskCreateDTO dto) {
        RtspCaptureTask task = new RtspCaptureTask();
        task.setTaskName(dto.getTaskName());
        task.setSourceType(dto.getSourceType());
        task.setRtspSourceId(dto.getRtspSourceId());
        task.setHttpCameraServerId(dto.getHttpCameraServerId());
        task.setHttpCameraId(dto.getHttpCameraId());
        task.setDatasetGroupId(dto.getDatasetGroupId());
        task.setCaptureInterval(dto.getCaptureInterval());
        task.setImageQuantity(dto.getImageQuantity());
        task.setAnnotateType(dto.getAnnotateType());
        task.setLabelIds(dto.getLabelIds());
        return new DataResponseBody<>(rtspCaptureTaskService.create(task, dto.getDatasetGroupName()));
    }

    @ApiOperation(value = "更新回流任务")
    @PutMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> update(@PathVariable Long id, @RequestBody CaptureTaskCreateDTO dto) {
        RtspCaptureTask task = new RtspCaptureTask();
        task.setTaskName(dto.getTaskName());
        task.setSourceType(dto.getSourceType());
        task.setRtspSourceId(dto.getRtspSourceId());
        task.setHttpCameraServerId(dto.getHttpCameraServerId());
        task.setHttpCameraId(dto.getHttpCameraId());
        task.setDatasetGroupId(dto.getDatasetGroupId());
        task.setCaptureInterval(dto.getCaptureInterval());
        task.setImageQuantity(dto.getImageQuantity());
        return new DataResponseBody<>(rtspCaptureTaskService.update(id, task));
    }

    @ApiOperation(value = "删除回流任务")
    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> delete(@PathVariable Long id) {
        return new DataResponseBody<>(rtspCaptureTaskService.delete(id));
    }

    @ApiOperation(value = "开始采集（自动在数据集组下创建数据集）")
    @PostMapping("/{id}/start")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> startCapture(@PathVariable Long id,
                                                  @RequestBody(required = false) CaptureStartDTO dto) {
        boolean delRaw = dto != null && Boolean.TRUE.equals(dto.getDelRaw());
        return new DataResponseBody<>(rtspCaptureTaskService.startCapture(id, delRaw));
    }

    @Data
    public static class CaptureTaskCreateDTO {
        private String taskName;
        private String sourceType;
        private Long rtspSourceId;
        private Long httpCameraServerId;
        private Long httpCameraId;
        /** 已有数据集组 ID（与 datasetGroupName 二选一） */
        private Long datasetGroupId;
        /** 新建数据集组名称（datasetGroupId 为空时使用） */
        private String datasetGroupName;
        private Integer captureInterval;
        private Integer imageQuantity;
        /** 数据集标注类型（102=目标检测，103=语义分割），为空则默认 102 */
        private Integer annotateType;
        /** 绑定的标签 ID 列表（dubhe-data 全局 label 表的 id），自动创建数据集时绑定 */
        private List<Integer> labelIds;
    }

    @Data
    public static class CaptureStartDTO {
        private Boolean delRaw;
    }
}
