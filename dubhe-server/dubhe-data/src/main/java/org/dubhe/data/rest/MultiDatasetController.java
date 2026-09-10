package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.MultiDatasetCreateDTO;
import org.dubhe.data.domain.dto.MultiRecordUploadUrlDTO;
import org.dubhe.data.service.MultiDatasetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "数据处理：多模态数据集")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/multi/datasets")
public class MultiDatasetController {
    private final MultiDatasetService multiDatasetService;

    public MultiDatasetController(MultiDatasetService multiDatasetService) {
        this.multiDatasetService = multiDatasetService;
    }

    @ApiOperation("创建多模态数据集")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody create(@Validated @RequestBody MultiDatasetCreateDTO dto) {
        return new DataResponseBody(multiDatasetService.create(dto));
    }

    @ApiOperation("获取 Apollo Record 的 MinIO 上传地址")
    @PostMapping("/{datasetId}/record-imports/upload-url")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody uploadRecordUrl(@PathVariable Long datasetId,
                                            @Validated @RequestBody MultiRecordUploadUrlDTO dto) {
        return new DataResponseBody(multiDatasetService.createRecordUploadUrl(datasetId, dto));
    }

    @ApiOperation("确认 Apollo Record 已上传并创建导入任务")
    @PostMapping("/{datasetId}/record-imports/{taskId}/commit")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody commitRecord(@PathVariable Long datasetId, @PathVariable Long taskId) {
        return new DataResponseBody(multiDatasetService.commitRecord(datasetId, taskId));
    }

    @ApiOperation("查询 Apollo Record 导入任务")
    @GetMapping("/{datasetId}/record-imports/{taskId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody importTask(@PathVariable Long datasetId, @PathVariable Long taskId) {
        return new DataResponseBody(multiDatasetService.getImportTask(datasetId, taskId));
    }
}
