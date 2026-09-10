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
import org.dubhe.data.domain.dto.LabelGroupCreateDTO;
import org.dubhe.data.domain.dto.LabelGroupDeleteDTO;
import org.dubhe.data.domain.dto.LabelTemplateDeleteDTO;
import org.dubhe.data.domain.entity.LabelTemplate;
import org.dubhe.data.domain.vo.LabelGroupQueryVO;
import org.dubhe.data.domain.vo.LabelGroupVO;
import org.dubhe.data.service.LabelTemplateService;
import org.dubhe.data.service.impl.LabelTemplateServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author mingming
 * @date 2025/04/16
 */
@Api(tags = "标签库")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/labelTemplate")
public class LabelTemplateController {
    @Resource
    private LabelTemplateServiceImpl labelTemplateService;
    @Resource
    private AuditLogHelper auditLogHelper;

    @GetMapping
    public DataResponseBody<List<LabelTemplate>> getAllLabelTemplate(
            @RequestParam(required = false) Integer type) {
        return new DataResponseBody<>(labelTemplateService.getAllLabelTemplate(type));
    }

    @ApiOperation(value = "标签库分页列表")
    @GetMapping(value = "/query")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody query(Page page, LabelTemplate labelTemplate) {
        return new DataResponseBody(labelTemplateService.listVO(page, labelTemplate));
    }

    @ApiOperation(value = "标签库创建")
    @PostMapping(value = "/create")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "label_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody create(@Validated @RequestBody LabelTemplate labelTemplate) {
        return new DataResponseBody(labelTemplateService.save(labelTemplate));
    }

    @ApiOperation(value = "标签组编辑")
    @PutMapping(value = "/update/{labelTemplateId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody update(@PathVariable(name = "labelTemplateId") Long labelTemplateId, @Validated @RequestBody LabelTemplate labelTemplate) {
        boolean updated = labelTemplateService.updateById(labelTemplate);
        if (updated) {
            auditLogHelper.saveUpdateAuditLog("label_update",
                    "  labelTemplateId: " + labelTemplateId + "  name: " + labelTemplate.getName());
        }
        return new DataResponseBody(updated);
    }

    @ApiOperation(value = "标签组详情")
    @GetMapping(value = "/slectById/{labelTemplateId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody get(@PathVariable(name = "labelTemplateId") Long labelTemplateId) {
        LabelTemplate labelTemplate = labelTemplateService.getById(labelTemplateId);
        return new DataResponseBody(labelTemplate);
    }

    @ApiOperation(value = "标签库删除", notes = "删除标签及标签库下的标签")
    @DeleteMapping(value = "/delete")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "label_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody delete(@Validated @RequestBody LabelTemplateDeleteDTO labelTemplateDeleteDTO) {
        labelTemplateService.delete(labelTemplateDeleteDTO);
        return new DataResponseBody();
    }
}
