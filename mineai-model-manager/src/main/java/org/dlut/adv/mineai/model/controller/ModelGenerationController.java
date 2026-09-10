package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.constant.Constant;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.core.vo.DatasetVO;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.core.vo.ModelGenerationVO;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.dto.*;
import org.dlut.adv.mineai.model.dto.BindVersionsRequestDTO;
import org.dlut.adv.mineai.model.dto.JobPrecisionInfoDTO;
import org.dlut.adv.mineai.model.dto.ModelGenerationBoundVersionDTO;
import org.dlut.adv.mineai.model.dto.UserDTO;
import org.dlut.adv.mineai.model.dto.*;
import org.dlut.adv.mineai.model.exception.BusinessException;
import org.dlut.adv.mineai.model.kubernetes.service.LogService;
import org.dlut.adv.mineai.model.service.*;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.vo.ModelGenerationBindInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/modelGeneration")
@Api(tags = "生产任务请求控制器")
public class ModelGenerationController {

    @Value("${datasetRootPath}")
    private String datasetRootPath;

    @Value("${kubernetes.namespace}")
    private String namespace;

    @Value("${container.weightPath}")
    private String weightPath;

    @Value("${kubernetes.rootPath.datasetRootPath}")
    private String minioPrefix;

    @Resource
    private ModelVersionService modelVersionService;

    @Resource
    private ModelGenerationService modelGenerationService;

    @Resource
    private LogService logService;

    @Resource
    private HardwareParamsService hardwareParamsService;

    @Resource
    private ModelDeploymentService modelDeploymentService;

    @Resource
    private ModelJobService modelJobService;

    @Resource
    private ModelDatasetService modelDatasetService;

    @Resource
    private ModelUserService modelUserService;

    @Resource
    private ModelExploreService modelExploreService;

    @Resource
    private ModelService modelService;

    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private AuditLogService auditLogService;

    @Resource
    private DubheUtils dubheUtils;

    @Resource
    private ChipService chipService;
    @Autowired
    private ModelJobController modelJobController;
    @Resource
    private ModelApplicationZipLabelService modelapplicationziplabelService;

