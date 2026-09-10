package org.dubhe.data.rest;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.VideoDatasetCreateDTO;
import org.dubhe.data.domain.dto.VideoDatasetFileCommitDTO;
import org.dubhe.data.domain.dto.VideoDatasetUploadUrlDTO;
import org.dubhe.data.domain.vo.VideoDatasetFileVO;
import org.dubhe.data.service.VideoDatasetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(tags = "数据处理：视频数据集")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/video/datasets")
public class VideoDatasetController {
    private final VideoDatasetService videoDatasetService;

    public VideoDatasetController(VideoDatasetService videoDatasetService) {
        this.videoDatasetService = videoDatasetService;
    }

    @ApiOperation("创建视频数据集")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody create(@Validated @RequestBody VideoDatasetCreateDTO dto) {
        return new DataResponseBody(videoDatasetService.create(dto));
    }

    @ApiOperation("获取视频文件的 MinIO 上传地址")
    @PostMapping("/{datasetId}/files/upload-url")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody uploadUrl(@PathVariable Long datasetId,
                                      @Validated @RequestBody VideoDatasetUploadUrlDTO dto) {
        return new DataResponseBody(videoDatasetService.createUploadUrl(datasetId, dto));
    }

    @ApiOperation("提交已上传的视频文件")
    @PostMapping("/{datasetId}/files/commit")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody commit(@PathVariable Long datasetId,
                                   @Validated @RequestBody VideoDatasetFileCommitDTO dto) {
        return new DataResponseBody(videoDatasetService.commitFile(datasetId, dto));
    }

    @ApiOperation("获取视频数据集详情")
    @GetMapping("/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody detail(@PathVariable(name = "datasetId") Long datasetId) {
        return new DataResponseBody(videoDatasetService.getDetail(datasetId));
    }

    @ApiOperation("分页查询独立视频数据集文件列表")
    @GetMapping("/{datasetId}/files")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody listFiles(@PathVariable Long datasetId,
                                      @RequestParam(defaultValue = "1") Long current,
                                      @RequestParam(defaultValue = "10") Long size) {
        Page<VideoDatasetFileVO> page = new Page<>(current, size);
        IPage<VideoDatasetFileVO> result = videoDatasetService.listFiles(datasetId, page);
        return new DataResponseBody(PageUtil.toPage(result));
    }
}
