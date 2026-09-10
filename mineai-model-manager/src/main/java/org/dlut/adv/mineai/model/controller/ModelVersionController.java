package org.dlut.adv.mineai.model.controller;


import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dto.ModelVersionDTO;
import org.dlut.adv.mineai.model.dto.UserDTO;
import org.dlut.adv.mineai.model.harbor.HarborService;
import org.dlut.adv.mineai.model.repository.ModelVersionRepo;
import org.dlut.adv.mineai.model.service.*;
import org.dlut.adv.mineai.model.service.impl.ModelVersionServiceImpl;
import org.dlut.adv.mineai.model.service.inter.ModelVersionInterService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunzhen
 */
@Slf4j
@RequestMapping("/modelVersion")
@RestController
public class ModelVersionController {
    @Resource
    ModelVersionService modelVersionService;
    @Resource
    ModelGenerationService modelGenerationService;
    @Resource
    ModelService modelService;

    @Resource
    private ModelMonitorService modelMonitorService;

    @Resource
    private ModelVersionServiceImpl modelVersionServiceImpl;

    @Resource
    private ModelVersionInterService modelVersionInterService;

    @Resource
    private ModelJobService modelJobService;

    @Resource
    private HarborService harborService;

    @Resource
    private MonitorModelConfigService monitorModelVersionConfigService;

    @Value("${zlm.rtsp.prefix}")
    private String rtspPrefix;

    @Value("${modelImageRootPath}")
    private String modelImageRootPath;

    @Resource
    private ModelUploadService modelUploadService;

    @Resource
    private ModelVersionRepo modelVersionRepo;

    @Resource
    private ModelUserService modelUserService;

    @Resource
    private ModelExploreService modelExploreService;

    @Resource
    private AuditLogService auditLogService;

    /**
     * 获取所有版本
     */
    @RequestMapping("/getModelVersion")
    public Msg<List<ModelVersion>> getModelVersion() {
        List<ModelVersion> modelVersions = modelVersionService.getModelVersion();
        return new Msg<>(MsgCode.SUCCEED, modelVersions);
    }

    @GetMapping("/findAll")
    public List<ModelVersion> getAllModelVersions() {
        // 返回包含 chip 的 ModelVersion 列表
        return modelVersionService.getModelVersions();
    }
    /**
     * 根据modelVersion的id获取modelVersion
     */
    @RequestMapping("/findModelVersionById")
    public Msg<ModelVersion> findModelVersionById(@RequestParam long id) {
        ModelVersion modelVersion = modelVersionService.findModelVersionById(id);
        if (modelVersion == null) {
            return new Msg<>(MsgCode.MODEL_VERSION_NOT_EXIST);
        } else {
            return new Msg<>(MsgCode.SUCCEED, modelVersion);
        }
    }


//    /**
//     * 根据model获取对应的modelVersion
//     */
//    @RequestMapping("/getModelVersionNameByModel")
//    public Msg<HashMap<Long, String>> getModelVersionNameByModel(@RequestParam long modelId) {
//        Model model = modelService.findModelById(modelId);
//        List<ModelVersion> modelVersionList = modelVersionService.findModelVersionsByModelAndTrainable(model);
//        HashMap<Long, String> modelVersionNameSet = new HashMap<>();
//        for (ModelVersion modelVersion : modelVersionList) {
//            modelVersionNameSet.put(modelVersion.getId(), modelVersion.getShowName());
//        }
//        return new Msg<>(MsgCode.SUCCEED, modelVersionNameSet);
//    }