    @GetMapping("/dynamicFindModelGenerationPage")
    public Msg<Page<ModelGenerationVO>> dynamicFindScenePage(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "desc") String order,
                                                             @RequestParam(defaultValue = "15") int pageSize,
                                                             @RequestParam(required = false) String vagueInfo, ModelGeneration modelGeneration) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Page<ModelGeneration> modelGenerationPage;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        if (vagueInfo != null) {
            modelGenerationPage = modelGenerationService.getVagueModelGenerationPage(pageable, vagueInfo);
        } else {
            modelGenerationPage = modelGenerationService.dynamicFindModelGenerationPage(pageable, modelGeneration);
        }
        List<ModelGenerationVO> modelGenerationVOS = modelGenerationPage.getContent().stream().map(currentModelGeneration -> {
            ModelGenerationVO modelGenerationVO = new ModelGenerationVO(currentModelGeneration);
            modelGenerationVO.setTrainDataset(modelDatasetService.selectDatasetVersionById(currentModelGeneration.getTrainDataset()));
            modelGenerationVO.setTestDataset(modelDatasetService.selectDatasetVersionById(currentModelGeneration.getTestDataset()));
            return modelGenerationVO;
        }).collect(Collectors.toList());
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelGenerationVOS, pageable, modelGenerationPage.getTotalElements()));
    }

    /**
     * 分页查询非引导类作业
     *
     * @param page
     * @param order
     * @param pageSize
     * @param vagueInfo
     * @param modelGeneration
     * @return
     */
    @GetMapping("/dynamicFindUnGuidedModelGenerationPage")
    public Msg<Page<ModelGenerationVO>> dynamicFindUnGuidedModelGenerationPage(@RequestParam(defaultValue = "0") int page,
                                                                               @RequestParam(defaultValue = "desc") String order,
                                                                               @RequestParam(defaultValue = "15") int pageSize,
                                                                               @RequestParam(required = false) String vagueInfo,
                                                                               @RequestParam(required = false) String userName,
                                                                               ModelGeneration modelGeneration) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelGeneration> queryPage;
        if (vagueInfo != null) {
            queryPage = modelGenerationService.getVagueUnGuidedModelGenerationPage(pageable, vagueInfo);
        } else {
            queryPage = modelGenerationService.dynamicFindUnGuidedModelGenerationPage(pageable, modelGeneration, userName);
        }
        List<ModelGeneration> content = queryPage.getContent();
        //获取当前页面所有modelgeneration的traindataset、testdataset的id
        List<Long> idList = new ArrayList<>(content.stream()
                .map(mgTmp -> Arrays.asList(mgTmp.getTrainDataset(), mgTmp.getTestDataset(), mgTmp.getValDataset()))
                .reduce(new ArrayList<>(), (x, y) -> {
                    ArrayList<Long> list = new ArrayList<>(x);
                    list.addAll(y);
                    return list;
                }));
        List<ModelGenerationVO> modelGenerationVOS;
        // idList size为0 说明不用发出请求查找版本  但不能说明 没有generation
        if (idList.size() != 0) {
            List<DatasetVersionVO> datasetVersionVOS = modelDatasetService.findDatasetVersionsByIds(idList);

            modelGenerationVOS = content.stream().map(currentModelGeneration -> {
                ModelGenerationVO modelGenerationVO = new ModelGenerationVO(currentModelGeneration);
                // 其实只有一个用户
                List<UserDTO> users = modelUserService.findUsersByIds(Arrays.asList(currentModelGeneration.getUserId()));
                modelGenerationVO.setUserName(users.get(0).getUsername());
//                modelGenerationVO.setTrainDataset(getDVById(datasetVersionVOS, currentModelGeneration.getTrainDataset()));
//                modelGenerationVO.setTestDataset(getDVById(datasetVersionVOS, currentModelGeneration.getTestDataset()));
//               // System.out.println("getDVById(datasetVersionVOS, currentModelGeneration.getValDataset()) = " + getDVById(datasetVersionVOS, currentModelGeneration.getValDataset()));
//                modelGenerationVO.setValDataset(getDVById(datasetVersionVOS, currentModelGeneration.getValDataset()));
//                if(currentModelGeneration.getStatus() == ModelGeneration.TEST_SUCCESS){
//                    modelGenerationVO.setCanPublish(modelGenerationService.compareModelGeneration(currentModelGeneration));
//                } else {
//                    modelGenerationVO.setCanPublish(false);
//                }
                return modelGenerationVO;
            }).collect(Collectors.toList());
        } else {
            modelGenerationVOS = content.stream().map(currentModelGeneration -> {
                ModelGenerationVO modelGenerationVO = new ModelGenerationVO(currentModelGeneration);
//                if(currentModelGeneration.getStatus() == ModelGeneration.TEST_SUCCESS){
//                    modelGenerationVO.setCanPublish(modelGenerationService.compareModelGeneration(currentModelGeneration));
//                } else {
//                    modelGenerationVO.setCanPublish(false);
//                }
                return modelGenerationVO;
            }).collect(Collectors.toList());
        }
        modelGenerationVOS.stream().forEach(modelGenerationVO -> {
            ModelVersion deployMV = modelGenerationVO.getDeployModelVersion();
            if (deployMV != null) {
                List<Deployment> deployments = modelDeploymentService.findDeploymentsByModelVersion(deployMV);
                modelGenerationVO.setHasDeployments(deployments.toArray().length != 0);
            }
        });
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelGenerationVOS, pageable, queryPage.getTotalElements()));
    }

    private DatasetVersionVO getDVById(List<DatasetVersionVO> list, long id) {
        if (list == null) {
            return new DatasetVersionVO();
        }
        return list.stream().filter(datasetVersionVO -> datasetVersionVO.getId() != null && datasetVersionVO.getId().equals(id)).findFirst().orElse(new DatasetVersionVO());
    }

    /**
     * 分页查询引导类作业
     *
     * @param page
     * @param order
     * @param pageSize
     * @param vagueInfo
     * @param modelGeneration
     * @return
     */
    @GetMapping("/dynamicFindGuidedModelGenerationPage")
    public Msg<Page<ModelGenerationVO>> dynamicFindGuidedModelGenerationPage(@RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "desc") String order,
                                                                             @RequestParam(defaultValue = "15") int pageSize, @RequestParam(required = false) String vagueInfo, ModelGeneration modelGeneration) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelGeneration> queryPage;
        if (vagueInfo != null) {
            queryPage = modelGenerationService.getVagueGuidedModelGenerationPage(pageable, vagueInfo);
        } else {
            queryPage = modelGenerationService.dynamicFindGuidedModelGenerationPage(pageable, modelGeneration);
        }
        List<ModelGeneration> content = queryPage.getContent();
        //获取当前页面所有modelgeneration的traindataset、testdataset的id
        List<Long> idList = new ArrayList<>(content.stream()
                .map(mgTmp -> Arrays.asList(mgTmp.getTrainDataset(), mgTmp.getTestDataset()))
                .reduce(new ArrayList<>(), (x, y) -> {
                    ArrayList<Long> list = new ArrayList<>(x);
                    list.addAll(y);
                    return list;
                }));

        List<ModelGenerationVO> modelGenerationVOS;
        // idList size为0 说明不用发出请求查找版本  但不能说明 没有generation
        if (!idList.isEmpty()) {
            List<DatasetVersionVO> datasetVersionVOS = modelDatasetService.findDatasetVersionsByIds(idList);

            // 1. 收集所有 trainDatasetStart ID，去重
            Set<Long> trainDatasetStartIds = content.stream()
                    .map(ModelGeneration::getTrainDatasetStart)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 2. 批量调用 dubheDataFeign，获取所有数据集信息
            Object rawData = dubheDataFeign.batchGet(dubheUtils.getAuthorization(), new ArrayList<>(trainDatasetStartIds)).getData();
            Map<String, DatasetVO> datasetMap = (Map<String, DatasetVO>) rawData;

            modelGenerationVOS = content.stream().map(currentModelGeneration -> {
                ModelGenerationVO modelGenerationVO = new ModelGenerationVO(currentModelGeneration);
//                modelGenerationVO.setTrainDatasetStartAll(dubheDataFeign.get(dubheUtils.getCurrentAuthorization(), currentModelGeneration.getTrainDatasetStart()).getData());
                modelGenerationVO.setTrainDatasetStartAll(datasetMap.get(currentModelGeneration.getTrainDatasetStart().toString()));
                modelGenerationVO.setTrainDataset(getDVById(datasetVersionVOS, currentModelGeneration.getTrainDataset()));
                // 一般情况也不可能为null
                if (currentModelGeneration.getModelApplication() != null) {
                    modelGenerationVO.setModelApplication(currentModelGeneration.getModelApplication());
                }
                if (currentModelGeneration.getTestDataset() != null) {
                    modelGenerationVO.setTestDataset(getDVById(datasetVersionVOS, currentModelGeneration.getTestDataset()));
                }
                return modelGenerationVO;
            }).collect(Collectors.toList());
        } else {
            modelGenerationVOS = content.stream().map(ModelGenerationVO::new).collect(Collectors.toList());
        }
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelGenerationVOS, pageable, queryPage.getTotalElements()));
    }

    /**
     * 创建生产任务，切分的字段使用 splitSize
     *
     * @param modelGeneration
     * @return
     */
    @RequestMapping("/addModelGeneration")
    @SystemControllerLog(description = "model_generation_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    @ApiOperation(value = "新建生产任务", notes = "新建生产任务的同时，把选择的算法的绑定状态修改")
    public Msg<Long> addModelGeneration(@ApiParam("生产任务实体") @RequestBody ModelGeneration modelGeneration) {
        log.info("param[modelGeneration]：{}", modelGeneration.toString());
        if (!modelGenerationService.isModelGenerationExist(modelGeneration)) {
            if (modelGeneration.getModelExplore() != null) {
                // 获得生产任务中对应的算法的实体
                ModelExplore modelExplore = modelExploreService.getModelExploreById(modelGeneration.getModelExplore().getId());
                modelExplore.setIsBoundWithGeneration(ModelExplore.BOUND);
                // 从训练模型版本中获取标注类型和标注格式并设置到生产任务中
                if (modelExplore.getTrainModelVersion() != null) {
                    applyAnnotationInfo(modelGeneration, modelExplore.getTrainModelVersion());
                }
                // 更新算法的绑定状态
                modelExploreService.saveModel(modelExplore);
            } else if (modelGeneration.getModel() != null) {
                Model model = modelService.getModelById(modelGeneration.getModel().getId());
                if (model != null && model.getTrainModelVersion() != null) {
                    applyAnnotationInfo(modelGeneration, model.getTrainModelVersion());
                }
            }
            modelGenerationService.saveModelGeneration(modelGeneration);
            return new Msg<>(MsgCode.SUCCEED, modelGeneration.getId());
        } else {
            return new Msg<>(MsgCode.MODEL_GENERATION_NAME_EXIST);
        }
    }

    private void applyAnnotationInfo(ModelGeneration modelGeneration, ModelVersion trainModelVersion) {
        String annotationType = trainModelVersion.getAnnotationType();
        String annotationFormat = trainModelVersion.getAnnotationFormat();
        if (annotationType != null) {
            modelGeneration.setAnnotationType(annotationType);
        }
        if (annotationFormat != null) {
            modelGeneration.setAnnotationFormat(annotationFormat);
        }
    }

    @GetMapping("/dynamicFindPublishingModelGenerationPage")
    public Msg<Page<ModelGenerationVO>> dynamicFindPublishingModelGenerationPage(@RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "desc") String order,
                                                                                 @RequestParam(defaultValue = "15") int pageSize, @RequestParam(required = false) String vagueInfo, ModelGeneration modelGeneration) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelGeneration> queryPage;
        if (vagueInfo != null) {
            queryPage = modelGenerationService.getVagueModelGenerationPage(pageable, vagueInfo);
        } else {
            queryPage = modelGenerationService.dynamicFindPublishingModelGenerationPage(pageable, modelGeneration);
        }
        List<ModelGeneration> content = queryPage.getContent();
        //获取当前页面所有modelgeneration的traindataset、testdataset的id
        List<Long> idList = new ArrayList<>(content.stream()
                .map(mgTmp -> Arrays.asList(mgTmp.getTrainDataset(), mgTmp.getTestDataset()))
                .reduce(new ArrayList<>(), (x, y) -> {
                    ArrayList<Long> list = new ArrayList<>(x);
                    list.addAll(y);
                    return list;
                }));
        List<ModelGenerationVO> modelGenerationVOS;
        // idList size为0 说明不用发出请求查找版本  但不能说明 没有generation
        if (idList.size() != 0) {
            List<DatasetVersionVO> datasetVersionVOS = modelDatasetService.findDatasetVersionsByIds(idList);
            modelGenerationVOS = queryPage.getContent().stream().map(currentModelGeneration -> {
                ModelGenerationVO modelGenerationVO = new ModelGenerationVO(currentModelGeneration);
                modelGenerationVO.setTrainDataset(getDVById(datasetVersionVOS, currentModelGeneration.getTrainDataset()));
                modelGenerationVO.setTestDataset(getDVById(datasetVersionVOS, currentModelGeneration.getTestDataset()));
                return modelGenerationVO;
            }).collect(Collectors.toList());
        } else {
            modelGenerationVOS = queryPage.getContent().stream().map(currentModelGeneration -> {
                ModelGenerationVO modelGenerationVO = new ModelGenerationVO(currentModelGeneration);
                return modelGenerationVO;
            }).collect(Collectors.toList());
        }
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelGenerationVOS, pageable, queryPage.getTotalElements()));
    }

    // 开始引导式
    @PostMapping("/startGuidedGeneration")
    public Msg<Long> startGuidedGeneration(@RequestBody GuidedGenerationStartDTO guidedGenerationStartDTO) {
        return modelGenerationService.startGuidedGeneration(guidedGenerationStartDTO);
    }

    // 开始fork引导式
    @PostMapping("/startForkGuidedGeneration")
    public Msg<Long> startForkGuidedGeneration(@RequestBody GuidedGenerationStartDTO guidedGenerationStartDTO) {
        return modelGenerationService.startForkGuidedGeneration(guidedGenerationStartDTO);
    }

    // 引导式参数和开始训练
    @PostMapping("/startGuidedGenerationParams")
    public Msg<Long> startGuidedGenerationParams(@RequestBody GuidedGenerationStartDTO guidedGenerationStartDTO) {
        // System.out.println("guidedGenerationStartDTO " + guidedGenerationStartDTO.toString());
        return modelGenerationService.startGuidedGenerationParams(guidedGenerationStartDTO);
    }


    /**
     * 编辑modelGeneration
     */
    @RequestMapping("/updateModelGeneration")
    public Msg<String> updateModelGeneration(@RequestBody ModelGeneration modelGeneration) {
        ModelGeneration mg = modelGenerationService.getModelGenerationById(modelGeneration.getId());
        mg.setName(modelGeneration.getName());
        mg.setDescription(modelGeneration.getDescription());
        mg.setId(modelGeneration.getId());
        mg.setSplitSize(modelGeneration.getSplitSize());
        mg.setSelectedLabels(modelGeneration.getSelectedLabels());
        // 添加GPU模式和GPU数量的更新
        mg.setGpuMode(modelGeneration.getGpuMode());
        mg.setGpuCount(modelGeneration.getGpuCount());
        Msg<String> result = modelGenerationService.updateModelGeneration(mg);
        if (MsgCode.SUCCEED.getCode().equals(result.getCode())) {
            saveModelGenerationUpdateAuditLog(mg);
        }
        return result;
    }

    /**
     * 按照 id 删除 modelGeneration
     *
     * @param modelGenerationId
     * @return
     */
    @RequestMapping("/deleteModelGeneration")
    public Msg<String> deleteModelGeneration(@RequestParam Long modelGenerationId) {
        saveDeleteModelGenerationAuditLog(modelGenerationId);
        return new Msg<>(modelGenerationService.deleteModelGeneration(modelGenerationId));
    }

    private void saveDeleteModelGenerationAuditLog(Long modelGenerationId) {
        UserContext userContext = UserContextHolder.getUserContext();
        RequestInfoContext requestInfoContext = RequestInfoContextHolder.getRequestInfoContext();
        if (userContext == null || requestInfoContext == null) {
            return;
        }

        ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
        if (modelGeneration == null) {
            return;
        }

        AuditLogModel auditLogModel = new AuditLogModel();
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setOperationType(AuditLogConstants.OperationType.DELETE);
        auditLogModel.setUid(userContext.getId());
        auditLogModel.setUname(userContext.getUsername());
        auditLogModel.setCreateDate(new Date());
        auditLogModel.setIp(requestInfoContext.getIpAddress());
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri());
        auditLogModel.setMethod(requestInfoContext.getRequestMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(Boolean.TRUE.equals(modelGeneration.getIsGuided())
                ? "guided_model_generation_delete"
                : "standard_model_generation_delete");
        auditLogModel.setParams("  modelGenerationId: " + modelGenerationId);

        auditLogService.saveAuditLog(auditLogModel);
    }

    private void saveModelGenerationUpdateAuditLog(ModelGeneration modelGeneration) {
        UserContext userContext = UserContextHolder.getUserContext();
        RequestInfoContext requestInfoContext = RequestInfoContextHolder.getRequestInfoContext();
        if (userContext == null || requestInfoContext == null || modelGeneration == null) {
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
        auditLogModel.setDescription(Boolean.TRUE.equals(modelGeneration.getIsGuided())
                ? "guided_model_generation_update"
                : "standard_model_generation_update");
        auditLogModel.setParams("  modelGenerationId: " + modelGeneration.getId()
                + "  name: " + modelGeneration.getName());

        auditLogService.saveAuditLog(auditLogModel);
    }

    /**
     * 按照 name 删除 modelGeneration
     *
     * @param name
     * @return
     */
    @RequestMapping("/findModelGenerationByName")
    public Msg<ModelGeneration> findModelGenerationByName(@RequestParam String name) {
        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationByName(name);
        return new Msg<>(MsgCode.SUCCEED, modelGeneration);
    }

    /**
     * 按照生产任务的 id 获取生产任务的信息
     *
     * @param id
     * @return
     */
    @RequestMapping("/findModelGenerationById")
    @ApiOperation(value = "按照MGId获取，获取MG信息", notes = "通过ModelGenerationId获取ModelGeneration实体")
    public Msg<ModelGenerationVO> findModelGenerationById(@ApiParam("生产任务id") @RequestParam Long id) {
        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(id);
        ModelGenerationVO modelGenerationVO = new ModelGenerationVO(modelGeneration);
        //确保均不为null再去查询
        if(modelGeneration.getTrainDataset() != null && !modelGeneration.getTrainDataset().equals(modelGeneration.getTrainDatasetStart())){
            modelGenerationVO.setTrainDataset(modelDatasetService.selectDatasetVersionById(modelGeneration.getTrainDataset()));
        }
        if(modelGeneration.getTestDataset() != null) {
            modelGenerationVO.setTestDataset(modelDatasetService.selectDatasetVersionById(modelGeneration.getTestDataset()));
        }
        if(modelGeneration.getValDataset() != null) {
            modelGenerationVO.setValDataset(modelDatasetService.selectDatasetVersionById(modelGeneration.getValDataset()));
        }
        return new Msg<>(MsgCode.SUCCEED, modelGenerationVO);
    }

    @GetMapping("/getLatestJobIdByGenerationId")
    @ApiOperation(value = "获取最新的子任务id")
    public Msg<Long> getLatestJobIdByGenerationId(@ApiParam("生产任务id") @RequestParam Long modelGenerationId) {
        log.info("modelGenerationId: {}", modelGenerationId);
        return new Msg<>(MsgCode.SUCCEED, modelGenerationService.findModelGenerationById(modelGenerationId).getLatestJobId());
    }

    /**
     * 发布模型到基础算法库
     *
     * @param modelGenerationId
     * @param bestWeightPath
     * @return
     */
    @RequestMapping("/generationRelease")
    @ApiOperation(value = "直接发布到基础算法库中", notes = "通过 ModelGeneration -> ModelExplore -> ModelVersion -> Chip")
    public Msg<String> generationRelease(@ApiParam("生产任务id") @RequestParam Long modelGenerationId,
                                         @ApiParam("子任务id") @RequestParam Long modelJobId,
                                         @ApiParam("最优训练权重") @RequestParam String bestWeightPath,
                                         @ApiParam("算法名称") @RequestParam String modelName,
                                         @ApiParam("算法描述") @RequestParam String description) {
        log.info("modelGenerationId: {}, modelJobId: {}, bestWeightPath: {}, modelName: {}, description: {}", modelGenerationId, modelJobId, bestWeightPath, modelName, description);
        return modelGenerationService.publishModel2BaseAlgorithmLibAndSaveParameters(modelGenerationId,
                modelJobId,
                bestWeightPath,
                modelName,
                description) ? new Msg<>(MsgCode.SUCCEED) : new Msg<>(MsgCode.FAILED);
    }

    @RequestMapping("/rejectPublishModelGeneration")
    public Msg<String> rejectPublishModelGeneration(@RequestParam long modelGenerationId) {
        ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
        modelGenerationService.saveModelGeneration(modelGeneration);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/bindDataset")
    public Msg<String> bindDataset(@RequestBody JSONObject modelGeneration) {

        ModelGeneration mg = modelGenerationService.getModelGenerationById(modelGeneration.getLong("id"));


        Long trainDataset = modelGeneration.getLong("trainDataset");
        //获取对应的trainDataset
        DatasetVersionVO datasetVersionByFullName = modelDatasetService.selectDatasetVersionById(trainDataset);

        if (datasetVersionByFullName == null) {
            return new Msg<>(MsgCode.FAILED, "没有对应的数据集");
        }
        int datasetSource = modelGeneration.getIntValue("datasetSource");

        //如果是采取切分方法处理数据集
        if (datasetSource == ModelGeneration.FROM_TRAIN_DATASET) {
            mg.setSplitSize(modelGeneration.getString("splitSize"));
        } else {
            Long testDataset = modelGeneration.getLong("testDataset");
            Long valDataset = modelGeneration.getLong("valDataset");
            DatasetVersionVO testDatasetVersion = modelDatasetService.selectDatasetVersionById(testDataset);
            DatasetVersionVO valDatasetVersion = modelDatasetService.selectDatasetVersionById(valDataset);
            if (testDatasetVersion == null || valDatasetVersion == null) {
                return new Msg<>(MsgCode.FAILED, "没有对应的数据集");
            }
            mg.setSplitSize(null);
            mg.setTestDataset(testDatasetVersion.getId());
            mg.setValDataset(valDatasetVersion.getId());
        }

        mg.setTrainDataset(datasetVersionByFullName.getId());
        mg.setDatasetSource(datasetSource);

        Msg<String> result = modelGenerationService.updateModelGeneration(mg);
        if (MsgCode.SUCCEED.getCode().equals(result.getCode())) {
            saveModelGenerationUpdateAuditLog(mg);
        }
        return result;
    }

    @RequestMapping("/findDatasetVersionByFullName")
    public DatasetVersionVO findDatasetVersionByFullName(String fullName) {
        int index = fullName.lastIndexOf(Constant.DATASET_VERSION_PREFIX);
        String datasetName = fullName.substring(0, index).trim();
        String versionName = fullName.substring(index).trim();
        return modelDatasetService.getDatasetVersionByNameAndVersion(datasetName, versionName);
    }

    //  /**
    //   * 根据生产任务的id获取对应训练作业的id
    //   *
    //   * @param modelGenerationId 生产任务id
    //   * @return
    //   */
    // @RequestMapping("/getTrainJobId")
    // public Msg<Long> getTrainJobId(@RequestParam long modelGenerationId) {
    //     ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
    //     long trainJobId = modelGeneration.getTrainJob().getId();
    //     return new Msg<>(MsgCode.SUCCEED, trainJobId);
    // }


    /**
     * 存储某个job的训练日志为文件
     *
     * @param modelGenerationId
     * @param limit
     * @param start
     * @param end
     * @return
     */
    // @RequestMapping("/getTrainLogPath")
    // public Msg<String> getTrainLogPath(long modelGenerationId, Long limit, Long start, Long end) {
    //     ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
    //     ModelJob modelJob = modelGeneration.getTrainJob();
    //     if (modelJob == null) {
    //         return new Msg<>(MsgCode.TRAIN_JOB_NOT_FOUND);
    //     }
    //     String filePath = "log" + "/" + modelGeneration.getId() + "/" + modelJob.getLogFilePath();
    //     if (logService.jobLogStorage(namespace, modelJob.getName(), limit, start, end, datasetRootPath + "/" + filePath)) {
    //         return new Msg<>(MsgCode.SUCCEED, filePath);
    //     }
    //     return new Msg<>(MsgCode.FAILED);
    // }
//    @RequestMapping("/testJobLogStorage")
//    public Msg<String> testJobLogStorage(long modelGenerationId, Long limit, Long start, Long end) {
//        ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
//        // ModelJob testJob = modelGeneration.getTestJob();
//        // ModelJob anotherTestJob = modelGeneration.getAnotherTestJob();
//        ModelJob testJob = new ModelJob();
//        ModelJob anotherTestJob = new ModelJob();
//        if (testJob == null) {
//            return new Msg<>(MsgCode.TEST_JOB_NOT_FOUND);
//        }
//        String filePath;
//        if (anotherTestJob == null) {
//            filePath = "testLog" + "/" + modelGeneration.getId() + "/" + testJob.getName() + ".log";
//            logService.testJobLogStorage(namespace, testJob.getName(), null, limit, start, end, datasetRootPath + "/" + filePath);
//        } else {
//            filePath = "testLog" + "/" + modelGeneration.getId() + "/" + testJob.getName() + "---" + anotherTestJob.getName() + ".log";
//            logService.testJobLogStorage(namespace, testJob.getName(), anotherTestJob.getName(), limit, start, end, datasetRootPath + "/" + filePath);
//        }
//        return new Msg<>(MsgCode.SUCCEED, filePath);
//    }

    /**
     * 获取任务的权重文件
     *
     * @param modelGenerationId
     * @return
     */
    @RequestMapping("/getWeightPath")
    public Msg<List<String>> getWeightPath(long modelGenerationId) {
        ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
        File weightDir = new File(datasetRootPath + "/" + weightPath + "/" + modelGeneration.getTrainModelVersion().getWeightPath());
        if (!weightDir.isDirectory()) {
            return new Msg<>(MsgCode.FAILED);
        }
        File[] files = weightDir.listFiles();
        if (files == null) {
            return new Msg<>(MsgCode.FAILED);
        } else {
            List<String> weightFiles = Arrays.stream(files).map(file -> weightPath + "/" + modelGeneration.getTrainModelVersion().getWeightPath() + "/" + file.getName()).collect(Collectors.toList());
            weightFiles.forEach(System.out::println);
            return new Msg<>(MsgCode.SUCCEED, weightFiles);
        }
    }

    /**
     * 获取所有的硬件信息
     *
     * @return
     */
    @RequestMapping("/findAllHardwareParams")
    public Msg<?> findAllHardwareParams(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "isDefault", required = false) Integer isDefault) {

        Long currentUserId = Long.valueOf(UserContextHolder.getUserContext().getId());

        // 如果提供了isDefault参数，则按isDefault字段查询
        if (isDefault != null) {
            if (page != null && size != null) {
                // 有分页参数时返回分页数据
                Pageable pageable = PageRequest.of(page, size);
                Page<HardwareParams> pageInfo = hardwareParamsService.findHardwareParamsPageByIsDefault(currentUserId, isDefault, pageable);
                return new Msg<>(MsgCode.SUCCEED, pageInfo);
            } else {
                // 无分页参数时返回全部数据 (这种情况下不太可能用到，但为了完整性保留)
                // 这里我们仍然按分页方式处理，但返回所有数据
                Pageable pageable = PageRequest.of(0, 1000); // 假设最多有1000条数据
                Page<HardwareParams> pageInfo = hardwareParamsService.findHardwareParamsPageByIsDefault(currentUserId, isDefault, pageable);
                return new Msg<>(MsgCode.SUCCEED, pageInfo);
            }
        }

        // 原有逻辑，不区分isDefault字段
        if (page != null && size != null) {
            // 有分页参数时返回分页数据
            Pageable pageable = PageRequest.of(page, size);
            Page<HardwareParams> pageInfo = hardwareParamsService.findHardwareParamsPage(currentUserId, pageable);
            return new Msg<>(MsgCode.SUCCEED, pageInfo);
        } else {
            // 无分页参数时返回全部数据
            List<HardwareParams> allData = hardwareParamsService.findAllHardwareParams(currentUserId);
            return new Msg<>(MsgCode.SUCCEED, allData);
        }
    }

    @DeleteMapping("/deleteHardwareParamsBatch")
    @ApiOperation(value = "批量删除硬件参数配置")
    public Msg<String> deleteHardwareParamsBatch(@ApiParam("硬件参数ID列表") @RequestBody List<Long> ids) {
        try {
            Map<String, Object> result = hardwareParamsService.deleteHardwareParamsBatch(ids);
            int deletedCount = (int) result.get("deletedCount");

            @SuppressWarnings("unchecked")
            List<String> referencedNames = (List<String>) result.get("referencedNames");

            String message = referencedNames.isEmpty()
                    ? String.format("成功删除 %d 条记录", deletedCount)
                    : String.format("成功删除 %d 条，以下配置被引用无法删除: %s",
                    deletedCount, String.join(", ", referencedNames));

            return new Msg<>(MsgCode.SUCCEED, message);

        } catch (BusinessException e) {
            return new Msg<>(MsgCode.FAILED, e.getMessage());
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED, "批量删除失败，请稍后重试");
        }
    }

    /**
     * 生产任务绑定硬件信息
     *
     * @param modelGenerationId 生产任务id
     * @param hardwareParamsId  硬件训练任务id
     * @return
     */
    @RequestMapping("/bindHardwareParams")
    public Msg<String> bindHardwareParams(@RequestParam long modelGenerationId, @RequestParam long hardwareParamsId) {
        return modelGenerationService.bindHardwareParams(modelGenerationId, hardwareParamsId);
    }

    @RequestMapping("/findGeneraByJob")
    public Msg<String> findGeneraByJob(@RequestParam long modelJobId) {
        return new Msg<>(MsgCode.SUCCEED, modelGenerationService.findGeneraByJob(modelJobService.getModelJobById(modelJobId)).getName());
    }

    @RequestMapping("/getMGStatus")
    public Msg<Map<Long, Integer>> getMGStatus(@RequestParam List<Long> mgIds) {
        return new Msg<>(MsgCode.SUCCEED, modelGenerationService.getModelGenerationStatusById(mgIds));
    }

    @RequestMapping("/saveModelGenerationTrainTime")
    public Msg<String> saveModelGenerationTrainTime(@RequestBody JSONObject mdJson) {
        JSONObject md = mdJson.getJSONObject("trainTimeValue");
        long modelGenerationId = md.getLong("modelGenerationId");
        int delayTrainTime = md.getIntValue("delayTrainTime");
        int maxTrainTime = md.getIntValue("maxTrainTime");
        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(modelGenerationId);
        modelGeneration.setDelayTrainTime(delayTrainTime);
        modelGeneration.setMaxTrainTime(maxTrainTime);
        modelGenerationService.saveModelGeneration(modelGeneration);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/setTrainParams")
    public Msg<String> setTrainParams(@RequestBody JSONObject jobJson) {
        long modelGenerationId = jobJson.getLong("modelGenerationId");
        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(modelGenerationId);
        JSONObject params = jobJson.getJSONObject("params");
        Map<String, String> generationParams = JSONObject.parseObject(params.toJSONString(), new TypeReference<Map<String, String>>() {
        });
        modelGeneration.setHyperParams(generationParams);
        modelGenerationService.saveModelGeneration(modelGeneration);
        return new Msg<>(MsgCode.SUCCEED);
    }

    /**
     * 用于给引导任务发布训练任务的模型和权重
     */
    @RequestMapping("/publishGuideGeneration")
    public Msg<String> publishGuideGeneration(@RequestParam long generationId) {

        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(generationId);
        Model storeModel = modelGeneration.getModel();
        ModelJob latestJob = modelJobService.getModelJobById(modelGeneration.getLatestJobId());
        storeModel.setTrainModelJob(latestJob);
        storeModel.setBestWeightPath(latestJob.getWeightPath());
        modelService.saveModel(storeModel);
        //modelGeneration.setStatus(ModelGeneration.PUBLISHED);
        modelGenerationService.saveModelGeneration(modelGeneration);
        return new Msg<>(MsgCode.SUCCEED);
    }

    /**
     * 用于给引导任务发布训练任务的模型和权重
     */
    @RequestMapping("/completeGuideGeneration")
    public Msg<String> completeGuideGeneration(@RequestParam long generationId) {
        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(generationId);
        //modelGeneration.setStatus(ModelGeneration.COMPLETED);
        modelGenerationService.saveModelGeneration(modelGeneration);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @PutMapping("/datasetCut")
    @ApiOperation(value = "数据集切分", notes = "按照切分比例进行数据集的切分")
    public Msg<String> datasetCut(@RequestParam("cutRadio") String radio, @RequestParam("modelGenerationId") long modelGenerationId) {
        ModelGeneration modelGenerationById = modelGenerationService.findModelGenerationById(modelGenerationId);
        long latestJobId = modelGenerationById.getLatestJobId();
        modelDatasetService.datasetCut(radio, modelGenerationId,latestJobId);
        return new Msg<>(MsgCode.SUCCEED);
    }

    /**
     * 获取所有的 Model Generation 信息
     *
     * @return
     */
    @GetMapping("/getAllModelGeneration")
    @ApiOperation(value = "获取所有 ModelGeneration", notes = "获取所有状态的 ModelGeneration")
    public Msg<List<HashMap<String, Object>>> getAllModelGeneration() {
        List<HashMap<String, Object>> result = modelGenerationService.getAllModelGeneration();
        return new Msg<>(MsgCode.SUCCEED, result);
    }

    /**
     * 获取 ModelGeneration 下的所有的 ModelJob 的 weightpath，过滤掉了转换的
     *
     * @param modelGenerationId
     * @return
     */
    @GetMapping("/preWeights/{modelGenerationId}")
    @ApiOperation(value = "获取 ModelGeneration 下的所有 ModelJob 的 BestWeight")
    public Msg<Map<Long, String>> getPreWeight(@PathVariable Long modelGenerationId) {
        Map<Long, String> result = modelJobService.getModelJob().stream()
                .filter(modelJob -> Objects.equals(modelJob.getModelGenerationId(), modelGenerationId) && !modelJob.getWeightPath().contains("convert"))
                .collect(Collectors.toMap(
                        ModelJob::getId,
                        ModelJob::getWeightPath
                ));
        return new Msg<>(MsgCode.SUCCEED, result);
    }

    /**
     * 根据一个完成的引导训练的新起一个完全一样的引导训练
     */
    @PostMapping("/createGuidedFork")
    @ApiOperation(value = "根据一个完成的引导训练的新起一个完全一样的引导训练")
    public Msg<String> createGuidedFork(@RequestParam("modelGenerationId") long modelGenerationId) {

        return modelGenerationService.createGuidedFork(modelGenerationId);
    }

//    @GetMapping("/findGuidedModelGenerationSupportAutoLabel")
//    @ApiOperation(value = "获取所有训练镜像支持自动标注的引导训练")
//    public Msg<List<ModelGeneration>> findGuidedModelGenerationSupportAutoLabel() {
//        List<ModelGeneration> modelGenerations = modelGenerationService.findGuidedModelGenerationSupportAutoLabel();
//        return new Msg<>(MsgCode.SUCCEED, modelGenerations);
//    }

    @GetMapping("/findGuidedModelGenerationSupportAutoLabelByLabels/{dataSetId}")
    @ApiOperation(value = "获取所有通过labels筛选训练镜像支持自动标注的引导训练")
    public Msg<List<ModelGeneration>> findGuidedModelGenerationSupportAutoLabelByLabels(@PathVariable Long dataSetId) {
        List<ModelGeneration> modelGenerations = modelGenerationService.findGuidedModelGenerationSupportAutoLabel(dataSetId);
        return new Msg<>(MsgCode.SUCCEED, modelGenerations);
    }

    @GetMapping("/findModelGenerationSupportAutoLabel")
    @ApiOperation(value = "获取所有训练镜像支持自动标注的标准化训练")
    public Msg<List<ModelGeneration>> findModelGenerationSupportAutoLabel(
            @RequestParam(required = false) Long datasetId) {
        List<ModelGeneration> modelGenerations = modelGenerationService.findModelGenerationSupportAutoLabel(datasetId);
        return new Msg<>(MsgCode.SUCCEED, modelGenerations);
    }

    @GetMapping("/findModelGenerationSupportAutoLabelByApplication")
    @ApiOperation(value = "根据应用固件获取支持首轮自动标注的标准化训练")
    public Msg<List<ModelGeneration>> findModelGenerationSupportAutoLabelByApplication(
            @RequestParam String applicationName,
            @RequestParam String deviceFirmware) {
        List<ModelGeneration> modelGenerations = modelGenerationService.findModelGenerationSupportAutoLabelByApplication(applicationName, deviceFirmware);
        return new Msg<>(MsgCode.SUCCEED, modelGenerations);
    }

    @GetMapping("/findModelJobByModelGenerationId")
    @ApiOperation(value = "根据ModelGenerationId获取ModelJob")
    public Msg<List<ModelJob>> findModelJobByModelGenerationId(@RequestParam("modelGenerationId") long modelGenerationId) {
        List<ModelJob> modelJobs = modelJobService.findModelJobsByModelGenerationId(modelGenerationId)
                .stream()
                .filter(modelJob -> modelJob.getJobType() == ModelJob.TRAIN && modelJob.getStatus() >= 2)
                .collect(Collectors.toList());
        return new Msg<>(MsgCode.SUCCEED, modelJobs);
    }

    @GetMapping("/generateGuidedModelGenerationNameByApplicationName")
    @ApiOperation(value = "根据应用名生成新的引导式训练名（应用名-1，应用名-2……）")
    public Msg<String> generateGuidedModelGenerationNameByApplicationName(@RequestParam("applicationName") String applicationName) {

        String latestName = modelGenerationService.generateGuidedModelGenerationNameByApplicationName(applicationName);
        return new Msg<>(MsgCode.SUCCEED,latestName);
    }

    @GetMapping("/getVersionLabelsByModelGenerationId/{modelGenerationId}")
    @ApiOperation(value = "根据ModelGenerationId获取VersionLabels")
    public Msg<List<LabelDTO>> getVersionLabelsByModelGenerationId(@PathVariable("modelGenerationId") Long modelGenerationId) {
        ModelGeneration generation = modelGenerationService.getModelGenerationById(modelGenerationId);
        List<String> labelNames = new ArrayList<>(generation.getSelectedLabels().values());
        List<LabelDTO> labelDTOS = new ArrayList<>();
        for (String labelName : labelNames) {
            labelDTOS.add(new LabelDTO(labelName));
        }
        return new Msg<>(MsgCode.SUCCEED, labelDTOS);
    }

    @GetMapping(value =  "/getVersionLabelsByJobName")
    @ApiOperation(value = "根据模型任务名称获取模型任务标签")
    public Msg<List<LabelDTO>> getVersionLabelsByJobName(@RequestParam("jobName") String jobName) {
        ModelJob modelJob = modelJobService.findModelJobByName(jobName);
        if (modelJob == null || modelJob.getTrainDatasetVersionId() == null) {
            return new Msg<>(MsgCode.FAILED);
        }
        List<LabelDTO> versionLabels = dubheDataFeign.getDatasetVersionLabelsByVersionId(dubheUtils.getAuthorization(), modelJob.getTrainDatasetVersionId()).getData();
        return new Msg<>(MsgCode.SUCCEED, versionLabels);
    }

    @PostMapping("checkDatasetBindRelationWithGeneration")
    @ApiOperation(value = "检查数据集和训练任务的绑定关系")
    public Msg<List<ModelGenerationBindInfo>> checkDatasetBindRelationWithGeneration(@RequestBody DatasetDeleteDTO datasetDeleteDTO) {
        List<ModelGenerationBindInfo> result = modelGenerationService.checkBindRelationWithGeneration(datasetDeleteDTO.getIds());
        return new Msg<>(MsgCode.SUCCEED, result);
    }

    @GetMapping(value =  "/getJobAccLossAndImgCountsByJobName")
    @ApiOperation(value = "根据job名获取相关信息")
    public Msg<JobPrecisionInfoDTO> getJobAccLossAndImgCountsByJobName(@RequestParam("jobName") String jobName) {
        return modelJobService.getJobAccLossAndImgCountsByJobName(jobName);
    }
    @GetMapping(value =  "/getJobAccLossAndImgCountsByGuidedGenerationId")
    @ApiOperation(value = "根据generationId获取相关信息(仅引导式用)")
    public Msg<JobPrecisionInfoDTO> getJobAccLossAndImgCountsByGuidedGenerationId(@RequestParam("modelGenerationId") Long modelGenerationId) {
        return modelJobService.getJobAccLossAndImgCountsByGuidedGenerationId(modelGenerationId, true);
    }

    @GetMapping("/findModelJobInfoCardsByGenerationId")
    @ApiOperation(value = "分页获取标准化训练任务的详细信息卡片")
    public Msg<Page<ModelJobInfoCardDTO>> findModelJobInfoCardsByGenerationId(
            @RequestParam Long modelGenerationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int pageSize,
            @RequestParam(required = false) String labels) { // 每页默认返回2条，方便前端展示
        Page<ModelJobInfoCardDTO> resultPage = modelJobService.findModelJobInfoCards(modelGenerationId, page, pageSize, labels);
        return new Msg<>(MsgCode.SUCCEED, resultPage);
    }

    @GetMapping("/getStandardAutoLabelNames/{modelGenerationId}")
    @ApiOperation(value = "获取标准化任务自动标注的所有标签名")
    public Msg<List<String>> getStandardAutoLabelNames(@PathVariable Long modelGenerationId) {
        try {
            List<String> labelNames = modelJobService.getLabelNamesByModelGenerationId(modelGenerationId);
            return new Msg<>(MsgCode.SUCCEED, labelNames);
        } catch (Exception e) {
            log.error("findStandardTrainingLabelsByModelGenerationId error: {}", e.getMessage());
            return new Msg<>(MsgCode.SUCCEED, Collections.emptyList() );
        }
    }

    @GetMapping("/findGuidedTrainingInfoCardsByDatasetId/{dataSetId}")
    @ApiOperation(value = "分页获取引导式训练的详细信息卡片")
    public Msg<Page<GuidedTrainingInfoCardDTO>> findGuidedTrainingInfoCardsByDatasetId(
            @PathVariable Long dataSetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String labels) {
        try {
            Page<GuidedTrainingInfoCardDTO> resultPage = modelGenerationService.findGuidedTrainingInfoCards(dataSetId, page, pageSize, name, labels);
            return new Msg<>(MsgCode.SUCCEED, resultPage);
        } catch (Exception e) {
            log.error("findGuidedTrainingInfoCardsByDatasetId error: {}", e.getMessage());
            Page<GuidedTrainingInfoCardDTO> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(page, pageSize),
                    0);
            return new Msg<>(MsgCode.SUCCEED, emptyPage);
        }
    }

    @GetMapping("/findGuidedTrainingInfoCardsByApplication")
    @ApiOperation(value = "根据应用固件分页获取可用于自迭代首轮自动标注的引导式训练卡片")
    public Msg<Page<GuidedTrainingInfoCardDTO>> findGuidedTrainingInfoCardsByApplication(
            @RequestParam String applicationName,
            @RequestParam String deviceFirmware,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String labels) {
        try {
            Page<GuidedTrainingInfoCardDTO> resultPage = modelGenerationService.findGuidedTrainingInfoCardsByApplication(
                    applicationName, deviceFirmware, page, pageSize, name, labels);
            return new Msg<>(MsgCode.SUCCEED, resultPage);
        } catch (Exception e) {
            log.error("findGuidedTrainingInfoCardsByApplication error: {}", e.getMessage());
            Page<GuidedTrainingInfoCardDTO> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(page, pageSize),
                    0);
            return new Msg<>(MsgCode.SUCCEED, emptyPage);
        }
    }

    @GetMapping("/getAutoLabelNames/{dataSetId}")
    @ApiOperation(value = "获取引导式任务自动标注的所有标签名")
    public Msg<List<String>> getAutoLabelNames(@PathVariable Long dataSetId) {
        try {
            List<String> labelNames = modelGenerationService.getLabelNamesByDataSetId(dataSetId);
            return new Msg<>(MsgCode.SUCCEED, labelNames);
        } catch (Exception e) {
            log.error("findGuidedTrainingLabelsByDatasetId error: {}", e.getMessage());
            return new Msg<>(MsgCode.SUCCEED, Collections.emptyList() );
        }
    }

    @PostMapping("/bindDatasetVersions")
    @ApiOperation(value = "绑定数据集版本")
    public Msg<String> bindDatasetVersions(@RequestBody BindVersionsRequestDTO request) {
        modelGenerationService.bindDatasetVersions(request.getDatasetVersionIds(), request.getModelGenerationId());
        return new Msg<>(MsgCode.SUCCEED);
    }

    @GetMapping("/getModelGenerationBoundVersions")
    @ApiOperation(value = "获取模型生成任务绑定的版本等信息(含有算法包标签筛选逻辑，引导式训练使用）")
    public Msg<List<ModelGenerationBoundVersionDTO>> getModelGenerationBoundVersions(@RequestParam Long modelGenerationId) {
        List<ModelGenerationBoundVersionDTO> result = modelGenerationService.getModelGenerationBoundVersions(modelGenerationId);
        return new Msg<>(MsgCode.SUCCEED, result);
    }

    @GetMapping("/getModelGenerationBoundVersionsWithoutLabelFilter")
    @ApiOperation(value = "获取模型生成任务绑定的版本等信息（不含引导式的标签筛选逻辑，供标准化训练编辑使用）")
    public Msg<List<ModelGenerationBoundVersionDTO>> getModelGenerationBoundVersionsWithoutLabelFilter(@RequestParam Long modelGenerationId) {
        List<ModelGenerationBoundVersionDTO> result = modelGenerationService.getModelGenerationBoundVersionsWithoutLabelFilter(modelGenerationId);
        return new Msg<>(MsgCode.SUCCEED, result);
    }

    @GetMapping("/getDatasetGroupNameByModelGenerationId")
    @ApiOperation(value = "获取引导式generation所属数据集组名称")
    public Msg<String> getDatasetGroupNameByModelGenerationId(@RequestParam Long modelGenerationId) {
        ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
        if (modelGeneration == null) {
            return new Msg<>(MsgCode.FAILED);
        }
        return new Msg<>(MsgCode.SUCCEED, dubheDataFeign.getDatasetGroupNameByDatasetId(dubheUtils.getAuthorization(), modelGeneration.getTrainDatasetStart()).getData());

    }

    @GetMapping("/getModelGenerationOverview")
    @ApiOperation(value = "获取模型生成任务概览")
    public Msg<ModelGenerationOverviewDTO> getModelGenerationOverview(@RequestParam Long modelGenerationId) {
        return modelGenerationService.getModelGenerationOverview(modelGenerationId);
    }

    @GetMapping("getGuidedGenerationLabels")
    @ApiOperation(value = "获取引导式任务标签")
    public Msg<List<LabelDTO>> getGuidedGenerationLabels(@RequestParam Long modelGenerationId) {
        ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
        if (modelGeneration == null) {
            return new Msg<>(MsgCode.FAILED);
        }
        return new Msg<>(
                MsgCode.SUCCEED,
                dubheDataFeign.findLabelByIds(
                        dubheUtils.getAuthorization(),
                        modelapplicationziplabelService.getBoundLabelIds(
                                modelGeneration.getModelApplication().getId()
                        )
                ).getData()
        );
    }



    @GetMapping("checkDatasetVersionBindRelationWithGeneration")
    @ApiOperation(value = "检查数据集版本和训练任务的绑定关系")
    public Msg<List<ModelGenerationBindInfo>> checkDatasetVersionBindRelationWithGeneration(@RequestParam Long versionID) {
        List<ModelGenerationBindInfo> result =
                modelGenerationService.checkBindRelationWithDatasetVersion(versionID);
        return new Msg<>(MsgCode.SUCCEED, result);
    }

    @GetMapping("/countGenerationWithNoDeleted")
    public Msg<Long> countGenerationWithNoDeleted() {
        return new Msg<>(MsgCode.SUCCEED, modelGenerationService.countGenerationWithNoDeleted());
    }

}
