package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.ApplicationName;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.entity.RequestInfoContext;
import org.dlut.adv.mineai.core.entity.RequestInfoContextHolder;
import org.dlut.adv.mineai.core.entity.UserContext;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.ApplicationNameService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/applicationName")
public class ApplicationNameController {

    @Resource
    private ApplicationNameService applicationNameService;

    @Resource
    private AuditLogService auditLogService;

    @GetMapping("/findAll")
    public Msg<List<ApplicationName>> findAll() {
        return new Msg<>(MsgCode.SUCCEED, applicationNameService.findAll());
    }

    @GetMapping("/dynamicFindApplicationNamePage")
    public Msg<Page<JSONObject>> dynamicFindApplicationNamePage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(required = false) String vagueInfo,
            ApplicationName applicationName) {

        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf(end));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");

        Page<ApplicationName> queryPage = vagueInfo != null
                ? applicationNameService.getVagueApplicationNamePage(pageable, vagueInfo)
                : applicationNameService.dynamicFindApplicationNamePage(pageable, applicationName);

        List<JSONObject> sceneJsonList = queryPage.getContent().stream().map(applicationName1 -> {
            JSONObject sceneJson = JSON.parseObject(JSONObject.toJSONStringWithDateFormat(applicationName1, "yyyy-MM-dd HH:mm:ss"));
            return sceneJson
                    .fluentPut("canDelete", applicationNameService.isApplicationNameDeletable(applicationName1))
                    .fluentPut("canEdit", applicationNameService.isApplicationNameEditable(applicationName1));
        }).collect(Collectors.toList());

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(sceneJsonList, pageable, queryPage.getTotalElements()));
    }

    @GetMapping("/findByApplicationName")
    public Msg<List<ApplicationName>> findByApplicationName(@RequestParam String applicationName) {
        return new Msg<>(MsgCode.SUCCEED, applicationNameService.findByApplicationName(applicationName));
    }

    @GetMapping("/searchApplicationByName")
    public Msg<List<ApplicationName>> searchApplicationByName(@RequestParam String applicationName) {
        return new Msg<>(MsgCode.SUCCEED, applicationNameService.findByApplicationNameContaining(applicationName));
    }

    @PostMapping("/save")
    public Msg<ApplicationName> saveOrUpdate(@RequestBody ApplicationName applicationName) {
        boolean isCreate = applicationName.getId() == null;
        try {
            ApplicationName applicationNames = applicationNameService.saveOrUpdate(applicationName);
            if (isCreate) {
                saveAddAuditLog("application_name_add", "  applicationName: " + applicationName.getApplicationName());
            } else {
                saveUpdateAuditLog("application_name_update",
                        "  applicationNameId: " + applicationName.getId()
                                + "  applicationName: " + applicationName.getApplicationName());
            }
            return new Msg<>(MsgCode.SUCCEED, applicationNames);
        } catch (Exception e) {
            return new Msg<>(MsgCode.APPLICATION_IS_BINDED);
        }
    }

    @DeleteMapping("/delete/{id}")
    @SystemControllerLog(description = "application_name_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public Msg<Void> deleteById(@PathVariable Long id) {
        try {
            applicationNameService.deleteById(id);
        } catch (Exception e) {
            return new Msg<>(MsgCode.APPLICATION_IS_BINDED);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }

    private void saveAddAuditLog(String description, String params) {
        saveAuditLog(description, params, AuditLogConstants.OperationType.ADD);
    }

    private void saveUpdateAuditLog(String description, String params) {
        saveAuditLog(description, params, AuditLogConstants.OperationType.UPDATE);
    }

    private void saveAuditLog(String description, String params, int operationType) {
        UserContext userContext = UserContextHolder.getUserContext();
        RequestInfoContext requestInfoContext = RequestInfoContextHolder.getRequestInfoContext();
        if (userContext == null || requestInfoContext == null) {
            return;
        }
        AuditLogModel auditLogModel = new AuditLogModel();
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setOperationType(operationType);
        auditLogModel.setUid(userContext.getId());
        auditLogModel.setUname(userContext.getUsername());
        auditLogModel.setCreateDate(new java.util.Date());
        auditLogModel.setIp(requestInfoContext.getIpAddress());
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri());
        auditLogModel.setMethod(requestInfoContext.getRequestMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(description);
        auditLogModel.setParams(params);
        auditLogService.saveAuditLog(auditLogModel);
    }
}
