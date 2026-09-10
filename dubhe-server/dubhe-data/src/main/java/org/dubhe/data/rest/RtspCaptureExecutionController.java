package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.entity.RtspCaptureExecution;
import org.dubhe.data.service.RtspCaptureExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "数据处理：RTSP回流执行记录")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/rtsp-capture-executions")
public class RtspCaptureExecutionController {

    @Autowired
    private RtspCaptureExecutionService rtspCaptureExecutionService;

    @ApiOperation(value = "RTSP回流执行记录分页查询")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Page<RtspCaptureExecution>> page(Page<RtspCaptureExecution> page, RtspCaptureExecution query) {
        return new DataResponseBody<>(rtspCaptureExecutionService.page(page, query));
    }

    @ApiOperation(value = "RTSP回流执行记录详情")
    @GetMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<RtspCaptureExecution> detail(@PathVariable Long id) {
        return new DataResponseBody<>(rtspCaptureExecutionService.detail(id));
    }

    @ApiOperation(value = "RTSP回流执行记录创建")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> create(@Validated @RequestBody RtspCaptureExecution execution) {
        return new DataResponseBody<>(rtspCaptureExecutionService.create(execution));
    }

    @ApiOperation(value = "RTSP回流执行记录更新")
    @PutMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> update(@PathVariable Long id, @Validated @RequestBody RtspCaptureExecution execution) {
        return new DataResponseBody<>(rtspCaptureExecutionService.update(id, execution));
    }

    @ApiOperation(value = "RTSP回流执行记录删除")
    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> delete(@PathVariable Long id) {
        return new DataResponseBody<>(rtspCaptureExecutionService.delete(id));
    }

    @ApiOperation(value = "删除执行记录并同步删除关联数据集")
    @DeleteMapping("/{id}/with-dataset")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> deleteWithDataset(@PathVariable Long id) {
        return new DataResponseBody<>(rtspCaptureExecutionService.deleteWithDataset(id));
    }

    @ApiOperation(value = "手动停止采集")
    @PostMapping("/{id}/cancel")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> cancel(@PathVariable Long id) {
        return new DataResponseBody<>(rtspCaptureExecutionService.cancel(id));
    }
}
