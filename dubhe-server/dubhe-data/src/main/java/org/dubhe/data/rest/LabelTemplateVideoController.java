package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.cloud.authconfig.audit.AuditLogHelper;
import org.dubhe.cloud.authconfig.audit.SystemControllerLog;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.LabelTemplateDeleteDTO;
import org.dubhe.data.domain.entity.LabelTemplateVideo;
import org.dubhe.data.service.LabelTemplateVideoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 视频标签库（独立表 label_template_video）
 */
@Api(tags = "视频标签库")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/labelTemplateVideo")
public class LabelTemplateVideoController {

    @Resource
    private LabelTemplateVideoService labelTemplateVideoService;
    @Resource
    private AuditLogHelper auditLogHelper;

    @GetMapping
    public DataResponseBody<List<LabelTemplateVideo>> getAll() {
        return new DataResponseBody<>(labelTemplateVideoService.getAll());
    }

    @ApiOperation(value = "视频标签库分页列表")
    @GetMapping(value = "/query")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody query(Page page, LabelTemplateVideo labelTemplateVideo) {
        return new DataResponseBody(labelTemplateVideoService.listVO(page, labelTemplateVideo));
    }

    @ApiOperation(value = "视频标签库创建")
    @PostMapping(value = "/create")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "label_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody create(@Validated @RequestBody LabelTemplateVideo labelTemplateVideo) {
        return new DataResponseBody(labelTemplateVideoService.save(labelTemplateVideo));
    }

    @ApiOperation(value = "视频标签编辑")
    @PutMapping(value = "/update/{labelTemplateId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody update(@PathVariable(name = "labelTemplateId") Long labelTemplateId,
                                   @Validated @RequestBody LabelTemplateVideo labelTemplateVideo) {
        boolean updated = labelTemplateVideoService.updateById(labelTemplateVideo);
        if (updated) {
            auditLogHelper.saveUpdateAuditLog("label_update",
                    "  labelTemplateId: " + labelTemplateId + "  name: " + labelTemplateVideo.getName());
        }
        return new DataResponseBody(updated);
    }

    @ApiOperation(value = "视频标签详情")
    @GetMapping(value = "/slectById/{labelTemplateId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody get(@PathVariable(name = "labelTemplateId") Long labelTemplateId) {
        LabelTemplateVideo labelTemplateVideo = labelTemplateVideoService.getById(labelTemplateId);
        return new DataResponseBody(labelTemplateVideo);
    }

    @ApiOperation(value = "视频标签库删除")
    @DeleteMapping(value = "/delete")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "label_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody delete(@Validated @RequestBody LabelTemplateDeleteDTO labelTemplateDeleteDTO) {
        labelTemplateVideoService.delete(labelTemplateDeleteDTO);
        return new DataResponseBody();
    }
}
