package org.dubhe.data.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.ImportTransferTaskCreateDTO;
import org.dubhe.data.domain.dto.ImportTransferTaskProgressDTO;
import org.dubhe.data.domain.vo.ImportTransferTaskVO;
import org.dubhe.data.service.ImportTransferTaskService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Api(tags = "数据处理：传输导入任务")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/import-tasks")
public class ImportTransferTaskController {
    private final ImportTransferTaskService service;

    public ImportTransferTaskController(ImportTransferTaskService service) {
        this.service = service;
    }

    @ApiOperation("创建导入任务")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody create(@RequestBody ImportTransferTaskCreateDTO dto) { return new DataResponseBody(service.create(dto)); }

    @ApiOperation("导入任务列表")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody list(@RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "20") long size,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String datasetType) {
        IPage<ImportTransferTaskVO> page = service.list(current, size, status, datasetType);
        return new DataResponseBody(PageUtil.toPage(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody get(@PathVariable Long id) { return new DataResponseBody(service.get(id)); }

    @PostMapping("/{id}/progress")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody progress(@PathVariable Long id, @RequestBody ImportTransferTaskProgressDTO dto) {
        service.progress(id, dto); return new DataResponseBody();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody complete(@PathVariable Long id) { service.complete(id, null); return new DataResponseBody(); }

    @PostMapping("/{id}/fail")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody fail(@PathVariable Long id, @RequestParam(required = false) String message) {
        service.fail(id, message); return new DataResponseBody();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody cancel(@PathVariable Long id) { service.cancel(id); return new DataResponseBody(); }

    @PostMapping("/{id}/retry")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody retry(@PathVariable Long id) { service.retry(id); return new DataResponseBody(); }

    @DeleteMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody delete(@RequestBody List<Long> ids) { service.delete(ids); return new DataResponseBody(); }
}