    /**
     * 分页查找
     *
     * @param page
     * @param order
     * @param pageSize
     * @param modelVersion
     * @return
     */
    /*@GetMapping("/dynamicFindModelVersionPage")
    public Msg<Page<ModelVersionDTO>> dynamicFindModelVersionPage(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "desc") String order,
                                                                  @RequestParam(defaultValue = "15") int pageSize, @RequestParam(required = false) String vagueInfo, ModelVersion modelVersion) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelVersion> queryPage;

        if (vagueInfo != null) {
            queryPage = modelVersionService.getVagueModelPage(pageable, vagueInfo);
        } else {
            queryPage = modelVersionService.dynamicFindModelVersionPage(pageable, modelVersion);
        }
        //原来是用于标注前端是否可以删除该modelversion，改为下面代码中的modelVersionDTO.setCanDelete(curMV.getIsDelete() == ModelVersion.NOT_DELETE);
//        List<JSONObject> modelVersionList = queryPage.getContent().stream().map(modelVersion1 -> {
//            JSONObject modelVersionJson = JSONArray.parseObject(JSONObject.toJSONString(modelVersion1));
//            return modelVersionJson.fluentPut("canDelete", modelVersion1.getIsDelete() == ModelVersion.NOT_DELETE);
//        }).collect(Collectors.toList());
        List<ModelVersion> content = queryPage.getContent();
        //获取当前页面所有modelgeneration的traindataset、testdataset的id
        List<Long> idList = content.stream()
                .map(ModelVersion::getRoleId).collect(Collectors.toList());
        List<ModelVersionDTO> modelVersionDTOS;


        List<UserDTO> userDTOs = modelUserService.findUsersByIds(idList);

        // 调用服务方法得到不能删除的ModelVersion列表
        List<ModelVersion> nonDeletableModelVersions = modelJobService.existsByModelVersionStatusAndJobStatus();
        List<ModelVersion> nonDeletableModelVersions2 = modelExploreService.AllNoDeleteModelVersion();
        // 将不能删除的ModelVersion列表转换为ID集合
        List<ModelVersion> newList = new ArrayList<>(nonDeletableModelVersions.size() + nonDeletableModelVersions2.size());
        newList.addAll(nonDeletableModelVersions);
        newList.addAll(nonDeletableModelVersions2);
        // 将不能删除的ModelVersion列表转换为ID集合，并去重
        Set<Long> nonDeletableModelVersionIds = newList.stream()
                .map(ModelVersion::getId)
                .collect(Collectors.toSet());

        Set<Long> nonDeletableModelVersionId2 = nonDeletableModelVersions.stream()
                .map(ModelVersion::getId)
                .collect(Collectors.toSet());

        modelVersionDTOS = content.stream().map(curMV -> {
            ModelVersionDTO modelVersionDTO = BeanUtil.toBean(curMV, ModelVersionDTO.class);
            modelVersionDTO.setUser(getUserDTOById(userDTOs, curMV.getRoleId()));
            modelVersionDTO.setCanDelete(!nonDeletableModelVersionIds.contains(curMV.getId()));
            modelVersionDTO.setCanEdit(!nonDeletableModelVersionId2.contains(curMV.getId()));
            return modelVersionDTO;
        }).collect(Collectors.toList());

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelVersionDTOS, pageable, queryPage.getTotalElements()));
    }*/
    @GetMapping("/dynamicFindModelVersionPage")
    public Msg<Page<ModelVersionDTO>> dynamicFindModelVersionPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(required = false) String vagueInfo,
            @RequestParam(required = false) String chipType, // 接收 chipType 参数
            @RequestParam(required = false) String userName,
            ModelVersion modelVersion) {

        // 去掉"end"后缀处理
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }

        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelVersion> queryPage;

        // 根据 vagueInfo 或 modelVersion 查询
        if (vagueInfo != null) {
            queryPage = modelVersionService.getVagueModelPage(pageable, vagueInfo, chipType);
        } else {
            queryPage = modelVersionService.dynamicFindModelVersionPage(pageable, modelVersion, chipType, userName);
        }

        List<ModelVersion> content = queryPage.getContent();
        List<Long> idList = content.stream().map(ModelVersion::getRoleId).collect(Collectors.toList());
        // 将 userDTOs 初始化为一个不可变的空列表
        final List<UserDTO> userDTOs;

        // 仅当 idList 不为空时才进行查询
        if (!idList.isEmpty()) {
            userDTOs = modelUserService.findUsersByIds(idList);
        } else {
            // 使用不可变的空列表
            userDTOs = Collections.emptyList();
        }



        // 调用服务方法得到不能删除的 ModelVersion 列表
        List<ModelVersion> nonDeletableModelVersions = modelJobService.existsByModelVersionStatusAndJobStatus();
        List<ModelVersion> nonDeletableModelVersions2 = modelExploreService.AllNoDeleteModelVersion();

        List<ModelVersion> newList = new ArrayList<>(nonDeletableModelVersions.size() + nonDeletableModelVersions2.size());
        newList.addAll(nonDeletableModelVersions);
        newList.addAll(nonDeletableModelVersions2);
        Set<Long> usedModelVersionIds = new HashSet<>();
        modelExploreService.findAll().forEach((modelExplore -> {
            if (modelExplore.getTrainModelVersion() != null) {
                usedModelVersionIds.add(modelExplore.getTrainModelVersion().getId());
            }
            if (modelExplore.getConvertModelVersion() != null) {
                usedModelVersionIds.add(modelExplore.getConvertModelVersion().getId());
            }
            if (modelExplore.getAutoLabelModelVersion() != null) {
                usedModelVersionIds.add(modelExplore.getAutoLabelModelVersion().getId());
            }
            if (modelExplore.getDeployModelVersion() != null){
                usedModelVersionIds.add(modelExplore.getDeployModelVersion().getId());
            }
        }));
        // 将不能删除的 ModelVersion 列表转换为 ID 集合，并去重
        Set<Long> nonDeletableModelVersionIds = newList.stream().map(ModelVersion::getId).collect(Collectors.toSet());
        Set<Long> nonDeletableModelVersionId2 = nonDeletableModelVersions.stream().map(ModelVersion::getId).collect(Collectors.toSet());

        List<ModelVersionDTO> modelVersionDTOS = content.stream().map(curMV -> {
            ModelVersionDTO modelVersionDTO = BeanUtil.toBean(curMV, ModelVersionDTO.class);

            // 设置关联的用户
            modelVersionDTO.setUser(getUserDTOById(userDTOs, curMV.getRoleId()));

            // 设置是否可以删除和编辑
            modelVersionDTO.setCanDelete(!nonDeletableModelVersionIds.contains(curMV.getId()) || !usedModelVersionIds.contains(curMV.getId()));
            modelVersionDTO.setCanEdit(!nonDeletableModelVersionId2.contains(curMV.getId()));

            // 映射 Chip 实体中的 chipType 字段
            if (curMV.getChip() != null) {
                modelVersionDTO.setChipType(curMV.getChip().getChipType());
            } else {
                modelVersionDTO.setChipType(" ");  // 若没有 chip，则设置为 " " 或其他默认值
            }

           /* // 映射 Chip 实体中的 chipType 字段
            // 直接将 Chip 实体放入 DTO
            modelVersionDTO.setChip(curMV.getChip());*/

            return modelVersionDTO;
        }).collect(Collectors.toList());

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelVersionDTOS, pageable, queryPage.getTotalElements()));
    }



    @GetMapping("/dynamicFindModelVersionPagePrivate")
    public Msg<Page<ModelVersionDTO>> dynamicFindModelVersionPagePrivate(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "desc") String order,
                                                                         @RequestParam(defaultValue = "15") int pageSize,
                                                                         @RequestParam(required = false) String vagueInfo,
                                                                         @RequestParam(required = false) String chipType, // 接收 chipType 参数
                                                                         ModelVersion modelVersion) {
        // 获取当前用户id加入查询中
        UserContext userContext = UserContextHolder.getUserContext();
        Integer userId = userContext.getId();

        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelVersion> queryPage;

        if (vagueInfo != null) {
            queryPage = modelVersionService.getVagueModelPage(pageable, vagueInfo,chipType);
        } else {
            queryPage = modelVersionService.dynamicFindModelVersionPagePrivate(pageable, modelVersion, userId,chipType);
        }

        List<ModelVersion> content = queryPage.getContent();
        List<Long> idList = content.stream()
                .map(ModelVersion::getRoleId)
                .collect(Collectors.toList());

        List<ModelVersionDTO> modelVersionDTOS = new ArrayList<>();

        if (!idList.isEmpty()) {
            List<UserDTO> userDTOs = modelUserService.findUsersByIds(idList);

            List<ModelVersion> nonDeletableModelVersions = modelJobService.existsByModelVersionStatusAndJobStatus();
            List<ModelVersion> nonDeletableModelVersions2 = modelExploreService.AllNoDeleteModelVersion();

            List<ModelVersion> newList = new ArrayList<>(nonDeletableModelVersions.size() + nonDeletableModelVersions2.size());
            newList.addAll(nonDeletableModelVersions);
            newList.addAll(nonDeletableModelVersions2);

            Set<Long> nonDeletableModelVersionIds = newList.stream()
                    .map(ModelVersion::getId)
                    .collect(Collectors.toSet());

            Set<Long> nonDeletableModelVersionId2 = nonDeletableModelVersions.stream()
                    .map(ModelVersion::getId)
                    .collect(Collectors.toSet());

            modelVersionDTOS = content.stream().map(curMV -> {
                ModelVersionDTO modelVersionDTO = BeanUtil.toBean(curMV, ModelVersionDTO.class);
                modelVersionDTO.setUser(getUserDTOById(userDTOs, curMV.getRoleId()));
                modelVersionDTO.setCanDelete(!nonDeletableModelVersionIds.contains(curMV.getId()));
                modelVersionDTO.setCanEdit(!nonDeletableModelVersionId2.contains(curMV.getId()));
                return modelVersionDTO;
            }).collect(Collectors.toList());
        }

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelVersionDTOS, pageable, queryPage.getTotalElements()));
    }


    /**
     * 查询公开镜像
     *
     * @param page
     * @param order
     * @param pageSize
     * @param modelVersion
     * @return
     */
    @GetMapping("/dynamicFindModelVersionPagePublic")
    public Msg<Page<ModelVersionDTO>> dynamicFindModelVersionPagePublic(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "desc") String order,
                                                                        @RequestParam(defaultValue = "15") int pageSize,
                                                                        @RequestParam(required = false) String vagueInfo,
                                                                        @RequestParam(required = false) String chipType, // 接收 chipType 参数
                                                                        ModelVersion modelVersion) {

        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelVersion> queryPage;

        if (vagueInfo != null) {
            queryPage = modelVersionService.getVagueModelPage(pageable, vagueInfo,chipType);
        } else {
            queryPage = modelVersionService.dynamicFindModelVersionPagePublic(pageable, modelVersion,chipType);
        }

        List<ModelVersion> content = queryPage.getContent();
        List<Long> idList = content.stream()
                .map(ModelVersion::getRoleId)
                .collect(Collectors.toList());

        List<ModelVersionDTO> modelVersionDTOS = new ArrayList<>();

        if (!idList.isEmpty()) {
            List<UserDTO> userDTOs = modelUserService.findUsersByIds(idList);

            List<ModelVersion> nonDeletableModelVersions = modelJobService.existsByModelVersionStatusAndJobStatus();
            List<ModelVersion> nonDeletableModelVersions2 = modelExploreService.AllNoDeleteModelVersion();

            List<ModelVersion> newList = new ArrayList<>(nonDeletableModelVersions.size() + nonDeletableModelVersions2.size());
            newList.addAll(nonDeletableModelVersions);
            newList.addAll(nonDeletableModelVersions2);

            Set<Long> nonDeletableModelVersionIds = newList.stream()
                    .map(ModelVersion::getId)
                    .collect(Collectors.toSet());

            Set<Long> nonDeletableModelVersionId2 = nonDeletableModelVersions.stream()
                    .map(ModelVersion::getId)
                    .collect(Collectors.toSet());

            modelVersionDTOS = content.stream().map(curMV -> {
                ModelVersionDTO modelVersionDTO = BeanUtil.toBean(curMV, ModelVersionDTO.class);
                modelVersionDTO.setUser(getUserDTOById(userDTOs, curMV.getRoleId()));
                modelVersionDTO.setCanDelete(!nonDeletableModelVersionIds.contains(curMV.getId()));
                modelVersionDTO.setCanEdit(!nonDeletableModelVersionId2.contains(curMV.getId()));
                return modelVersionDTO;
            }).collect(Collectors.toList());
        }

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelVersionDTOS, pageable, queryPage.getTotalElements()));
    }

    /**
     * 将指定的 ModelVersion 的 isPublic 字段设置为 true
     *
     * @param id ModelVersion 的 ID
     * @return 更新结果
     */
    @PutMapping("/setPublic/{id}")
    public Msg<String> setPublic(@PathVariable Long id) {
        boolean updated = modelVersionService.updateIsPublic(id, true);
        if (updated) {
            return new Msg<>(MsgCode.SUCCEED, "成功将 isPublic 设置为 true");
        } else {
            return new Msg<>(MsgCode.FAILED, "更新失败，可能是该 ModelVersion 不存在");
        }
    }

    /**
     * 将指定的 ModelVersion 的 isPublic 字段设置为 false
     *
     * @param id ModelVersion 的 ID
     * @return 更新结果
     */
    @PutMapping("/setPrivate/{id}")
    public Msg<String> setPrivate(@PathVariable Long id) {
        boolean updated = modelVersionService.updateIsPublic(id, false);
        if (updated) {
            return new Msg<>(MsgCode.SUCCEED, "成功将 isPublic 设置为 false");
        } else {
            return new Msg<>(MsgCode.FAILED, "更新失败，可能是该 ModelVersion 不存在");
        }
    }

    private UserDTO getUserDTOById(List<UserDTO> list, long id) {
        if (list == null) {
            return new UserDTO();
        }
        return list.stream().filter(userDTO -> userDTO.getId() == id).findFirst().orElse(new UserDTO());
    }
    /**
     * 删除模型版本
     */
