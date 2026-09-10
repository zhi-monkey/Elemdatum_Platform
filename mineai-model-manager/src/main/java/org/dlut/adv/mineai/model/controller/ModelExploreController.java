package org.dlut.adv.mineai.model.controller;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dto.ModelDTO;
import org.dlut.adv.mineai.model.dto.ModelGenerationDTO;
import org.dlut.adv.mineai.model.dto.UserDTO;
import org.dlut.adv.mineai.model.repository.ModelGenerationRepo;
import org.dlut.adv.mineai.model.service.*;
import org.dlut.adv.mineai.model.service.inter.ModelClassificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/modelExplore")
@Api(tags = "算法请求控制器")
public class ModelExploreController {
    private final ModelGenerationRepo modelGenerationRepo;

    public ModelExploreController(ModelGenerationRepo modelGenerationRepo) {
        this.modelGenerationRepo = modelGenerationRepo;
    }

    @Resource
    private ModelExploreService modelExploreService;

    @Resource
    private ModelMonitorService modelMonitorService;

    @Resource
    private ModelVersionService modelVersionService;

    @Resource
    private ModelGenerationService modelGenerationService;

    @Resource
    private MonitorModelConfigService monitorModelConfigService;

    @Resource
    private ModelDeploymentService modelDeploymentService;

    @Resource
    private ModelUserService modelUserService;

    @Resource
    private ModelService modelService;

    @Resource
    private ModelConfigService modelConfigService;

    @Resource
    private ModelClassificationService modelClassificationService;

    @Value("${zlm.secret}")
    private String secret;

    @Value("${zlm.rtsp.prefix}")
    private String rtspPrefix;

