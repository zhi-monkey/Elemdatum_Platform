package org.dubhe.data.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.DatasetLabelInfoDTO;
import org.dubhe.data.domain.dto.PcDatasetAnnotationDTO;
import org.dubhe.data.domain.dto.PcDatasetCreateDTO;
import org.dubhe.data.domain.dto.PcDatasetFileCommitDTO;
import org.dubhe.data.domain.dto.PcDatasetUploadUrlDTO;
import org.dubhe.data.domain.vo.PcDatasetDetailVO;
import org.dubhe.data.domain.vo.PcDatasetFileVO;
import org.dubhe.data.service.PcDatasetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "数据处理：点云 PCD 数据集")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/pointcloud/datasets")
public class PcDatasetController {
    private final PcDatasetService pcDatasetService;

    public PcDatasetController(PcDatasetService pcDatasetService) {
        this.pcDatasetService = pcDatasetService;
    }

    @ApiOperation("创建点云数据集")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody create(@Validated @RequestBody PcDatasetCreateDTO dto) {
        return new DataResponseBody(pcDatasetService.create(dto));
    }

    @ApiOperation("获取 PCD 文件的 MinIO 上传地址")
    @PostMapping("/{datasetId}/files/upload-url")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody uploadUrl(@PathVariable Long datasetId,
                                      @Validated @RequestBody PcDatasetUploadUrlDTO dto) {
        return new DataResponseBody(pcDatasetService.createUploadUrl(datasetId, dto));
    }

    @ApiOperation("提交已上传的 PCD 文件")
    @PostMapping("/{datasetId}/files/commit")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody commit(@PathVariable Long datasetId,
                                   @Validated @RequestBody PcDatasetFileCommitDTO dto) {
        return new DataResponseBody(pcDatasetService.commitFile(datasetId, dto));
    }

    @ApiOperation("查询点云数据集详情")
    @GetMapping("/{datasetId}/detail")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<PcDatasetDetailVO> detail(@PathVariable Long datasetId) {
        return new DataResponseBody<>(pcDatasetService.detail(datasetId));
    }

    @ApiOperation("分页查询点云数据集 PCD 文件列表")
    @GetMapping("/{datasetId}/files")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody listFiles(@PathVariable Long datasetId,
                                                              @RequestParam(defaultValue = "1") Long current,
                                                              @RequestParam(defaultValue = "10") Long size) {
        Page<PcDatasetFileVO> page = new Page<>(current, size);
        IPage<PcDatasetFileVO> result = pcDatasetService.listFiles(datasetId, page);
        // 统一返回 { result, page } 分页结构，避免前端 BasicTable 无法识别 IPage.records。
        return new DataResponseBody(PageUtil.toPage(result));
    }

    @ApiOperation("删除单个 PCD 文件")
    @DeleteMapping("/{datasetId}/files/{fileId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteFile(@PathVariable Long datasetId, @PathVariable Long fileId) {
        pcDatasetService.deleteFile(datasetId, fileId);
        return new DataResponseBody();
    }

    @ApiOperation("获取 PCD 文件的 MinIO 预签名下载 URL")
    @GetMapping("/{datasetId}/files/{fileId}/download")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<String> downloadUrl(@PathVariable Long datasetId, @PathVariable Long fileId) {
        return new DataResponseBody<>(pcDatasetService.getDownloadUrl(datasetId, fileId));
    }

    @ApiOperation("查询点云数据集标签列表")
    @GetMapping("/{datasetId}/labels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<DatasetLabelInfoDTO>> getLabels(@PathVariable Long datasetId) {
        return new DataResponseBody<>(pcDatasetService.getLabels(datasetId));
    }

    @ApiOperation("保存点云 PCD 文件的 3D Box 标注")
    @PutMapping("/{datasetId}/files/{fileId}/annotations")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<PcDatasetFileVO> saveAnnotations(@PathVariable Long datasetId,
                                                             @PathVariable Long fileId,
                                                             @Validated @RequestBody PcDatasetAnnotationDTO dto) {
        return new DataResponseBody<>(pcDatasetService.saveAnnotations(datasetId, fileId, dto));
    }

    @ApiOperation("删除点云数据集")
    @DeleteMapping("/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteDataset(@PathVariable Long datasetId) {
        pcDatasetService.deleteDataset(datasetId);
        return new DataResponseBody();
    }
}