//    @RequestMapping("/deleteModel")
//    public Msg<String> deleteModel(@RequestBody Model model) {
//        MineService mineService = modelVersionService.findModelMineServiceByModel(model);
//        if (mineService != null) {
//            //判断ModelVersion是否绑定MineService，如果有，则删除ModelVersion绑定的MineService
//            modelVersionService.deleteModelMineServiceByModel(model);
//        }
//        modelMonitorService.deleteMonitorAndModel(model.getId());
//        boolean status = modelVersionService.deleteModelVersion(modelVersion);
////        List<ModelJob> modelJobs = modelJobService.findModelJobsByModelVersionId(modelVersion.getId());
////        System.out.println("modelVersionName:"+modelVersion.getName());
////        for(ModelJob modelJob :modelJobs){
////            System.out.println("TTTTTTTTTTTTTTTTTTT:"+modelJob.getModelVersion().getId());
////            modelJobService.updateJobModelVersionName(modelVersion,modelJob);
////            System.out.println("modelJob:"+modelJob.getModelVersion().getName());
////        }
//        if (status) {
//            return new Msg<>(MsgCode.SUCCEED);
//        } else {
//            return new Msg<>(MsgCode.DELETE_MODEL_FAILED);
//        }
//
//
//    }

//    /**
//     * 新增modelversion
//     */
//    @RequestMapping("/addModelVersion")
//    public Msg<String> addModelVersion(@RequestBody ModelVersion modelVersion) {
//        if (!modelVersionService.isVersionExist(modelVersion)) {
//            modelVersionService.saveModelVersion(modelVersion);
//            return new Msg<>(MsgCode.SUCCEED);
//        } else {
//            return new Msg<>(MsgCode.ADD_MODEL_VERSION_FAILED);
//        }
//    }

    /**
     * 通过Url上传镜像文件
     * 这里的fileName就是算法的英文名
     */
    @RequestMapping("/addUrlModelVersion")
    @SystemControllerLog(description = "model_version_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public Msg<ModelVersion> addUrlModelVersion(@RequestParam String url, String generationName, String englishName, String description, String imageType) {
        return modelUploadService.uploadUrlToHarbor(url, generationName, englishName, description, imageType);
    }

    /**
     * 通过url更新镜像文件
     */
    @RequestMapping("/updateUrlModelVersion")
    public Msg<ModelVersion> updateUrlModelVersion(@RequestParam String url, String generationName, String englishName, String description, String imageType) {
        Msg<ModelVersion> msg = modelUploadService.updateUrlImage(url, generationName, englishName, description, imageType);
        if (MsgCode.SUCCEED.getCode().equals(msg.getCode())) {
            saveUpdateAuditLog("model_version_update",
                    "  generationName: " + generationName + "  englishName: " + englishName + "  imageType: " + imageType);
        }
        return msg;
    }

    /**
     * 上传本地镜像文件，解析入库
     * generationName 是生产任务的name
     * modelName是生产任务的英文名
     * 这里的fileName是镜像文件的名称
     */
    @RequestMapping("/addLocalityModelVersion")
    @SystemControllerLog(description = "model_version_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public Msg<ModelVersion> addLocalityModelVersion(@RequestParam String generationName, String englishName, String fileName, String description, String imageType) {
        //将镜像文件上传至Harbor仓库
        Msg<ModelVersion> msg = modelUploadService.uploadImageToHarbor(generationName, englishName, fileName, imageType, description);

        //删除本地临时镜像文件
        if (!modelUploadService.deleteFile(englishName, fileName)) {
            return new Msg<>(MsgCode.DELETE_IMAGE_FILE_FAILED);
        }
        if (MsgCode.SUCCEED.getCode().equals(msg.getCode())) {
            saveUpdateAuditLog("model_version_update",
                    "  generationName: " + generationName + "  englishName: " + englishName
                            + "  fileName: " + fileName + "  imageType: " + imageType);
        }
        return msg;
    }

    @RequestMapping("/updateLocalityModelVersion")
    public Msg<ModelVersion> updateLocalityModelVersion(@RequestParam String generationName, String englishName, String fileName, String description, String imageType) {
        //将镜像文件上传至Harbor仓库
        Msg<ModelVersion> msg = modelUploadService.updateLocalImage(generationName, englishName, fileName, imageType, description);
        //删除本地临时镜像文件
        if (!modelUploadService.deleteFile(englishName, fileName)) {
            return new Msg<>(MsgCode.DELETE_IMAGE_FILE_FAILED);
        }
        return msg;
    }


    /**
     * 更新版本
     */
    @RequestMapping("/updateModelVersion")
    public Msg<String> updateModelVersion(@RequestBody ModelVersion modelVersion) {
        if (!modelVersionService.isVersionExist(modelVersion)) {
            return new Msg<>(MsgCode.MODEL_VERSION_NOT_EXIST);
        }
        boolean status = modelVersionService.updateModelVersion(modelVersion);
        if (status) {
            saveUpdateAuditLog("model_version_update",
                    "  modelVersionId: " + modelVersion.getId() + "  showName: " + modelVersion.getShowName());
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.UPDATE_MODEL_VERSION_FAILED);
        }
    }

    @RequestMapping("findImageSupportAutoLabel")
    @ApiOperation(value = "获取支持自动标注的镜像列表", notes = "获取的是 ModelVersion 集合，可根据标注类型筛选")
    public Msg<List<ModelVersion>> findImageSupportAutoLabel(
            @RequestParam(value = "annotationType", required = false) String annotationType) {
        return new Msg<>(MsgCode.SUCCEED, modelVersionService.findImageSupportAutoLabel(annotationType));
    }

    @GetMapping("findMatchedAutoLabelImages")
    @ApiOperation(value = "获取与应用绑定标签匹配的自动标注镜像列表", notes = "根据应用和设备固件绑定的标签过滤默认自动标注镜像，可根据标注类型筛选")
    public Msg<List<ModelVersion>> findMatchedAutoLabelImages(
            @RequestParam("applicationName") String applicationName,
            @RequestParam("deviceFirmware") String deviceFirmware,
            @RequestParam(value = "annotationType", required = false) String annotationType) {
        return new Msg<>(MsgCode.SUCCEED, modelVersionService.findMatchedAutoLabelImages(applicationName, deviceFirmware, annotationType));
    }

    /**
     * 根据monitorId查询已绑定的模型版本List
     */
