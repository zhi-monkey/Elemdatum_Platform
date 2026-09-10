package org.dlut.adv.mineai.model.controller;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.domain.vo.ModelVO;
import org.dlut.adv.mineai.model.domain.vo.WeightPathVO;
import org.dlut.adv.mineai.model.dto.UserDTO;
import org.dlut.adv.mineai.model.dto.ZipFileDTO;
import org.dlut.adv.mineai.model.service.*;
import org.dlut.adv.mineai.model.service.inter.ModelClassificationStatistics;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.dlut.adv.mineai.model.utils.ZipFileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/model")
public class ModelController {
    @Resource
    private ModelService modelService;

    @Resource
    private ModelMonitorService modelMonitorService;

    @Resource
    private ModelVersionService modelVersionService;

    @Resource
    private ModelGenerationService modelGenerationService;

    @Resource
    private MonitorModelConfigService monitorModelConfigService;

    @Resource
    private ModelJobService modelJobService;

    @Resource
    private ModelUploadService modelUploadService;

    @Resource
    private ModelUserService modelUserService;

    @Resource
    private ZipFileUtil zipFileUtil;

    @Resource
    private AuditLogService auditLogService;

    @Value("${zlm.secret}")
    private String secret;

    @Value("${zlm.rtsp.prefix}")
    private String rtspPrefix;

    @Value("${datasetRootPath}")
    private String datasetRootPath;

    @Value("${container.weightPath}")
    private String weightPath;

