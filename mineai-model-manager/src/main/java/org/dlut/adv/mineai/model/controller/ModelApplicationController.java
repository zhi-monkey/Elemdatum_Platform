package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.client.DubheUserFeign;
import org.dlut.adv.mineai.model.dto.ModelApplicationDTO;
import org.dlut.adv.mineai.model.dto.SaveZipUrlDTO;
import org.dlut.adv.mineai.model.dto.UniqueModelApplicationDTO;
import org.dlut.adv.mineai.model.dto.UserDTO;
import org.dlut.adv.mineai.model.openfeign.PackageFeign;
import org.dlut.adv.mineai.model.service.*;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.MinioUtils;
import org.dlut.adv.mineai.model.vo.ModelApplicationUnbindCheckVO;
import org.dlut.adv.mineai.model.vo.ModelApplicationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/modelApplication")
public class ModelApplicationController {
    @Resource
    private ModelApplicationService modelApplicationService;

    @Resource
    private ApplicationNameService applicationNameService;

    @Resource
    private PackageFeign packageFeign;

    @Resource
    private DubheUserFeign dubheUserFeign;

    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private AuditLogService auditLogService;

    @Resource
    private ModelApplicationZipLabelService modelApplicationZipLabelService;

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private DubheUtils dubheUtils;
    @Autowired
    private ModelGenerationService modelGenerationService;
    @Value("${datasetRootPath}")
    private String datasetRootPath;

    @Value("${container.weightPath}")
    private String weightPath;
    @Autowired
    private ModelJobService modelJobService;


    /**
     * 根据请求参数构造 ModelApplication 查询条件对象
     *
     * @param applicationTaskName 任务名称
     * @param scenes              场景名称（单个）
     * @param deviceFirmWare      设备信息，支持 deviceName、firmwareVersion 或两者用 "-" 分隔
     * @param modelId             模型名称
     * @return 构造好的 ModelApplication 对象，包含关联实体的查询条件
     */
    private ModelApplication buildModelApplicationQueryParams(
            String applicationTaskName,
            String scenes,
            String deviceFirmWare,
            String modelId) {

        ModelApplication modelApplication = new ModelApplication();

        // 设置任务名称
        modelApplication.setApplicationTaskName(applicationTaskName);

        // 设置场景列表
        if (StringUtils.isNotEmpty(scenes)) {
            Scene scene = new Scene();
            scene.setName(scenes);
            List<Scene> sceneList = new ArrayList<>();
            sceneList.add(scene);
            modelApplication.setApplicableScene(sceneList);
        }

        // 设置模型信息
        if (StringUtils.isNotEmpty(modelId)) {
            Model model = new Model();
            model.setModelName(modelId);
            modelApplication.setModel(model);
        }

        // 处理设备信息，支持 deviceName、firmwareVersion 或两者拼接
        if (StringUtils.isNotEmpty(deviceFirmWare)) {
            String deviceName = null;
            String firmwareVersion = null;

            String[] parts = deviceFirmWare.split("-");
            if (parts.length == 1) {
                deviceName = parts[0];
            } else if (parts.length >= 2) {
                deviceName = parts[0];
                firmwareVersion = parts[1];
            }

            Device device = new Device();
            if (StringUtils.isNotEmpty(deviceName)) {
                device.setDeviceName(deviceName);
            }
            if (StringUtils.isNotEmpty(firmwareVersion)) {
                device.setFirmwareVersion(firmwareVersion);
            }
            modelApplication.setDevice(device);
        }

        return modelApplication;
    }