//    @RequestMapping("/findModelVersionsByMonitorId")
//    public Msg<List<ModelVersion>> findModelVersionsByMonitorId(@RequestParam long monitorId) {
//        List<ModelVersion> modelVersionList = modelVersionService.findModelVersionsByMonitorId(monitorId);
//        return new Msg<>(MsgCode.SUCCEED, modelVersionList);
//    }

    /**
     * 根据模型版本Id查询Monitor
     */
//    @RequestMapping("/findMonitorByModelVersionId")
//    public Msg<List<Monitor>> findMonitorByModelVersionId(@RequestParam Long modelVersionId) {
//        List<Monitor> monitorList = modelVersionService.findMonitorsBymodelVersionId(modelVersionId);
//        return new Msg<>(MsgCode.SUCCEED, monitorList);
//    }

    /**
     * 根据模型版本Id查询Monitor info 转化为前端使用类型
     */
//    @RequestMapping("/findMonitorsInfoByModelVersionId")
//    public Msg<List<JSONObject>> findMonitorsInfoByModelVersionId(@RequestParam Long modelVersionId) {
//        List<JSONObject> monitorList = modelVersionService.findMonitorsInfoByModelVersionId(modelVersionId);
//        return new Msg<>(MsgCode.SUCCEED, monitorList);
//    }

    /**
     * 判断当前算法版本是否绑定监控设备
     */
