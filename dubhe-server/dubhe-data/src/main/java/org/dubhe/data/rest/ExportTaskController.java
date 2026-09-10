package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.ExportTaskCreateDTO;
import org.dubhe.data.domain.vo.ExportTaskVO;
import org.dubhe.data.service.ExportTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * @description 数据导出任务
 * @date 2026-08-29
 */
@Api(tags = "数据处理：数据导出任务")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets/export")
public class ExportTaskController {

    @Autowired
    private ExportTaskService exportTaskService;

    @Autowired
    private MinioUtil minioUtil;

    @Value("${minio.bucketName}")
    private String bucketName;

    @ApiOperation("提交导出任务")
    @PostMapping(value = "/tasks")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody createTask(@RequestBody ExportTaskCreateDTO dto) {
        return new DataResponseBody(exportTaskService.createTask(dto));
    }

    @ApiOperation("导出任务列表")
    @GetMapping(value = "/tasks")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody listTasks(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "20") long size) {
        return new DataResponseBody(PageUtil.toPage(exportTaskService.listTasks(current, size)));
    }

    @ApiOperation("查询导出任务进度")
    @GetMapping(value = "/tasks/{taskId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getTask(@PathVariable String taskId) {
        return new DataResponseBody(exportTaskService.getTask(taskId));
    }

    @ApiOperation("确认导出任务")
    @PostMapping(value = "/tasks/{taskId}/confirm")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody confirm(@PathVariable String taskId) {
        exportTaskService.confirm(taskId);
        return new DataResponseBody();
    }

    @ApiOperation("取消导出任务")
    @PostMapping(value = "/tasks/{taskId}/cancel")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody cancel(@PathVariable String taskId) {
        exportTaskService.cancel(taskId);
        return new DataResponseBody();
    }

    @ApiOperation("批量删除导出任务")
    @DeleteMapping(value = "/tasks")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteTasks(@RequestBody List<Long> ids) {
        exportTaskService.deleteTasks(ids);
        return new DataResponseBody();
    }

    @ApiOperation("下载导出文件")
    @GetMapping(value = "/tasks/{taskId}/download")
    @PreAuthorize(Permissions.DATA)
    public void download(@PathVariable String taskId, HttpServletResponse response) throws Exception {
        exportTaskService.download(taskId, response);
    }
}
