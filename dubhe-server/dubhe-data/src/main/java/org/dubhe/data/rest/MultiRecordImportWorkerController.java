package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.MultiRecordImportAssetsDTO;
import org.dubhe.data.domain.dto.MultiRecordImportClaimDTO;
import org.dubhe.data.domain.dto.MultiRecordImportFailDTO;
import org.dubhe.data.domain.dto.MultiRecordImportFinishDTO;
import org.dubhe.data.domain.dto.MultiRecordImportProgressDTO;
import org.dubhe.data.service.MultiDatasetService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal endpoints consumed by the recordimport Pod. */
@Api(tags = "数据处理：多模态Record导入Worker")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "internal/multi/record-imports")
public class MultiRecordImportWorkerController {
    private final MultiDatasetService multiDatasetService;

    public MultiRecordImportWorkerController(MultiDatasetService multiDatasetService) {
        this.multiDatasetService = multiDatasetService;
    }

    @ApiOperation("Worker领取导入任务")
    @PostMapping("/claim")
    public DataResponseBody claim(@Validated @RequestBody MultiRecordImportClaimDTO dto) {
        return new DataResponseBody(multiDatasetService.claimImportTask(dto));
    }

    @ApiOperation("Worker上报导入进度")
    @PostMapping("/{taskId}/progress")
    public DataResponseBody progress(@PathVariable Long taskId,
                                     @Validated @RequestBody MultiRecordImportProgressDTO dto) {
        multiDatasetService.reportProgress(taskId, dto);
        return new DataResponseBody();
    }

    @ApiOperation("Worker批量提交已上传资源")
    @PostMapping("/{taskId}/assets")
    public DataResponseBody assets(@PathVariable Long taskId,
                                   @Validated @RequestBody MultiRecordImportAssetsDTO dto) {
        multiDatasetService.saveAssets(taskId, dto);
        return new DataResponseBody();
    }

    @ApiOperation("Worker完成导入")
    @PostMapping("/{taskId}/complete")
    public DataResponseBody complete(@PathVariable Long taskId,
                                     @Validated @RequestBody MultiRecordImportFinishDTO dto) {
        multiDatasetService.finishImport(taskId, dto);
        return new DataResponseBody();
    }

    @ApiOperation("Worker报告导入失败")
    @PostMapping("/{taskId}/fail")
    public DataResponseBody fail(@PathVariable Long taskId,
                                 @Validated @RequestBody MultiRecordImportFailDTO dto) {
        multiDatasetService.failImport(taskId, dto);
        return new DataResponseBody();
    }
}