    /**
     * 分页查找
     *
     * @param page
     * @param order
     * @param pageSize
     * @param vagueInfo
     * @param model
     * @return
     */
    @GetMapping("/dynamicFindModelPage")
    public Msg<Page<JSONObject>> dynamicFindModelPage(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "desc") String order,
                                                      @RequestParam(defaultValue = "15") int pageSize,
                                                      @RequestParam(required = false) String vagueInfo,
                                                      @RequestParam(required = false) String chipType, // 新增 chipType
                                                      Model model) {

        // 如果 chipType 不为空，则设置到 Model 的 Chip 对象中
        if (StringUtils.isNotBlank(chipType)) {
            Chip chip = new Chip();
            chip.setChipType(chipType);
            model.setChip(chip);
        }

        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<Model> queryPage;
        if (StringUtils.isBlank(vagueInfo)) {
            queryPage = modelService.dynamicFindModelPage(pageable, model, chipType);
        } else {
            queryPage = modelService.getVagueModelPage(pageable, vagueInfo);
        }
        List<Model> modelList = queryPage.getContent();

        List<Long> userIdList = modelList.stream().map(Model::getCreateUserId).collect(Collectors.toList());

        List<UserDTO> usersByIds = modelUserService.findUsersByIds(userIdList);
        Map<Long, UserDTO> userMap = usersByIds.stream()
                .collect(Collectors.toMap(UserDTO::getId, Function.identity()));

        List<JSONObject> modelJsonList = modelList.stream().map(model1 -> {
            JSONObject modelJson = JSON.parseObject(JSONObject.toJSONStringWithDateFormat(model1, "yyyy-MM-dd HH:mm:ss"));

            UserDTO user = userMap.get(model1.getCreateUserId());
            if (user != null) {
                // 将 UserDTO 转为 JSONObject（同样格式化日期）
                JSONObject userJson = JSON.parseObject(
                        JSONObject.toJSONStringWithDateFormat(user, "yyyy-MM-dd HH:mm:ss")
                );
                modelJson.fluentPut("createUser", userJson);
            } else {
                modelJson.fluentPut("createUser", null);
            }

            return modelJson.fluentPut("canDelete", modelService.isBindSth(model1));
        }).collect(Collectors.toList());
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelJsonList, pageable, queryPage.getTotalElements()));
    }

    @GetMapping("/dynamicFindModelPageWithoutEnglishName")
    public Msg<Page<JSONObject>> dynamicFindModelPageWithoutEnglishName(@RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "asc") String order,
                                                                        @RequestParam(defaultValue = "15") int pageSize,
                                                                        @RequestParam(required = false) String vagueInfo,
                                                                        @RequestParam(required = false) String chipType, // 新增 chipType
                                                                        Model model) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        if (model.getDescription() == null) {
            model.setDescription("");
        }
        if (model.getModelEnglishName() == null) {
            model.setModelEnglishName("");
        }
        if (model.getModelName() == null) {
            model.setModelName("");
        }
        Page<Model> queryPage;
        if (vagueInfo != null) {
            queryPage = modelService.getVagueModelPageWithoutEnglishName(pageable, vagueInfo);
        } else {
            queryPage = modelService.dynamicFindModelPage(pageable, model,chipType);
        }
        List<JSONObject> modelJsonList = queryPage.getContent().stream().map(model1 -> {
            JSONObject modelJson = JSON.parseObject(JSONObject.toJSONString(model1));
            return modelJson.fluentPut("canDelete", modelService.isBindSth(model1));
        }).collect(Collectors.toList());
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelJsonList, pageable, queryPage.getTotalElements()));
    }

    /**
     * 新增 Model
     */
    @RequestMapping("/addModel")
    @SystemControllerLog(description = "model_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public Msg<String> addModel(@RequestBody Model model) {
        if (!modelService.isModelExist(model) && !modelService.isModelEnglishNameExist(model)) {
            model.setSubsystem(Model.CENTRAL_PLATFORM);
            modelService.saveModel(model);
            return new Msg<>(MsgCode.SUCCEED);
        } else if (modelService.isModelExist(model)) {
            return new Msg<>(MsgCode.MODEL_NAME_EXIST);
        } else if (modelService.isModelEnglishNameExist(model)) {
            return new Msg<>(MsgCode.MODEL_ENGLISH_NAME_EXIST);
        }
        return new Msg<>(MsgCode.ADD_MODEL_FAILED);
    }

    /**
     * 更新 Model
     *
     * @param model
     * @return
     */
    @ApiOperation(value = "更新 Model")
    @RequestMapping("/updateModel")
    public Msg<Boolean> updateModel(@ApiParam("算法Model实体") @RequestBody Model model) {
        MsgCode msgCode = modelService.updateModel(model);
        if (MsgCode.SUCCEED.equals(msgCode)) {
            saveUpdateAuditLog("model_update",
                    "  modelId: " + model.getId() + "  modelName: " + model.getModelName());
        }
        return new Msg<>(msgCode);
    }

    private void saveUpdateAuditLog(String description, String params) {
        saveAuditLog(description, params, AuditLogConstants.OperationType.UPDATE);
    }

    private void saveDownloadAuditLog(String description) {
        saveAuditLog(description, null, AuditLogConstants.OperationType.DOWNLOAD);
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
        auditLogModel.setCreateDate(new Date());
        auditLogModel.setIp(requestInfoContext.getIpAddress());
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri());
        auditLogModel.setMethod(requestInfoContext.getRequestMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(description);
        auditLogModel.setParams(params);
        auditLogService.saveAuditLog(auditLogModel);
    }

    @ApiOperation(value = "删除基础算法库中的算法", notes = "同时更新发布该算法的 ModelJob 的状态为训练成功")
    @RequestMapping("/deleteModel/{modelJobId}")
    @SystemControllerLog(description = "model_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public Msg<String> deleteModel(@ApiParam("算法Model实体") @RequestBody Model model, @ApiParam("ModelJobId") @PathVariable Long modelJobId) {
        if (modelService.getAllBindModelIds().contains(model.getId())){
            return new Msg<>(MsgCode.MODEL_IS_BIND);
        }
        MineService mineService = modelService.findModelMineServiceByModelId(model);
        if (mineService != null) {
            modelService.deleteModelMineServiceByModelId(model);
        }
        modelMonitorService.deleteMonitorAndModel(model.getId());
        boolean status = modelService.deleteModel(model.getId());
        // 修改对应 ModelJob 的状态信息
        ModelJob modelJob = modelJobService.getModelJobById(modelJobId);
        modelJob.setStatus(ModelJobStateCodeConstant.TRAIN_SUCCEEDED);
        modelJobService.saveModelJob(modelJob);
        if (status) {
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.DELETE_MODEL_FAILED);
        }
    }

    /**
     * 根据 monitorId 查询已绑定的模型版本 List
     *
     * @param monitorId
     * @return
     */
    @RequestMapping("/findModelsByMonitorId")
    public Msg<List<Model>> findModelsByMonitorId(@RequestParam long monitorId) {
        List<Model> modelList = modelService.findModelsByMonitorId(monitorId);
        return new Msg<>(MsgCode.SUCCEED, modelList);
    }

    /**
     * 获得所有 Model
     *
     * @return model list
     */
    @RequestMapping("/getModel")
    public Msg<List<Model>> getModel() {
        List<Model> models = modelService.getModel();
        return new Msg<>(MsgCode.SUCCEED, models);
    }

    /**
     * 获取 modelName 的列表
     *
     * @return
     */
    @RequestMapping("/getModelNameList")
    public Msg<HashMap<Long, String>> getModelNameList() {
        List<Model> models = modelService.getModel();
        HashMap<Long, String> modelNameSet = new HashMap<>();
        for (Model model : models) {
            modelNameSet.put(model.getId(), model.getModelName());
        }
        return new Msg<>(MsgCode.SUCCEED, modelNameSet);
    }

    /**
     * 获得前50条Model
     *
     * @return
     */
    @RequestMapping("/findModelListByDataNumAndDesc")
    public Msg<List<Model>> findModelListByDataNumAndDesc() {
        List<Model> models = modelService.findModelListByDataNumAndDesc();
        return new Msg<>(MsgCode.SUCCEED, models);
    }

   // @RequestMapping("/findModelConfigsByModelId")
   // public Msg<List<ModelConfig>> findModelConfigsByModelId(@RequestParam long modelId) {
   //     Model m = modelService.findModelById(modelId);
   //     if (m != null) {
   //         return new Msg<>(MsgCode.SUCCEED, m.getModelConfigList());
   //     } else {
   //         return new Msg<>(MsgCode.MODEL_NOT_EXIST);
   //     }
   // }

    /**
     * 获取绑定 Model 的所有监控设备
     *
     * @param modelId
     * @return
     */
    @RequestMapping("/findMonitorByModelId")
    public List<Monitor> findMonitorByModelId(@RequestParam Long modelId) {
        List<Monitor> monitorList = modelService.findMonitorsBymodelId(modelId);
        return monitorList;
    }

    @RequestMapping("/findModelConfigsByModelId")
    public Msg<List<ModelConfig>> findModelConfigsByModelId(@RequestParam long modelId) {
        Model m = modelService.findModelById(modelId);
        if (m != null) {
            return new Msg<>(MsgCode.SUCCEED, m.getModelConfigList());
        } else {
            return new Msg<>(MsgCode.MODEL_NOT_EXIST);
        }
    }

    /**
     * 根据 modelId 查询模型提示信息
     *
     * @param modelId
     * @return
     */
    @RequestMapping("/findAreaLabelingTipByModelId")
    public Msg<String> findAreaLabelingTipByModelId(@RequestParam long modelId) {
        Model m = modelService.findModelById(modelId);
        if (m != null) {
            return new Msg<>(MsgCode.SUCCEED, m.getAreaLabelingTip());
        } else {
            return new Msg<>(MsgCode.MODEL_NOT_EXIST);
        }
    }

    /**
     * 绑定算法
     *
     * @param data 前端数据 Monitor_id ModelIdList
     * @return
     */
    @RequestMapping("/bindModel")
    public Msg<Boolean> bindModel(@RequestBody JSONObject data) {
        Long monitorId = data.getLong("monitorId");
        JSONArray modelIdArray = JSON.parseArray(data.get("modelIds").toString());
        List<Long> modelIds = modelIdArray.toJavaList(Long.class);
        Monitor monitor = modelMonitorService.getMonitorById(monitorId);
        if (monitor.getIsDelete() == Monitor.DELETE) {
            return new Msg<>(MsgCode.BIND_MODEL_FAILED);
        }
        for (Long modelId : modelIds) {
            Model model = modelService.getModelById(modelId);
            MonitorModelConfig monitorModelConfig = new MonitorModelConfig();
            monitorModelConfig.setMonitor(monitor);
            monitorModelConfig.setModel(model);
            String streamUrl = rtspPrefix + "/" + "model_" + modelId + "/" + "monitor_" + monitorId;
            monitorModelConfig.setStreamUrl(streamUrl);
            monitorModelConfigService.saveMonitorModelConfig(monitorModelConfig);
        }
        return new Msg<>(MsgCode.SUCCEED, true);
    }

    /**
     * 解绑数据集
     *
     * @param data Monitor_id ModelIdList
     * @return
     */
    @RequestMapping("/unBindModel")
    public Msg<Boolean> unBindModel(@RequestBody JSONObject data) {
        Long monitorId = data.getLong("monitorId");
        JSONArray modelIdArray = JSON.parseArray(data.get("modelIds").toString());
        List<Long> modelIds = modelIdArray.toJavaList(Long.class);
        for (Long modelId : modelIds) {
            monitorModelConfigService.deleteMonitorModelConfigByMonitorAndModel(monitorId, modelId);
        }
        return new Msg<>(MsgCode.SUCCEED, true);
    }

    @RequestMapping("/unBindModelToController")
    public Msg<Boolean> unBindModel(@RequestParam Long monitorId, @RequestParam Long modelId) {
        monitorModelConfigService.deleteMonitorModelConfigByMonitorAndModel(monitorId, modelId);
        return new Msg<>(MsgCode.SUCCEED, true);
    }

    /**
     * 根据modelName查询modelConfigList
     *
     * @param modelName [String]模型名称
     * @return [List<ModelConfig>]
     */
    @RequestMapping("/findModelConfigsByModelName")
    public Msg<List<ModelConfig>> findModelConfigsByModelName(@RequestParam String modelName) {
        List<ModelConfig> modelConfigList = modelService.findModelConfigsByModelName(modelName);
        if (modelConfigList != null) {
            return new Msg<>(MsgCode.SUCCEED, modelConfigList);
        } else {
            return new Msg<>(MsgCode.MODEL_CONFIG_LIST_IS_NULL);
        }
    }

    @RequestMapping("/findStreamByMonitorIdAndModelId")
    public Msg<String> findStreamByMonitorIdAndModelId(@RequestParam long monitorId, @RequestParam long modelId) {
        MonitorModelConfig monitorModelConfig = monitorModelConfigService.findMonitorModelConfigByMonitorIdAndModelId(modelId, monitorId);
        String streamUrl = monitorModelConfig.getStreamUrl();
        return new Msg<>(MsgCode.SUCCEED, streamUrl);
    }

    @RequestMapping("/findMonitorModelConfigByMonitorIdAndModelId")
    public Msg<MonitorModelConfig> findMonitorModelConfigByMonitorIdAndModelId(@RequestParam long monitorId, @RequestParam long modelId) {
        MonitorModelConfig monitorModelConfig = monitorModelConfigService.findMonitorModelConfigByMonitorIdAndModelId(modelId, monitorId);
        return new Msg<>(MsgCode.SUCCEED, monitorModelConfig);
    }

    /**
     * 根据modelId查modelName
     */
    @RequestMapping("/findModelNameById")
    public Msg<String> findModelNameById(@RequestParam long id) {
        String modelName = modelService.findModelById(id).getModelName();
        return new Msg<>(MsgCode.SUCCEED, modelName);
    }

    /**
     * 判断当前算法是否绑定监控设备
     */
    @RequestMapping("/isBindWithMonitor")
    public Msg<Boolean> isBindWithMonitor(@RequestParam long modelId) {
        return new Msg<>(MsgCode.SUCCEED, !modelService.findMonitorsBymodelId(modelId).isEmpty());
    }

    @RequestMapping("/findModelbyMineServiceId")
    public Msg<List<Model>> findModelbyMineServiceId(@RequestParam long mineServiceId) {
        List<Model> models = modelService.findModelbyMineServiceId(mineServiceId);
        return new Msg<>(MsgCode.SUCCEED, models);
    }


    @RequestMapping("/findModelIdByMonitorId")
    public Msg<List<Long>> findModelIdByMonitorId(@RequestParam long monitorId) {

        return new Msg<>(MsgCode.SUCCEED, modelService.findModelIdByMonitorId(monitorId));
    }

    @RequestMapping("/uploadUrlImage")
    public Msg<Model> uploadUrlImage(@RequestParam Long modelId, Long userId, String url, String showName, String level, String description, String use) {
        return modelService.uploadUrlImage(modelId, userId, url, showName, level, description, use);
    }

    @RequestMapping("/uploadLocalModel")
    public Msg<Model> uploadLocalModel(@RequestParam Long userId, Long modelId, String showName, String level, String fileName, String description, String use) {
        Msg<Model> msg = modelService.createLocal(userId, modelId, showName, level, fileName, description, use);
        //删除本地临时镜像文件
        if (!modelUploadService.deleteFile(showName + '_' + level, fileName)) {
            return new Msg<>(MsgCode.DELETE_IMAGE_FILE_FAILED);
        }
        return msg;
    }

    @GetMapping("/getAllModelInfo")
    @ApiOperation(value = "查询所有算法信息", notes = "查询算法商城中的索引信息")
    public Msg<List<ModelVO>> getAllModel() {
        List<ModelVO> result = this.modelService.getNotDeletedModelList()
                .stream()
                .map(model -> {
                    ModelVO modelVO = new ModelVO();
                    modelVO.setId(model.getId());
                    modelVO.setModelName(model.getModelName());
                    modelVO.setReleaseTime(model.getReleaseTime());
                    modelVO.setDescription(model.getDescription());
                    return modelVO;
                })
                .collect(Collectors.toList());
        log.info("查询到的未删除的算法的数量是：{}", result.size());
        return new Msg<>(MsgCode.SUCCEED, result);
    }


    /**
     * 查询 model 的权重文件路径
     *
     * @param modelId 模型id
     * @return
     */
    @RequestMapping("/getWeightPath")
    public Msg<List<Map<String, String>>> getWeightPath(long modelId) {
        Model model = modelService.getModelById(modelId);
        if (model == null) {
            throw new RuntimeException("modelId: " + modelId + "对应的model根本就jb not found");
        }

        // dataSetPath: C:/dataset/weight/job-local-cz-49
        File weightDir = new File(datasetRootPath + "/" + weightPath + "/" + model.getBestWeightPath());
        if (!weightDir.isDirectory()) {
            return new Msg<>(MsgCode.FAILED_TO_GET_WEIGHT_FILE);
        }
        File[] files = weightDir.listFiles();
        if (files == null) {
            return new Msg<>(MsgCode.FAILED_TO_GET_WEIGHT_FILE);
        } else {
            List<Map<String, String>> weightFiles = Arrays.stream(files).flatMap(file -> {
                List<Map<String, String>> fileInfoList = new ArrayList<>();
                String path = "";
                long lastModified = 0;
                String formattedDate = "";
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                if (file.isFile()) {
                    path = weightPath + "/" + model.getBestWeightPath() + "/" + file.getName();
                    lastModified = file.lastModified();
                    Date date = new Date(lastModified);
                    formattedDate = dateFormat.format(date);
                    Map<String, String> fileInfo = new HashMap<>();
                    // Populate the map with path and formatted date
                    fileInfo.put("path", path);
                    fileInfo.put("formattedDate", formattedDate);
                    fileInfoList.add(fileInfo);
                } else if (file.isDirectory() && file.listFiles() != null) {
                    File[] innerFiles = file.listFiles();
                    for (File innerFile : innerFiles) {
                        path = weightPath + "/" + model.getBestWeightPath() + "/" + file.getName() + "/" + innerFile.getName();
                        lastModified = innerFile.lastModified();
                        Date date = new Date(lastModified);
                        formattedDate = dateFormat.format(date);
                        Map<String, String> fileInfo = new HashMap<>();
                        // Populate the map with path and formatted date
                        fileInfo.put("path", path);
                        fileInfo.put("formattedDate", formattedDate);
                        fileInfoList.add(fileInfo);
                    }
                }
                return fileInfoList.stream();
            }).collect(Collectors.toList());

            // Print the list of maps
            weightFiles.forEach(System.out::println);
            return new Msg<>(MsgCode.SUCCEED, weightFiles);
        }
    }

    /**
     * 直接查询job的权重文件
     *
     * @param jobId jobId
     * @return
     */
    @RequestMapping("/getWeightPathByJobId")
    public Msg<List<Map<String, String>>> getWeightPathByJobId(Long jobId) {

        ModelJob modelJob = modelJobService.getModelJobById(jobId);

        if (modelJob == null) {
            throw new RuntimeException("jobId: " + jobId + "对应的job not found");
        }
        String modelJobWeightPath = modelJob.getWeightPath();

        // dataSetPath: C:/dataset/weight/job-local-cz-49
        File weightDir = new File(datasetRootPath + "/" + weightPath + "/" + modelJobWeightPath);
        if (!weightDir.isDirectory()) {
            return new Msg<>(MsgCode.FAILED_TO_GET_WEIGHT_FILE);
        }
        File[] files = weightDir.listFiles();
        if (files == null) {
            return new Msg<>(MsgCode.FAILED_TO_GET_WEIGHT_FILE);
        } else {
            List<Map<String, String>> weightFiles = Arrays.stream(files).flatMap(file -> {
                List<Map<String, String>> fileInfoList = new ArrayList<>();
                String path = "";
                long lastModified = 0;
                String formattedDate = "";
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                if (file.isFile()) {
                    path = weightPath + "/" + modelJobWeightPath + "/" + file.getName();
                    lastModified = file.lastModified();
                    Date date = new Date(lastModified);
                    formattedDate = dateFormat.format(date);
                    Map<String, String> fileInfo = new HashMap<>();
                    fileInfo.put("path", path);
                    fileInfo.put("formattedDate", formattedDate);
                    fileInfoList.add(fileInfo);
                } else if (file.isDirectory() && file.listFiles() != null) {
                    // 如果是目录，遍历目录中的所有文件
                    File[] innerFiles = file.listFiles();
                    for (File innerFile : innerFiles) {
                        path = weightPath + "/" + modelJobWeightPath + "/" + file.getName() + "/" + innerFile.getName();
                        lastModified = innerFile.lastModified();
                        Date date = new Date(lastModified);
                        formattedDate = dateFormat.format(date);
                        Map<String, String> fileInfo = new HashMap<>();
                        fileInfo.put("path", path);
                        fileInfo.put("formattedDate", formattedDate);
                        fileInfoList.add(fileInfo);
                    }
                }
                return fileInfoList.stream();
            }).collect(Collectors.toList());

            // Print the list of maps
            weightFiles.forEach(System.out::println);
            return new Msg<>(MsgCode.SUCCEED, weightFiles);
        }
    }


    /**
     * 查询 model 的权重文件路径，包含文件大小
     *
     * @param jobId modelJob的id
     * @return 包含文件名和文件大小信息的 VO 列表
     */
    @RequestMapping("/getWeightPathWithSize")
    public Msg<List<WeightPathVO>> getWeightPathWithSize(Long jobId) {
        ModelJob modelJob = modelJobService.getModelJobById(jobId);
        if (modelJob == null) {
            throw new RuntimeException("modelId: " + jobId + "对应的model not found");
        }

        String relativePrefix = modelJob.getWeightPath();
        File weightDir = new File(datasetRootPath + "/" + weightPath + "/" + relativePrefix);

        // 转换任务兼容：转换产物通常落在 weight/<trainJobName>/<convertJobName>/
        if (!weightDir.isDirectory() && modelJob.getJobType() == ModelJob.CONVERT) {
            String trainJobName = resolveTrainJobNameForConvert(modelJob);
            if (StringUtils.isNotBlank(trainJobName)) {
                File nestedDir = new File(datasetRootPath + "/" + weightPath + "/" + trainJobName + "/" + modelJob.getWeightPath());
                if (nestedDir.isDirectory()) {
                    weightDir = nestedDir;
                    relativePrefix = trainJobName + "/" + modelJob.getWeightPath();
                } else {
                    File trainRootDir = new File(datasetRootPath + "/" + weightPath + "/" + trainJobName);
                    File[] candidateDirs = trainRootDir.listFiles(
                            file -> file.isDirectory() && file.getName().startsWith(trainJobName + "-convert-")
                    );
                    if (candidateDirs != null && candidateDirs.length > 0) {
                        Arrays.sort(candidateDirs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                        weightDir = candidateDirs[0];
                        relativePrefix = trainJobName + "/" + candidateDirs[0].getName();
                    } else if (trainRootDir.isDirectory()) {
                        // 本地调试兜底：展示训练目录下所有产物
                        weightDir = trainRootDir;
                        relativePrefix = trainJobName;
                    }
                }
            } else {
                log.warn("convert job train name unresolved: jobId={}, jobName={}, weightPath={}",
                        jobId, modelJob.getName(), modelJob.getWeightPath());
            }
        }

        if (!weightDir.isDirectory()) {
            log.warn("getWeightPathWithSize path not found: jobId={}, jobType={}, weightPath={}, absolutePath={}",
                    jobId, modelJob.getJobType(), modelJob.getWeightPath(), weightDir.getAbsolutePath());
            return new Msg<>(MsgCode.FAILED_TO_GET_WEIGHT_FILE);
        }

        File[] files = weightDir.listFiles();
        if (files == null) {
            return new Msg<>(MsgCode.FAILED_TO_GET_WEIGHT_FILE);
        } else {
            relativePrefix = weightPath + "/" + relativePrefix;
            List<WeightPathVO> weightFiles = collectWeightFilesRecursively(weightDir, relativePrefix);

            weightFiles.forEach(System.out::println);
            return new Msg<>(MsgCode.SUCCEED, weightFiles);
        }
    }

    private String extractTrainJobNameFromConvertName(String convertJobName) {
        if (convertJobName == null) {
            return null;
        }
        int idx = convertJobName.indexOf("-convert-");
        if (idx <= 0) {
            return null;
        }
        return convertJobName.substring(0, idx);
    }

    private String resolveTrainJobNameForConvert(ModelJob modelJob) {
        if (modelJob == null) {
            return null;
        }
        String trainJobName = extractTrainJobNameFromConvertName(modelJob.getName());
        if (StringUtils.isNotBlank(trainJobName)) {
            return trainJobName;
        }
        return extractTrainJobNameFromConvertName(modelJob.getWeightPath());
    }

    private List<WeightPathVO> collectWeightFilesRecursively(File rootDir, String relativePrefix) {
        try {
            return Files.walk(rootDir.toPath())
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        WeightPathVO vo = new WeightPathVO();
                        String relativePath = rootDir.toPath().relativize(path).toString().replace("\\", "/");
                        vo.setFileName(relativePrefix + "/" + relativePath);
                        try {
                            vo.setFileSize(formatFileSize(Files.size(path)));
                        } catch (IOException e) {
                            vo.setFileSize("N/A");
                        }
                        try {
                            vo.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                    .format(new Date(Files.getLastModifiedTime(path).toMillis())));
                        } catch (IOException e) {
                            vo.setCreateTime("-");
                        }
                        vo.setStatus("ready");
                        return vo;
                    })
                    .sorted(Comparator.comparing(WeightPathVO::getFileName))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("递归读取权重文件失败: rootDir={}", rootDir.getAbsolutePath(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 格式化文件大小
     *
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    private String formatFileSize(long size) {
        if (size <= 0) {
            return "0 B";
        }
        final String[] units = new String[] {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = (int) (Math.log10(size) / Math.log10(1024));
        double formattedSize = size / Math.pow(1024, unitIndex);
        return new DecimalFormat("#,##0.#").format(formattedSize) + " " + units[unitIndex];
    }

    @SneakyThrows
    @GetMapping("/downloadWeightFilesByFileName")
    public ResponseEntity<byte[]> downloadWeightFilesByFileName(@RequestParam("filePath") String filePath,
                                                                @RequestParam(value = "recordAudit", required = false, defaultValue = "true") Boolean recordAudit) {
        String absoluteFilePath = datasetRootPath + "/" + weightPath + "/" + filePath;

        // 从本地读取文件
        Path path = Paths.get(absoluteFilePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found for filePath: " + path);
        }

        try {
            byte[] fileBytes = Files.readAllBytes(path);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(path.getFileName().toString()).build());
            if (Boolean.TRUE.equals(recordAudit)) {
                saveDownloadAuditLog("model_weight_download");
            }
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载model的权重文件
     */
    @RequestMapping("/download")
    public void downloadAll(HttpServletResponse response, long modelId) {
        Model model = modelService.getModelById(modelId);
        File weightDir = new File(datasetRootPath + "/" + weightPath + "/" + model.getBestWeightPath());
        if (!weightDir.isDirectory()) {
            return;
        }
        File[] files = weightDir.listFiles();

        if (files == null) {
            return;
        } else {
            List<File> weightFiles = Arrays.stream(files).map(file -> {
                String path = "";
                File ans = null;
                if (file.isFile()) {
                    path = datasetRootPath + "/" + weightPath + "/" + model.getBestWeightPath() + "/" + file.getName();
                    ans = new File(path);
                } else if (file.isDirectory() && file.listFiles() != null && file.listFiles().length != 0 && file.listFiles()[0].isFile()) {
                    path = datasetRootPath + "/" + weightPath + "/" + model.getBestWeightPath() + "/" + file.getName() + "/" + file.listFiles()[0].getName();
                    ans = new File(path);
                }
                return ans;
            }).collect(Collectors.toList());
            List<ByteArrayOutputStream> streams = new ArrayList<>();
            List<String> fileNms = new ArrayList<>();
            for (File one : weightFiles) {
                streams.add(zipFileUtil.getByteArrayOutputStream(one.getPath()));
                fileNms.add(one.getName());
            }
            ZipFileDTO zipFileDTO = ZipFileDTO.builder()
                    .zipFileNm("模型文件.zip")
                    .streams(streams)
                    .fileNms(fileNms)
                    .build();
            if (zipFileUtil.downloadZipFile(response, zipFileDTO)) {
                saveDownloadAuditLog("model_weight_download");
            }
        }
    }

    @GetMapping("/getModelClassigicationCount")
    @ApiOperation(value = "获取算法分类统计")
    public Msg<List<ModelClassificationStatistics>> getModelClassigicationCount() {
        return new Msg<>(MsgCode.SUCCEED, modelService.getModelClassigicationCount());
    }

    @GetMapping("/getBaseModelCount")
    @ApiOperation(value = "获取基础算法数量")
    public Msg<Long> getBaseModelCount() {
        return new Msg<>(MsgCode.SUCCEED, modelService.getBaseModelCount());
    }

    @GetMapping("/getModelByApplicationNameAndDevice")
    public Msg<Model> getModelByApplicationNameAndDevice(@RequestParam String applicationName, @RequestParam String deviceName) {
        return new Msg<>(MsgCode.SUCCEED, modelService.getModelByApplicationNameAndDevice(applicationName, deviceName));
    }

    @GetMapping("findModelById")
    public Msg<Model> findModelById(@RequestParam Long id) {
        return new Msg<>(MsgCode.SUCCEED, modelService.getModelById(id));
    }

    @GetMapping("getModelExploreByModelId/{modelId}")
    public Msg<Long> getModelExploreByModelId(@PathVariable("modelId") Long modelId) {
        return new Msg<>(MsgCode.SUCCEED, modelService.getModelExploreByModelId(modelId));
    }
}