    @GetMapping("/findAll")
    public Msg<Page<ModelApplication>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int pageSize,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String applicationTaskName,
            @RequestParam(required = false) String scenes,
            @RequestParam(required = false) String deviceFirmWare,
            @RequestParam(required = false) String modelId,
            @RequestParam(defaultValue = "false") String vagueInfo  // 是否模糊查找的标志
    ) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");

        // 创建 ModelApplication 对象并设置查询参数
        // 调用方法构造查询条件对象
        ModelApplication modelApplication = buildModelApplicationQueryParams(applicationTaskName, scenes, deviceFirmWare, modelId);

        // 判断是模糊查找还是精确查找
        Page<ModelApplication> queryPage;
        if (vagueInfo != null) {
            queryPage = modelApplicationService.findVagueApplicationPage(pageable, modelApplication);
        } else {
            queryPage = modelApplicationService.dynamicFindApplicationPage(pageable, modelApplication);
        }

        List<ModelApplication> content = queryPage.getContent();

        // 获取桶名称并输出日志信息
        String bucketName = minioUtils.getBucketName();
        log.info("使用的桶名称: {}", bucketName);

        content.forEach(modelApplicationItem -> {
            String filePath = modelApplicationItem.getAppZipPath();

            if (filePath != null && !filePath.isEmpty()) {
                try {
                    // 去除存储桶名称，filePath 只包含相对路径
                    String relativeFilePath = filePath.replace(bucketName + "/", "");

                    // 从 MinIO 获取文件大小并设置
                    long fileSize = minioUtils.getMinioFileSize(bucketName, relativeFilePath);
                    modelApplicationItem.setAppZipSize(fileSize);
                } catch (Exception e) {
                    log.error("获取文件大小失败，文件不存在或无法访问，路径: {}, 错误信息: {}", filePath, e.getMessage());
                }
            } else {
                log.warn("算法包路径为空，跳过文件大小获取，应用任务ID: {}", modelApplicationItem.getId());
            }
        });

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(content, pageable, queryPage.getTotalElements()));
    }

    @GetMapping("/countAll")
    public Msg<Map<String, Long>> countAll() {
        return new Msg<>(MsgCode.SUCCEED, modelApplicationService.countDistinctApplicationNamesReleasedAndUnreleased());
    }


    @GetMapping("/findAllSubtask")
    public Msg<Page<ModelApplicationVO>> findAllSubtask(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int pageSize,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String applicationTaskName,
            @RequestParam(required = false) String modelName,
            @RequestParam(defaultValue = "false") String vagueInfo  // 是否模糊查找的标志
    ) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");

        // 创建 ModelApplication 对象并设置查询参数
        ModelApplication modelApplication = new ModelApplication();
        modelApplication.setApplicationTaskName(applicationTaskName);

        Page<ModelApplication> queryPage;
        queryPage = modelApplicationService.findSubApplicationTaskPage(pageable, modelApplication, modelName);

        // 获取当前用户角色和ID
        String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
        String username = UserContextHolder.getUserContext().getUsername();
        boolean isAdmin = "管理员".equals(roleName) || "管理人员".equals(roleName);
        // 转换为 VO 对象
        List<ModelApplicationVO> voList = queryPage.getContent().stream()
                .map(modelApp -> {
                    ModelApplicationVO vo = ModelApplicationVO.fromModelApplication(modelApp);
                    // 如果不是管理员/管理人员，且发布者不等于当前用户，则设置canBeDeleted为false
                    if (!isAdmin && !username.equals(vo.getPublisherId())) {
                        vo.setCanBeDeleted(false);
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(voList, pageable, queryPage.getTotalElements()));
    }

    //下面这个是给算法应用商城总览用的
    @GetMapping("/findAllForReal")
    public Msg<Page<ModelApplication>> findAllForReal(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int pageSize,
            @RequestParam(defaultValue = "desc") String order
    ) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");

        Page<ModelApplication> queryPage;
        queryPage = modelApplicationService.findAllApplicationPage(pageable);

        List<ModelApplication> content = queryPage.getContent();


        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(content, pageable, queryPage.getTotalElements()));
    }

    // 添加或编辑应用
    @PostMapping("/save")
    public Msg<ModelApplication> saveOrUpdate(@RequestBody ModelApplicationDTO modelApplicationDTO) {
        boolean isCreate = modelApplicationDTO.getId() == null;
        ApplicationName applicationName = applicationNameService.findApplicationNameById(modelApplicationDTO.getApplicationNameId());
        ModelApplication modelApplication = new ModelApplication();
        // id不为空，说明是在update
        if (modelApplicationDTO.getId() != null) {
            // 拿到已有的modelApplication去进行修改
            modelApplication = modelApplicationService.findModelApplicationById(modelApplicationDTO.getId());
        }
        DataResponseBody<UserDTO> response = dubheUserFeign.findUserById(dubheUtils.getAuthorization(), modelApplicationDTO.getPublisherId());
        UserDTO userDTO = response.getData();
        modelApplication.setPublisherId(userDTO.getUsername());
        modelApplication.setApplicationTaskName(modelApplicationDTO.getApplicationTaskName());
        modelApplication.setApplicationName(applicationName);
        modelApplication.setDescription(modelApplicationDTO.getDescription());
        modelApplication.setDevice(modelApplicationDTO.getDevice());
        modelApplication.setApplicableScene(modelApplicationDTO.getApplicableScene());
        modelApplication.setModel(modelApplicationDTO.getModel());
        modelApplication.setCreatorId(UserContextHolder.getUserContext().getId());
        modelApplication.setUpdateTime(Date.from(Instant.now()));
        // 先删除原先绑定场景，再绑新的
        modelApplicationService.removeBoundScene(userDTO.getId());
        ModelApplication saved = modelApplicationService.saveOrUpdate(modelApplication);
        if (saved != null) {
            if (isCreate) {
                saveAddAuditLog("model_application_add", "  applicationTaskName: " + modelApplicationDTO.getApplicationTaskName());
            } else {
                saveUpdateAuditLog("model_application_update",
                        "  modelApplicationId: " + modelApplicationDTO.getId()
                                + "  applicationTaskName: " + modelApplicationDTO.getApplicationTaskName());
            }
            return new Msg<>(MsgCode.SUCCEED, saved);
        } else {
            return new Msg<>(MsgCode.FAILED);
        }
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
        auditLogModel.setCreateDate(new Date());
        auditLogModel.setIp(requestInfoContext.getIpAddress());
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri());
        auditLogModel.setMethod(requestInfoContext.getRequestMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(description);
        auditLogModel.setParams(params);
        auditLogService.saveAuditLog(auditLogModel);
    }

    // 根据ID删除应用
    @Transactional
    @DeleteMapping("/delete/{id}")
    @SystemControllerLog(description = "model_application_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public Msg<Void> deleteById(@PathVariable Long id) {
        modelApplicationService.deleteById(id);
        modelApplicationService.removeBoundScene(id);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PostMapping("/bindReleased/{id}")
    public Msg<Void> bindReleased(@PathVariable Long id) {
        modelApplicationService.bindReleasedById(id);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PostMapping("/unbindReleased/{id}")
    public Msg<Void> unbindReleased(@PathVariable Long id) {
        modelApplicationService.unbindReleasedById(id);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @GetMapping("/checkUnbindReleased/{id}")
    public Msg<ModelApplicationUnbindCheckVO> checkUnbindReleased(@PathVariable Long id) {
        List<ModelGeneration> blockingGenerations = modelGenerationService.findBlockingGenerationsForCancelRelease(id);
        boolean canUnbind = blockingGenerations.isEmpty();
        ModelApplicationUnbindCheckVO payload = ModelApplicationUnbindCheckVO.builder()
                .canUnbind(canUnbind)
                .blockingGenerations(blockingGenerations.stream()
                        .map(generation -> ModelApplicationUnbindCheckVO.ModelGenerationBriefVO.builder()
                                .id(generation.getId())
                                .name(generation.getName())
                                .status(generation.getStatusNew())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        return new Msg<>(MsgCode.SUCCEED, payload);
    }

    @PostMapping("/bindScene")
    public Msg<Void> bindScene(@RequestBody ModelApplication modelApplication) {
        // 删除所有当前modelApplication关联的场景，再写入
        modelApplicationService.removeBoundScene(modelApplication.getId());
//        System.out.println( "modelApplication: " + modelApplication);
        ModelApplication saved = modelApplicationService.saveOrUpdate(modelApplication);
//        System.out.println( "saved: " + saved);
        if (saved != null) {
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @PostMapping("bindDevice")
    public Msg<Void> bindDevice(@RequestBody ModelApplication modelApplication) {
        // 删除所有当前modelApplication关联的设备，再写入
        modelApplicationService.removeBoundDevice(modelApplication.getId());
//        System.out.println( "modelApplication: " + modelApplication);
        ModelApplication saved = modelApplicationService.saveOrUpdate(modelApplication);
//        System.out.println( "saved: " + saved);
        if (saved != null) {
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @PostMapping("bindModel")
    public Msg<Void> bindModel(@RequestBody ModelApplication modelApplication) {
        // 删除所有当前modelApplication关联的模型，再写入
        modelApplicationService.removeBoundModel(modelApplication.getId());
//        System.out.println( "modelApplication: " + modelApplication);
        ModelApplication saved = modelApplicationService.saveOrUpdate(modelApplication);
//        System.out.println( "saved: " + saved);
        if (saved != null) {
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @PostMapping("saveZipUrl")
    public Msg<Void> saveZipUrl(@RequestBody SaveZipUrlDTO saveZipUrlDTO) {
        ModelApplication modelApplication = modelApplicationService.findModelApplicationById(saveZipUrlDTO.getModelApplicationId());

        String appZipPath = saveZipUrlDTO.getAppZipPath();
        String curPath = modelApplication.getAppZipPath();
        // 此时已经在date_label中插入完成, 返回为已经插入的label的id集合, 由于是读一行插一行, 并且label的id为自增, 所以实际上id的顺序就是标签的顺序
        List<Long> labelIdsFromFile = dubheDataFeign.saveLabelByReadFile(dubheUtils.getAuthorization(), appZipPath).getData();
        if (labelIdsFromFile == null) {
            return new Msg<>(MsgCode.CLASSES_NOT_EXISTS);
        }

        // 当前labelIdsFromFile不为空, 就可以放心把算法包对应的label移除
        List<Long> boundLabelIds = modelApplicationZipLabelService.getBoundLabelIds(modelApplication.getId());
        if (boundLabelIds != null && !boundLabelIds.isEmpty()) {
            dubheDataFeign.removeLabelByIds(dubheUtils.getAuthorization(), boundLabelIds);
        }
        // 删除已有的绑定关系
        modelApplicationZipLabelService.removeBoundLabelByModelApplicationId(modelApplication.getId());

        // 删掉之后把新的set进去
        modelApplication.setAppZipPath(appZipPath);
        // 保险起见排一下序
        Collections.sort(labelIdsFromFile);
        // 保存绑定关系
        modelApplicationZipLabelService.saveBoundLabelIds(modelApplication.getId(), labelIdsFromFile);
        modelApplication.setUpdateTime(new Date());
        ModelApplication saved = modelApplicationService.saveOrUpdate(modelApplication);
        if (saved != null) {
            // 已经上传过算法包
            if (StringUtils.isNotBlank(curPath)) {
                // 从minio移除文件
                minioUtils.removeObject(Paths.get(curPath).getFileName().toString());
            }
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @GetMapping("startPacking/{modelApplicationId}")
    public Msg<String> startPacking(@PathVariable("modelApplicationId") Long modelApplicationId,
                                    @RequestParam("authCode") String authCode) {
        ModelApplication modelApplication = modelApplicationService.findModelApplicationById(modelApplicationId);
        String appZipPath = modelApplication.getAppZipPath();
        if (appZipPath == null) {
            return new Msg<>(MsgCode.FAILED, "没有压缩包你发起什么打包任务呢宝贝");
        }
        try {
            String encodedPath = URLEncoder.encode(appZipPath, StandardCharsets.UTF_8.name());
            String taskId = packageFeign.startPacking(encodedPath,authCode);
            if (taskId != null) {
                return new Msg<>(MsgCode.SUCCEED, taskId);
            } else {
                return new Msg<>(MsgCode.FAILED);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new Msg<>(MsgCode.ENCODE_FAILED);
        }
    }

    @GetMapping("startAlterAndPack")
    public Msg<String> startAlterAndPack(
            @RequestParam("filePath") String filePath,
            @RequestParam("appZipPath") String appZipPath,
            @RequestParam("datasetId") Long datasetId,
            @RequestParam("versionName") String versionName,
            @RequestParam("jobId") Long jobId,
            @RequestParam("authCode") String authCode

    ) {
        // C:/dataset/weight/job-local-cz-449/job-local-cz-449-convert-2024-11-4-18-56-45/best.rknn
        String localFilePath = datasetRootPath + "/" + weightPath + "/" + filePath;
        File file = new File(localFilePath);

        if (!file.exists()) {
            return new Msg<>(MsgCode.FAILED, "File not found");
        }

        // localFileName: best.rknn
        String localFileName = file.getName();

        // 把这个rknn文件上传到minio中/tmp/${fileName}中；objectName: /tmp/best.rknn
        String objectName = "tmp/" + localFileName;
        if (!minioUtils.uploadLocalFile(localFilePath, objectName)) {
            return new Msg<>(MsgCode.FAILED, "File upload to minio failed");
        }

        // String classesUrl = "/" + minioUtils.getBucketName() + "/dataset/" + datasetId + "/versionFile/" + versionName + "/YOLO/annotations/classes.txt";

        ModelJob job = modelJobService.getModelJobById(jobId);
        Map<Integer, String> selectedLabels = job.getSelectedLabels();
        String yamlContent = generateClassesTxtFromMap(selectedLabels);
        String classesUrl = "package/" + job.getName() + "/classes.txt";
        try {
            minioUtils.writeString(minioUtils.getBucketName(), classesUrl, yamlContent);
            log.info("Successfully wrote classes.txt for job '{}' to MinIO path: {}/{}", job.getName(), minioUtils.getBucketName(), classesUrl);
        } catch (Exception e) {
            log.error("Failed to write data.yaml to MinIO for job {}", job.getName(), e);
            throw new RuntimeException("Failed to prepare classes.txt for YOLO job", e);
        }

        String taskId = packageFeign.startAlterAndPack(appZipPath, objectName, localFileName, classesUrl, authCode);
        if (taskId != null) {
            return new Msg<>(MsgCode.SUCCEED, taskId);
        } else {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 从标签Map生成YOLO classes.txt格式的字符串内容。
     * 每行一个类别名称，按索引顺序排列。
     * Example Output:
     * hat
     * head
     * person
     *
     * @param selectedLabels Map, 键是类别索引，值是类别名称. e.g., {0: "hat", 1: "head", 2: "person"}
     * @return classes.txt格式的字符串
     */
    public String generateClassesTxtFromMap(Map<Integer, String> selectedLabels) {
        if (selectedLabels == null || selectedLabels.isEmpty()) {
            return "";
        }

        StringBuilder txtBuilder = new StringBuilder();

        selectedLabels.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    txtBuilder.append(entry.getValue())
                            .append("\n");
                });

        return txtBuilder.toString();
    }

    @GetMapping("getReleasedApplicationName")
    public Msg<List<String>> getReleasedApplicationName() {
        List<String> applicationNames = modelApplicationService.getReleasedApplicationName();
        return new Msg<>(MsgCode.SUCCEED, applicationNames);
    }

    @PostMapping("getUniqueModelApplication")
    public Msg<ModelApplication> getDeviceAndFirmwareName(@RequestBody UniqueModelApplicationDTO uniqueModelApplicationDTO) {
        return new Msg<>(MsgCode.SUCCEED, modelApplicationService.getUniqueModelApplication(uniqueModelApplicationDTO.getApplicationName(), uniqueModelApplicationDTO.getDeviceAndFirmwareName()));
    }

    @GetMapping("getDeviceByBoundApplicationName")
    public Msg<List<Device>> getDeviceByBoundApplicationName(@RequestParam("applicationName") String applicationName) {
        List<Device> devices = modelApplicationService.getDeviceByBoundApplicationName(applicationName);
        return new Msg<>(MsgCode.SUCCEED, devices);
    }

    @GetMapping("getApplicationNameByBoundDeviceFirmware")
    public Msg<List<ApplicationName>> getApplicationNameByBoundDeviceFirmware(@RequestParam("deviceFirmware") String deviceFirmware) {
        int lastIndex = deviceFirmware.lastIndexOf('-');
        String deviceName;
        String firmware;
        if (lastIndex != -1) {
            deviceName = deviceFirmware.substring(0, lastIndex);
            firmware = deviceFirmware.substring(lastIndex + 1);
        } else {
            return new Msg<>(MsgCode.DEVICE_NOT_EXIST);
        }
        List<ApplicationName> applicationNames = modelApplicationService.findApplicationNameByBoundDeviceFirmware(deviceName, firmware);
        return new Msg<>(MsgCode.SUCCEED, applicationNames);
    }

    /**
     * 暂无用法
     * @param applicationName applicationName
     * @param deviceFirmware deviceFirmware
     * @return {@link Msg }<{@link String }>
     */
    @RequestMapping("getSplitSizeByApplicationNameAndDevice/{applicationName}/{deviceFirmware}")
    public Msg<String> getSplitSizeByApplicationNameAndDevice(@PathVariable("applicationName") String applicationName, @PathVariable("deviceFirmware") String deviceFirmware) {
        return new Msg<>(MsgCode.SUCCEED, modelApplicationService.getSplitSizeByApplicationNameAndDevice(applicationName, deviceFirmware));
    }


    @PostMapping("checkIfApplicationExists")
    public Msg<Boolean> checkIfApplicationExists(@RequestBody JSONObject jsonObject) {
        String deviceName = jsonObject.getString("deviceName");
        String firmwareVersion = jsonObject.getString("firmwareVersion");
        String applicationName = jsonObject.getString("applicationName");

        boolean exists = modelApplicationService.checkIfApplicationExists(deviceName, firmwareVersion, applicationName);
        return new Msg<>(MsgCode.SUCCEED, exists);
    }

    @GetMapping("getBoundLabels/{modelApplicationId}")
    public Msg<List<Map<String, Object>>> getBoundLabels(@PathVariable("modelApplicationId") Long modelApplicationId) {
        return new Msg<>(MsgCode.SUCCEED, modelApplicationService.getBoundLabels(modelApplicationId));
    }
}
