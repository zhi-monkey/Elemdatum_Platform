package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.service.TransferExportTaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Api(tags = "数据处理：传输导出任务")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/export-tasks")
public class TransferExportTaskController {
    private final TransferExportTaskService service;
    public TransferExportTaskController(TransferExportTaskService service) { this.service = service; }
    @GetMapping @PreAuthorize(Permissions.DATA)
    public DataResponseBody list(@RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "20") long size,
                                 @RequestParam(required = false) String status, @RequestParam(required = false) String datasetType) {
        return new DataResponseBody(PageUtil.toPage(service.list(current, size, status, datasetType)));
    }
    @GetMapping("/{id}") @PreAuthorize(Permissions.DATA)
    public DataResponseBody get(@PathVariable Long id) { return new DataResponseBody(service.get(id)); }
    @PostMapping("/{id}/cancel") @PreAuthorize(Permissions.DATA)
    public DataResponseBody cancel(@PathVariable Long id) { service.cancel(id); return new DataResponseBody(); }
    @DeleteMapping @PreAuthorize(Permissions.DATA)
    public DataResponseBody delete(@RequestBody List<Long> ids) { service.delete(ids); return new DataResponseBody(); }
    @GetMapping("/{id}/download") @PreAuthorize(Permissions.DATA)
    public void download(@PathVariable Long id, HttpServletResponse response) throws Exception { service.download(id, response); }
}
