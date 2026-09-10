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
import org.dubhe.data.domain.entity.LabelTemplatePointcloud;
import org.dubhe.data.service.LabelTemplatePointcloudService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 点云标签库（独立表 label_template_pointcloud）
 * @author mingming
 * @date 2025/04/16
 */
@Api(tags = "点云标签库")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/labelTemplatePointcloud")
public class LabelTemplatePointcloudController {

    @Resource
    private LabelTemplatePointcloudService labelTemplatePointcloudService;
    @Resource
    private AuditLogHelper auditLogHelper;

    @GetMapping
    public DataResponseBody<List<LabelTemplatePointcloud>> getAll() {
        return new DataResponseBody<>(labelTemplatePointcloudService.getAll());
    }

    @ApiOperation(value = "点云标签库分页列表")
    @GetMapping(value = "/query")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody query(Page page, LabelTemplatePointcloud labelTemplatePointcloud) {
        return new DataResponseBody(labelTemplatePointcloudService.listVO(page, labelTemplatePointcloud));
    }

    @ApiOperation(value = "点云标签库创建")
    @PostMapping(value = "/create")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "label_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody create(@Validated @RequestBody LabelTemplatePointcloud labelTemplatePointcloud) {
        return new DataResponseBody(labelTemplatePointcloudService.save(labelTemplatePointcloud));
    }

    @ApiOperation(value = "点云标签编辑")
    @PutMapping(value = "/update/{labelTemplateId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody update(@PathVariable(name = "labelTemplateId") Long labelTemplateId,
                                   @Validated @RequestBody LabelTemplatePointcloud labelTemplatePointcloud) {
        boolean updated = labelTemplatePointcloudService.updateById(labelTemplatePointcloud);
        if (updated) {
            auditLogHelper.saveUpdateAuditLog("label_update",
                    "  labelTemplateId: " + labelTemplateId + "  name: " + labelTemplatePointcloud.getName());
        }
        return new DataResponseBody(updated);
    }

    @ApiOperation(value = "点云标签详情")
    @GetMapping(value = "/slectById/{labelTemplateId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody get(@PathVariable(name = "labelTemplateId") Long labelTemplateId) {
        LabelTemplatePointcloud labelTemplatePointcloud = labelTemplatePointcloudService.getById(labelTemplateId);
        return new DataResponseBody(labelTemplatePointcloud);
    }

    @ApiOperation(value = "点云标签库删除")
    @DeleteMapping(value = "/delete")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "label_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody delete(@Validated @RequestBody LabelTemplateDeleteDTO labelTemplateDeleteDTO) {
        labelTemplatePointcloudService.delete(labelTemplateDeleteDTO);
        return new DataResponseBody();
    }
}