    /**
     * 分页查询未发布算法
     *
     * @param page
     * @param order
     * @param pageSize
     * @param vagueInfo
     * @param model
     * @return
     */
    @GetMapping("/dynamicFindUnReleasedModelPage")
    public Msg<Page<ModelDTO>> dynamicFindUnReleasedModelPage(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "desc") String order,
                                                              @RequestParam(defaultValue = "15") int pageSize,
                                                              @RequestParam(required = false) String vagueInfo,
                                                              @RequestParam(required = false) String userName,
                                                              ModelExplore model) {
        // log.warn(modelGeneration.getUserId().toString());
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelExplore> queryPage;
//        if (vagueInfo != null) {
//            queryPage = modelExploreService.getVagueUnReleasedModelPage(pageable, vagueInfo);
//        } else {
//            queryPage = modelExploreService.dynamicFindUnReleasedModelPage(pageable, model);
//        }
        if (StringUtils.isBlank(vagueInfo)) {
            queryPage = modelExploreService.dynamicFindUnReleasedModelPage(pageable, model, userName);
        } else {
            queryPage = modelExploreService.getVagueUnReleasedModelPage(pageable, vagueInfo);
        }
        List<ModelExplore> content = queryPage.getContent();
        // 获取当前页面所有modelgeneration的traindataset、testdataset的id
        List<Long> idList = new ArrayList<>(content.stream()
                .map(modelTmp -> Arrays.asList(modelTmp.getCreatorId(), modelTmp.getApplicantId()))
                .reduce(new ArrayList<>(), (x, y) -> {
                    ArrayList<Long> list = new ArrayList<>(x);
                    list.addAll(y);
                    return list;
                }));

        List<ModelDTO> modelDTOs;
        // 将 userDTOs 初始化为一个不可变的空列表
        final List<UserDTO> userDTOs;

        // 仅当 idList 不为空时才进行查询
        if (!idList.isEmpty()) {
            userDTOs = modelUserService.findUsersByIds(idList);
        } else {
            // 使用不可变的空列表
            userDTOs = Collections.emptyList();
        }

        modelDTOs = content.stream().map(curModel -> {
            ModelDTO modelDTO = BeanUtil.toBean(curModel, ModelDTO.class);
            modelDTO.setCreator(getUserDTOById(userDTOs, curModel.getCreatorId()));
            modelDTO.setApplicant(getUserDTOById(userDTOs, curModel.getApplicantId()));
            return modelDTO;
        }).collect(Collectors.toList());

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelDTOs, pageable, queryPage.getTotalElements()));
    }


    /**
     * 分页查询未发布算法处于审核状态的算法
     *
     * @param page
     * @param order
     * @param pageSize
     * @param vagueInfo
     * @param model
     * @return
     */
    @GetMapping("/dynamicFindUnderExamineModelPage")
    public Msg<Page<ModelDTO>> dynamicFindUnderExamineModelPage(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "desc") String order,
                                                                @RequestParam(defaultValue = "15") int pageSize,
                                                                @RequestParam(required = false) String vagueInfo,
                                                                ModelExplore model) {
        // log.warn(modelGeneration.getUserId().toString());
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelExplore> queryPage;
        if (vagueInfo != null) {
            queryPage = modelExploreService.getVagueUnderExamineModelPage(pageable, vagueInfo);
        } else {
            queryPage = modelExploreService.dynamicFindUnderExamineModelPage(pageable, model);
        }
        List<ModelExplore> content = queryPage.getContent();
        //获取当前页面所有modelgeneration的traindataset、testdataset的id
        List<Long> idList = new ArrayList<>(content.stream()
                .map(modelTmp -> Arrays.asList(modelTmp.getCreatorId(), modelTmp.getApplicantId()))
                .reduce(new ArrayList<>(), (x, y) -> {
                    ArrayList<Long> list = new ArrayList<>(x);
                    list.addAll(y);
                    return list;
                }));
        List<ModelDTO> modelDTOs;


        List<UserDTO> userDTOs = modelUserService.findUsersByIds(idList);

        modelDTOs = content.stream().map(curModel -> {
            ModelDTO modelDTO = BeanUtil.toBean(curModel, ModelDTO.class);
            modelDTO.setCreator(getUserDTOById(userDTOs, curModel.getCreatorId()));
            modelDTO.setApplicant(getUserDTOById(userDTOs, curModel.getApplicantId()));
            List<ModelGeneration> allByModelId = modelGenerationService.findAllByModelExploreId(curModel.getId());
            if (allByModelId != null && allByModelId.size() > 0) {
                modelDTO.setModelGenerationDTO(BeanUtil.toBean(allByModelId.get(0), ModelGenerationDTO.class));
            }
            return modelDTO;
        }).collect(Collectors.toList());

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelDTOs, pageable, queryPage.getTotalElements()));
    }

    private UserDTO getUserDTOById(List<UserDTO> list, long id) {
        if (list == null) {
            return new UserDTO();
        }
        return list.stream().filter(userDTO -> userDTO.getId() == id).findFirst().orElse(new UserDTO());
    }

    /**
     * 分页查询发布了的算法
     *
     * @param page
     * @param order
     * @param pageSize
     * @param vagueInfo
     * @param model
     * @return
     */
    @GetMapping("/dynamicFindReleasedModelPage")
    public Msg<Page<ModelDTO>> dynamicFindReleasedModelPage(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "desc") String order,
                                                            @RequestParam(defaultValue = "15") int pageSize,
                                                            @RequestParam(required = false) String vagueInfo,
                                                            ModelExplore model) {
        // log.warn(modelGeneration.getUserId().toString());
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<ModelExplore> queryPage;
        if (vagueInfo != null) {
            queryPage = modelExploreService.getVagueReleasedModelPage(pageable, vagueInfo);
        } else {
            queryPage = modelExploreService.dynamicFindReleasedModelPage(pageable, model);
        }
        List<ModelExplore> content = queryPage.getContent();
        //获取当前页面所有modelgeneration的traindataset、testdataset的id
        List<Long> idList = new ArrayList<>(content.stream()
                .map(modelTmp -> Arrays.asList(modelTmp.getCreatorId(), modelTmp.getApplicantId()))
                .reduce(new ArrayList<>(), (x, y) -> {
                    ArrayList<Long> list = new ArrayList<>(x);
                    list.addAll(y);
                    return list;
                }));
        List<ModelDTO> modelDTOs;


        List<UserDTO> userDTOs = modelUserService.findUsersByIds(idList);

        modelDTOs = content.stream().map(curModel -> {
            ModelDTO modelDTO = BeanUtil.toBean(curModel, ModelDTO.class);
            modelDTO.setCreator(getUserDTOById(userDTOs, curModel.getCreatorId()));
            modelDTO.setApplicant(getUserDTOById(userDTOs, curModel.getApplicantId()));
            return modelDTO;
        }).collect(Collectors.toList());

        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelDTOs, pageable, queryPage.getTotalElements()));
    }

