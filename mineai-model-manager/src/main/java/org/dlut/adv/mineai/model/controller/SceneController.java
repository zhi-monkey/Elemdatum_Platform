package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.entity.RequestInfoContext;
import org.dlut.adv.mineai.core.entity.RequestInfoContextHolder;
import org.dlut.adv.mineai.core.entity.Scene;
import org.dlut.adv.mineai.core.entity.UserContext;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.domain.vo.SceneWithApplicationsVO;
import org.dlut.adv.mineai.model.service.SceneService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/scene")
public class SceneController {

    @Resource
    private SceneService sceneService;

    @Resource
    private AuditLogService auditLogService;

    @GetMapping("/findAll")
    public Msg<List<Scene>> findAll() {
        return new Msg<>(MsgCode.SUCCEED, sceneService.findAll());
    }

    @GetMapping("/findAllActive")
    public Msg<List<Scene>> findAllActive() {
        return new Msg<>(MsgCode.SUCCEED, sceneService.findAllNotDelete());
    }

    @GetMapping("/findAllActiveWithApplications")
    public Msg<List<SceneWithApplicationsVO>> findAllActiveWithApplications() {
        return new Msg<>(MsgCode.SUCCEED, sceneService.findAllActiveWithApplications());
    }

    @GetMapping("/dynamicFindScenePage")
    public Msg<Page<JSONObject>> dynamicFindScenePage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(required = false) String vagueInfo,
            Scene scene) {

        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf(end));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");

        Page<Scene> queryPage = vagueInfo != null
                ? sceneService.getVagueScenePage(pageable, vagueInfo)
                : sceneService.dynamicFindScenePage(pageable, scene);

        List<JSONObject> sceneJsonList = queryPage.getContent().stream().map(scene1 -> {
            JSONObject sceneJson = JSON.parseObject(JSONObject.toJSONStringWithDateFormat(scene1, "yyyy-MM-dd HH:mm:ss"));
            return sceneJson
                    .fluentPut("canDelete", sceneService.isSceneDeletable(scene1))
                    .fluentPut("canEdit", sceneService.isSceneEditable(scene1));
        }).collect(Collectors.toList());

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(sceneJsonList, pageable, queryPage.getTotalElements()));
    }

    @PostMapping("/save")
    public Msg<Void> saveOrUpdate(@RequestBody Scene scene) {
        boolean isCreate = scene.getId() == null;
        if (!sceneService.isSceneNameExist(scene.getName()).isEmpty()) {
            return new Msg<>(MsgCode.SCENE_NAME_EXIST);
        }
        try {
            sceneService.saveOrUpdate(scene);
            if (isCreate) {
                saveAddAuditLog("scene_add", "  name: " + scene.getName());
            } else {
                saveUpdateAuditLog("scene_update", "  sceneId: " + scene.getId() + "  name: " + scene.getName());
            }
        } catch (Exception e) {
            return new Msg<>(MsgCode.SCENE_IS_BINDED);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }

    @DeleteMapping("/delete/{id}")
    @SystemControllerLog(description = "scene_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public Msg<Void> deleteById(@PathVariable Long id) {
        try {
            sceneService.deleteById(id);
        } catch (Exception e) {
            return new Msg<>(MsgCode.SCENE_IS_BINDED);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }

    @GetMapping("findSceneListByIds")
    public Msg<List<Scene>> findSceneListByIds(@RequestParam List<Long> ids) {
        return new Msg<>(MsgCode.SUCCEED, sceneService.findSceneListByIds(ids));
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