//    @RequestMapping("/isBindWithMonitor")
//    public Msg<Boolean> isBindWithMonitor(@RequestParam long modelVersionId) {
//        return new Msg<>(MsgCode.SUCCEED, !modelVersionService.findMonitorsBymodelVersionId(modelVersionId).isEmpty());
//    }

    /**
     * 查找当前mineService的modelVersion
     */
//    @RequestMapping("/findModelVersionByMineServiceId")
//    public Msg<List<ModelVersion>> findModelVersionByMineServiceId(@RequestParam long mineServiceId) {
//        List<ModelVersion> modelVersions = modelVersionService.findModelVersionByMineServiceId(mineServiceId);
//        return new Msg<>(MsgCode.SUCCEED, modelVersions);
//    }

//    @RequestMapping("/findMonitorsInfoByModelVersionShowName")
//    public Msg<List<JSONObject>> findMonitorsInfoByModelVersionShowName(@RequestParam String modelVersionShowName){
//        List<JSONObject> monitorList = modelVersionService.findMonitorsInfoByModelVersionShowName(modelVersionShowName);
//        return new Msg<>(MsgCode.SUCCEED,monitorList);
//    }
    @RequestMapping("/findModelVersionByModelVersionShowName")
    public Msg<ModelVersion> findModelVersionByModelVersionShowName(@RequestParam String modelVersionShowName) {
        ModelVersion modelVersion = modelVersionRepo.findModelVersionByShowNameAndIsInferable(modelVersionShowName, true);
        return new Msg<>(MsgCode.SUCCEED, modelVersion);
    }

    /**
     * 查询所有可用于自动标注的modelversion
     *
     * @return
     */
    @RequestMapping("/findAutoLabelModelVersion")
    public Msg<List<ModelVersion>> findAutoLabelModelVersion() {
        List<ModelVersion> modelVersions = modelVersionService.getAutoLabelModelVersions();
        return new Msg<>(MsgCode.SUCCEED, modelVersions);
    }

    /**
     * 解绑算法版本
     *
     * @param data Monitor_id ModelIdList
     * @return
     */
    @RequestMapping("/unBindModelVersion")
    public Msg<Boolean> unBindModelVersion(@RequestBody JSONObject data) {
        Long monitorId = data.getLong("monitorId");
        JSONArray modelVersionIdArray = JSON.parseArray(data.get("modelVersionIds").toString());
        List<Long> modelVersionIds = modelVersionIdArray.toJavaList(Long.class);
        for (Long modelVersionId : modelVersionIds) {
            monitorModelVersionConfigService.deleteMonitorModelConfigByMonitorAndModel(monitorId, modelVersionId);
        }
        return new Msg<>(MsgCode.SUCCEED, true);
    }


    /**
     * 查询monitorModelConfig根据monitorId和modelVersionId
     *
     * @param monitorId
     * @param modelVersionId
     * @return
     */
    @RequestMapping("/findMonitorModelConfigByMonitorIdAndModelVersionId")
    public Msg<MonitorModelConfig> findMonitorModelConfigByMonitorIdAndModelId(@RequestParam long monitorId, @RequestParam long modelVersionId) {
        MonitorModelConfig monitorModelConfig = monitorModelVersionConfigService.findMonitorModelConfigByMonitorIdAndModelId(modelVersionId, monitorId);
        return new Msg<>(MsgCode.SUCCEED, monitorModelConfig);
    }

    /**
     * 根据modelVersionId查询模型提示信息
     */
    @RequestMapping("/findAreaLabelingTipByModelVersionId")
    public Msg<String> findAreaLabelingTipByModelVersionId(@RequestParam long modelVersionId) {
        ModelVersion mv = modelVersionService.findModelVersionById(modelVersionId);
        if (mv != null) {
            return new Msg<>(MsgCode.SUCCEED, mv.getAreaLabelingTip());
        } else {
            return new Msg<>(MsgCode.MODEL_VERSION_NOT_EXIST);
        }
    }


    /**
     * @param use 镜像用途. 现在修改为多项选 例如可能为 "训练,自动标注"
     * @return {@link Msg }<{@link ModelVersion }>
     */
    @RequestMapping("/createUrlImage")
    @SystemControllerLog(description = "model_version_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public Msg<ModelVersionDTO> createModelVersion(@RequestParam Long userId, String url, String showName, String level, String description, String use, String chipType) {
        return modelVersionInterService.create(userId, url, showName, level, description, use, chipType);
    }

    /**
     * 更新模型版本
     *
     * @param use 镜像用途. 现在修改为多项选 例如可能为 "训练,自动标注" 但是这里传了use根本没用，如果后面要添加use相关更改, 注意一下
     * @return {@link Msg }<{@link ModelVersion }>
     */
    @RequestMapping("/updateUrlImage")
    public Msg<ModelVersion> updateModelVersion(@RequestParam Long mdId, String url, String showName, String level, String description, String use,String chipType) {
        Msg<ModelVersion> msg = modelVersionInterService.updateModelVersion(mdId, url, showName, level, description, use,chipType);
        if (MsgCode.SUCCEED.getCode().equals(msg.getCode())) {
            saveUpdateAuditLog("model_version_update",
                    "  modelVersionId: " + mdId + "  showName: " + showName + "  level: " + level);
        }
        return msg;
    }

    private void saveUpdateAuditLog(String description, String params) {
        UserContext userContext = UserContextHolder.getUserContext();
        RequestInfoContext requestInfoContext = RequestInfoContextHolder.getRequestInfoContext();
        if (userContext == null || requestInfoContext == null) {
            return;
        }
        AuditLogModel auditLogModel = new AuditLogModel();
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setOperationType(AuditLogConstants.OperationType.UPDATE);
        auditLogModel.setUid(userContext.getId());
        auditLogModel.setUname(userContext.getUsername());
        auditLogModel.setCreateDate(new Date());
        auditLogModel.setIp(requestInfoContext.getIpAddress());
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri());
        auditLogModel.setMethod(requestInfoContext.getRequestMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(description);
        auditLogModel.setParams(params);
        auditLogService.saveAuditLog(auditLogModel);
    }

    /**
     * @param use       镜像用途. 现在修改为多项选 例如可能为 "训练,自动标注"
     * @return {@link Msg }<{@link ModelVersion }>
     */// 2、Local
    @RequestMapping("/createLocalImage")
    @SystemControllerLog(description = "model_version_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public Msg<ModelVersion> createLocalModelVersion(@RequestParam Long userId, String showName, String level, String fileName, String description, String use,String chipType) {
        Msg<ModelVersion> msg = modelVersionInterService.createLocal(userId,showName, level, fileName, description, use,chipType);
        //删除本地临时镜像文件
        if (!modelUploadService.deleteFile(showName + '_' + level, fileName)) {
            return new Msg<>(MsgCode.DELETE_IMAGE_FILE_FAILED);
        }
        return msg;
    }

    /**
     * 删除模型版本
     */
    @RequestMapping("/deleteImage")
    @SystemControllerLog(description = "model_version_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public Msg<String> deleteModelVersion(@RequestParam long modelVersionId) {
        ModelVersion modelVersion = modelVersionRepo.findModelVersionById(modelVersionId);
        modelVersionService.deleteModelVersion(modelVersion);
        return new Msg<>(MsgCode.SUCCEED);
    }


    @GetMapping("/modelversions/train")
    public Msg<List<ModelVersion>> getTrainModelVersions() {
        UserContext userContext = UserContextHolder.getUserContext();
        return new Msg<>(MsgCode.SUCCEED, modelVersionService.getTrainModelVersions((long) userContext.getId()));
    }

    @GetMapping("/modelversions/deploy")
    public Msg<List<ModelVersion>> getDeployModelVersions() {
        UserContext userContext = UserContextHolder.getUserContext();
        return new Msg<>(MsgCode.SUCCEED, modelVersionService.getDeployModelVersions((long) userContext.getId()));
    }

    @GetMapping("/modelversions/conversion")
    public Msg<List<ModelVersion>> getConvertModelVersions() {
        UserContext userContext = UserContextHolder.getUserContext();
        return new Msg<>(MsgCode.SUCCEED, modelVersionService.getConversionModelVersions((long) userContext.getId()));
    }

    @RequestMapping("/modelVersionUse")
    public String ModelVersionUse(@RequestParam long modelVersionId) {
        return modelVersionInterService.ModelVersionUse(modelVersionId);
    }
}