//    @GetMapping("/dynamicFindModelPageWithoutEnglishName")
//    public Msg<Page<JSONObject>> dynamicFindModelPageWithoutEnglishName(@RequestParam(defaultValue = "0") int page,
//                                                                        @RequestParam(defaultValue = "asc") String order,
//                                                                        @RequestParam(defaultValue = "15") int pageSize, @RequestParam(required = false) String vagueInfo, ModelExplore model) {
//        String end = "end";
//        if (order.contains(end)) {
//            order = order.substring(0, order.indexOf("end"));
//        }
//        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
//        if (model.getDescription() == null) {
//            model.setDescription("");
//        }
//        if (model.getModelEnglishName() == null) {
//            model.setModelEnglishName("");
//        }
//        if (model.getModelName() == null) {
//            model.setModelName("");
//        }
//        Page<ModelExplore> queryPage;
//        if (vagueInfo != null) {
//            queryPage = modelExploreService.getVagueModelPageWithoutEnglishName(pageable, vagueInfo);
//        } else {
//            queryPage = modelExploreService.dynamicFindModelPage(pageable, model);
//        }
//        List<JSONObject> modelJsonList = queryPage.getContent().stream().map(model1 -> {
//            JSONObject modelJson = JSON.parseObject(JSONObject.toJSONString(model1));
//            return modelJson.fluentPut("canDelete", modelExploreService.isBindSth(model1));
//        }).collect(Collectors.toList());
//        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelJsonList, pageable, queryPage.getTotalElements()));
//    }

    /**
     * 新增 Model
     */
    @RequestMapping("/addModel")
    public Msg<Long> addModel(@RequestBody ModelExplore modelExplore) {
        // 设置默认值
        modelExplore.setModelStatus(ModelExplore.UNRELEASED);
        modelExplore.setReleaseStatus(ModelExplore.NOT_UNDER_EXAMINE);
        modelExplore.setSubsystem(ModelExplore.CENTRAL_PLATFORM);
        modelExplore.setIsBoundWithGeneration(ModelExplore.NOT_BOUND);
        //每个账号自己下面看所拥有的算法有没有重名
        if(modelExploreService.isModelExploreNameExistWithOwnerID(modelExplore)){
           return new Msg<>(MsgCode.MODEL_EXPLORE_NAME_EXISTS,null);
        }
        modelExploreService.saveModel(modelExplore);
        return new Msg<>(MsgCode.SUCCEED, modelExplore.getId());
    }

    /**
     * 编辑Model
     */
    @RequestMapping("/updateModel")
    public Msg<Boolean> updateModel(@RequestBody ModelExplore model) {
        ModelExplore originModel = modelExploreService.findModelExploreById(model.getId());
        //如果改了名字
        //每个账号自己下面看所拥有的算法有没有重名
        if(!(originModel.getModelName().equals(model.getModelName())) && modelExploreService.isModelExploreNameExistWithOwnerID(model)){
            return new Msg<>(MsgCode.MODEL_EXPLORE_NAME_EXISTS,null);
        }
        return new Msg<>(modelExploreService.updateModelExplore(model));
    }

    /**
     * 删除model
     */
    @RequestMapping("/deleteModel")
    public Msg<String> deleteModel(@RequestBody ModelExplore model) {

        modelMonitorService.deleteMonitorAndModel(model.getId());
        boolean status = modelExploreService.deleteModel(model.getId());
        if (status) {
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.DELETE_MODEL_FAILED);
        }
    }

    /**
     * 获得所有 Model
     *
     * @return model list
     */
    @RequestMapping("/getModel")
    public Msg<List<ModelExplore>> getModel() {
        List<ModelExplore> models = modelExploreService.getModelExplore();
        return new Msg<>(MsgCode.SUCCEED, models);
    }

    /**
     * 获取modelName的列表
     */
    @RequestMapping("/getModelNameList")
    public Msg<HashMap<Long, String>> getModelNameList() {
        List<ModelExplore> models = modelExploreService.getModelExplore();
        HashMap<Long, String> modelNameSet = new HashMap<>();
        for (ModelExplore model : models) {
            modelNameSet.put(model.getId(), model.getModelName());
        }
        return new Msg<>(MsgCode.SUCCEED, modelNameSet);
    }

    @DeleteMapping("deleteModelExplore/{modelExploreId}")
    public Msg<Void> deleteModelExplore(@PathVariable("modelExploreId") Long modelExploreId) {
        modelExploreService.deleteModelExplore(modelExploreId);
        return new Msg<>(MsgCode.SUCCEED);
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
    //

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
     * 根据 算法id 返回 算法名称
     *
     * @param id
     * @return
     */
    @RequestMapping("/findModelNameById")
    public Msg<String> findModelNameById(@RequestParam long id) {
        String modelName = modelExploreService.findModelExploreById(id).getModelName();
        return new Msg<>(MsgCode.SUCCEED, modelName);
    }

    /**
     * 获取所有位发布的算法
     *
     * @return
     */
    @GetMapping("/getUnpublishedModel")
    @ApiOperation(value = "获取私有的未删除的算法", notes = "获取的是 ModelExplore 实例")
    public Msg<List<ModelExplore>> getUnpublishedModel() {
        String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
        Integer userId = UserContextHolder.getUserContext().getId();
        List<ModelExplore> modelExplores = modelExploreService.findAll();
//        List<ModelExplore> result = modelExplores.stream()
//                .filter(modelExplore -> modelExplore.getIsDelete() == ModelExplore.NOT_DELETE && modelExplore.getCreatorId() == userId)
//                .collect(Collectors.toList());
        List<ModelExplore> result = modelExplores.stream()
                .filter(modelExplore -> modelExplore.getIsDelete() == ModelExplore.NOT_DELETE)
                .filter(modelExplore -> {
                    if ("管理员".equals(roleName) || "管理人员".equals(roleName)) {
                        return true;
                    } else {
                        return modelExplore.getCreatorId() == userId;
                    }
                })
                .collect(Collectors.toList());
        // 按照 id desc
        Collections.reverse(result);
        log.info("未删除的算法数：{}", result.size());
        if (result.isEmpty()) {
            return new Msg<>(MsgCode.NO_MODEL_EXPLORE);
        }
        return new Msg<>(MsgCode.SUCCEED, result);
    }

    // @RequestMapping("/rejectRelease")
    // public Msg<String> rejectRelease(@RequestParam long id, @RequestParam String reason, @RequestParam long generationId) {
    //     ModelExplore modelExplore = modelExploreService.findModelExploreById(id);
    //     modelExplore.setRejectReason(reason);
    //     modelExplore.setReleaseStatus(ModelExplore.DENIED);
    //     modelExploreService.saveModel(modelExplore);
    //     ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(generationId);
    //     modelGeneration.setStatus(ModelGeneration.REJECTED);
    //     modelGenerationService.saveModelGeneration(modelGeneration);
    //     return new Msg<>(MsgCode.SUCCEED);
    // }

    // /**
    //  * 发布算法到算法商城
    //  *
    //  * @param data = {modelExploreId:long  ModelExploreId:long  modelClassifications:[{id:long}]}
    //  * @return
    //  */
    // @RequestMapping("/publishModel")
    // @ApiOperation(value = "发布算法到算商城", notes = "发布到算法商城中的算法是model")
    // public Msg<String> publishModel(@RequestBody JSONObject data) {
    //     long modelExploreId = data.getLong("modelExploreId");
    //     long modelGenerationId = data.getLong("modelGenerationId");
    //     JSONArray mcIds = data.getJSONArray("modelClassifications");
    //     List<ModelClassification> modelClassifications = new ArrayList<>();
    //     for (int i = 0; i < mcIds.size(); i++) {
    //         JSONObject mcId = mcIds.getJSONObject(i);
    //         modelClassifications.add(modelClassificationService.getModelClassification(mcId.getLong("id")));
    //     }
    //     log.info("算法id：{}，生产任务id：{}", modelExploreId, modelGenerationId);
    //     try {
    //         ModelExplore modelExplore = modelExploreService.getModelExploreById(modelExploreId);
    //         Model model = modelService.findModelByName(modelExplore.getModelName());
    //         // 算法算法商城中没有算法，新建！
    //         if (Objects.isNull(model)) {
    //             Model newModel = new Model();
    //             BeanUtils.copyProperties(modelExplore, newModel, "releaseTime", "id", "isDelete");
    //             newModel.setModelEnglishName("model-" + System.currentTimeMillis());
    //             newModel.setReleaseTime(new Date());
    //             newModel.setGenerationId(modelGenerationId);
    //             newModel.setIsDelete(Model.NOT_DELETE);
    //             newModel.setBestWeightPath(modelExplore.getBestWeightPath());
    //             newModel.setSource(Model.GENERATION);
    //             // 复制镜像
    //             modelVersionService.copyModelVersion(modelExplore);
    //             List<ModelConfig> modelConfigList = modelExplore.getModelConfigList();
    //             ArrayList<ModelConfig> modelConfigs = new ArrayList<>();
    //             modelConfigList.forEach(modelConfig -> {
    //                 ModelConfig newModelConfig = new ModelConfig();
    //                 BeanUtils.copyProperties(modelConfig, newModelConfig, "id");
    //                 modelConfigService.saveModelConfig(newModelConfig);
    //                 // 复制
    //                 modelConfigs.add(newModelConfig);
    //             });
    //             newModel.setModelConfigList(modelConfigs);
    //             newModel.setModelClassification(modelClassifications);
    //             modelService.saveModel(newModel);
    //         } else {
    //             // 算法商城中存在 -> 查询该算法的所有分类信息
    //             model.setGenerationId(modelGenerationId);
    //             model.setBestWeightPath(modelExplore.getBestWeightPath());
    //             model.setReleaseTime(new Date());
    //             model.setIsDelete(Model.NOT_DELETE);
    //             model.setSource(Model.GENERATION);
    //             model.setModelClassification(modelClassifications);
    //             modelVersionService.copyModelVersion(modelExplore);
    //             // update
    //             modelService.saveModel(model);
    //         }
    //         //更新modelExplore状态为审核通过
    //         modelExplore.setReleaseStatus(ModelExplore.APPROVED);
    //         modelExploreService.saveModel(modelExplore);
    //         ModelGeneration mg = modelGenerationRepo.getModelGenerationById(modelGenerationId);
    //         mg.setStatus(ModelGeneration.PUBLISHED);
    //         modelGenerationRepo.save(mg);
    //         return new Msg<>(MsgCode.SUCCEED);
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //         return new Msg<>(MsgCode.FAILED);
    //     }
    // }
}
