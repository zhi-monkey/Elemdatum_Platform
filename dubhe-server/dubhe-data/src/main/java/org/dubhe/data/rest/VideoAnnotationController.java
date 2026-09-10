package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.VideoAnnotationTrackCreateDTO;
import org.dubhe.data.domain.dto.VideoTrackKeyframeCreateDTO;
import org.dubhe.data.domain.dto.VideoTrackKeyframeUpdateDTO;
import org.dubhe.data.service.VideoAnnotationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 视频 BBox 跟踪标注。
 */
@Api(tags = "数据处理：视频标注")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/video")
public class VideoAnnotationController {

    @Autowired
    private VideoAnnotationService videoAnnotationService;

    @ApiOperation("创建/打开视频标注任务")
    @PostMapping("/datasets/{datasetId}/files/{fileId}/annotation/tasks")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody openOrCreate(@PathVariable Long datasetId, @PathVariable Long fileId) {
        return new DataResponseBody(videoAnnotationService.openOrCreate(datasetId, fileId));
    }

    @ApiOperation("加载视频标注（元信息 + tracks + keyframes）")
    @GetMapping("/datasets/{datasetId}/files/{fileId}/annotation")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody load(@PathVariable Long datasetId, @PathVariable Long fileId) {
        return new DataResponseBody(videoAnnotationService.load(datasetId, fileId));
    }

    @ApiOperation("创建 Track")
    @PostMapping("/tracks")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody createTrack(@Valid @RequestBody VideoAnnotationTrackCreateDTO dto) {
        return new DataResponseBody(videoAnnotationService.createTrack(dto));
    }

    @ApiOperation("新增关键帧")
    @PostMapping("/tracks/{trackId}/keyframes")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody createKeyframe(@PathVariable Long trackId,
                                           @Valid @RequestBody VideoTrackKeyframeCreateDTO dto) {
        dto.setTrackId(trackId);
        return new DataResponseBody(videoAnnotationService.createKeyframe(dto));
    }

    @ApiOperation("修改关键帧")
    @PutMapping("/keyframes/{keyframeId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody updateKeyframe(@PathVariable Long keyframeId,
                                           @Valid @RequestBody VideoTrackKeyframeUpdateDTO dto) {
        return new DataResponseBody(videoAnnotationService.updateKeyframe(keyframeId, dto));
    }

    @ApiOperation("删除关键帧")
    @DeleteMapping("/keyframes/{keyframeId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteKeyframe(@PathVariable Long keyframeId) {
        videoAnnotationService.deleteKeyframe(keyframeId);
        return new DataResponseBody();
    }

    @ApiOperation("结束 Track")
    @PostMapping("/tracks/{trackId}/finish")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody finishTrack(@PathVariable Long trackId) {
        videoAnnotationService.finishTrack(trackId);
        return new DataResponseBody();
    }

    @ApiOperation("删除 Track")
    @DeleteMapping("/tracks/{trackId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteTrack(@PathVariable Long trackId) {
        videoAnnotationService.deleteTrack(trackId);
        return new DataResponseBody();
    }

    @ApiOperation("提交标注")
    @PostMapping("/files/{fileId}/annotation/submit")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody submit(@PathVariable Long fileId) {
        videoAnnotationService.submit(fileId);
        return new DataResponseBody();
    }

    @ApiOperation("审核通过")
    @PostMapping("/tasks/{taskId}/review")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody review(@PathVariable Long taskId) {
        videoAnnotationService.review(taskId);
        return new DataResponseBody();
    }

    @ApiOperation("审核驳回")
    @PostMapping("/tasks/{taskId}/reject")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody reject(@PathVariable Long taskId) {
        videoAnnotationService.reject(taskId);
        return new DataResponseBody();
    }

    @ApiOperation("任务列表（审核/进度回显）")
    @GetMapping("/datasets/{datasetId}/tasks")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody listTasks(@PathVariable Long datasetId) {
        return new DataResponseBody(videoAnnotationService.listTasks(datasetId));
    }

    @ApiOperation("导出标注")
    @GetMapping("/files/{fileId}/annotation/export")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody export(@PathVariable Long fileId,
                                   @RequestParam(required = false, defaultValue = "json") String format) {
        return new DataResponseBody(videoAnnotationService.export(fileId, format));
    }
}
