package org.dlut.adv.mineai.model.service;


import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.common.aliasing.qual.Unique;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.utils.FieldRename;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.harbor.HarborController;
import org.dlut.adv.mineai.model.repository.*;
import org.dlut.adv.mineai.model.service.inter.ModelClassificationStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.persistence.criteria.*;
import javax.ws.rs.core.Application;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModelService {
    @Resource
    ModelRepo modelRepo;

    @Resource
    ModelMineServiceRepo modelMineServiceRepo;

    @Resource
    private MonitorModelConfigRepo monitorModelConfigRepo;

    @Resource
    private HarborController harborController;

    @Resource
    private ModelVersionRepo modelVersionRepo;

    @Resource
    private ModelVersionService modelVersionService;

    @Resource
    private ModelConfigService modelConfigService;

    FieldRename fieldRename = new FieldRename();

    @Value("${modelImageRootPath}")
    private String modelImageRootPath;

    @Lazy
    @Resource
    private ModelUploadService modelUploadService;
    @Autowired
    private ModelGenerationRepo modelGenerationRepo;
    @Autowired
    private ApplicationNameService applicationNameService;
    @Autowired
    private ApplicationNameRepo applicationNameRepo;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private ModelApplicationRepo modelApplicationRepo;

    /**
     * 用于动态分页查找
     */
    public Page<Model> dynamicFindModelPage(Pageable pageable, Model model,String chipType) {
        return modelRepo.findAll(new Specification<Model>() {
            @Override
            public javax.persistence.criteria.Predicate toPredicate(Root<Model> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
                //未被删除保留
                predicates.add(cb.notEqual(root.<Integer>get("isDelete"), 1));

                if (StringUtils.isNotBlank(chipType)) {  // 使用传入的 chipType 参数进行查询
                    Join<Model, Chip> chipJoin = root.join("chip", JoinType.LEFT);
                    predicates.add(cb.like(chipJoin.get("chipType"), "%" + chipType + "%"));
                }

                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(model.getModelName())
                        && StringUtils.isBlank(model.getDescription())
                        && StringUtils.isBlank(model.getModelEnglishName())
                        && model.getMonitorType() == 0;
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(model.getModelName())) {
                        predicates.add(cb.like(root.<String>get("modelName"), "%" + model.getModelName() + "%"));
                    }
                    //根据描述查询
                    if (StringUtils.isNotBlank(model.getDescription())) {
                        predicates.add(cb.like(root.<String>get("description"), "%" + model.getDescription() + "%"));
                    }
                    //根据英文名称查询
                    if (StringUtils.isNotBlank(model.getModelEnglishName())) {
                        predicates.add(cb.like(root.<String>get("modelEnglishName"), "%" + model.getModelEnglishName() + "%"));
                    }
                    if (model.getMonitorType() != 0) {
                        predicates.add(cb.equal(root.get("monitorType"), model.getMonitorType()));
                    }
                }
                cq.where(predicates.toArray(new javax.persistence.criteria.Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 模糊查询
     */
    public Page<Model> getVagueModelPage(Pageable pageable, String vagueInfo) {
        return modelRepo.findAll(new Specification<Model>() {
            @Override
            public Predicate toPredicate(Root<Model> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.notEqual(root.<Integer>get("isDelete"), 1));

                    // 关联查询 Chip 表的 chipType
                    Join<Model, Chip> chipJoin = root.join("chip", JoinType.LEFT);

                    predicates.add(cb.or(cb.like(root.<String>get("modelName"), "%" + vagueInfo + "%"),
                            cb.like(root.<String>get("description"), "%" + vagueInfo + "%"),
                            cb.like(root.<String>get("modelEnglishName"), "%" + vagueInfo + "%"),
                            cb.like(chipJoin.get("chipType"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }


    public Page<Model> getVagueModelPageWithoutEnglishName(Pageable pageable, String vagueInfo) {
        return modelRepo.findAll(new Specification<Model>() {
            @Override
            public Predicate toPredicate(Root<Model> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.notEqual(root.<Integer>get("isDelete"), 1));

                    predicates.add(cb.or(cb.like(root.<String>get("modelName"), "%" + vagueInfo + "%"), cb.like(root.<String>get("description"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    public List<Model> getModel() {
        return modelRepo.findModelsByIsDelete(Model.NOT_DELETE);
    }

    public List<Model> findModelListByDataNumAndDesc() {
        return modelRepo.findModelListByDataNumAndDesc(Model.NOT_DELETE, 50);
    }

    /**
     * 保存算法
     *
     * @param model
     */
    public void saveModel(Model model) {
        modelRepo.save(model);
    }

    /**
     * 判断modelName是否存在
     *
     * @param model
     * @return
     */
    public boolean isModelExist(Model model) {
        if (StringUtils.isBlank(model.getModelName())) {
            return false;
        }
        return modelRepo.findModelByModelName(model.getModelName()) != null;
    }

    /**
     * @param model
     * @description 判断Description是否存在
     */
    public boolean isModelDescriptionExist(Model model) {
        if (StringUtils.isBlank(model.getDescription())) {
            return false;
        }
        return modelRepo.findModelByDescription(model.getDescription()) != null;
    }

    /**
     * @param model
     * @description 判断EnglishName是否存在
     */
    public boolean isModelEnglishNameExist(Model model) {
        if (StringUtils.isBlank(model.getModelEnglishName())) {
            return false;
        }
        return modelRepo.findModelByModelEnglishName(model.getModelEnglishName()) != null;
    }

    /**
     * 更新算法
     *
     * @param model
     * @return
     */
    public MsgCode updateModel(Model model) {
        //判断模型名称   模型描述是否为空
        if (StringUtils.isBlank(model.getModelName())) {
            return MsgCode.MODEL_NAME_EMPTY;
        }
        if (StringUtils.isBlank(model.getDescription())) {
            return MsgCode.MODEL_DESCRIPTION_EMPTY;
        }

        Model sameModelNameModel = modelRepo.findModelByModelName(model.getModelName());
        Model sameModelEnglishNameModel = modelRepo.findModelByModelEnglishName(model.getModelEnglishName());
        Model sameDesNameModel = modelRepo.findModelByDescription(model.getDescription());
        if (sameModelNameModel != null) {
            if (sameModelNameModel.getId() != model.getId()) {
                return MsgCode.MODEL_NAME_EXIST;
            } else {
                if (sameDesNameModel != null) {
                    if (sameDesNameModel.getId() != model.getId()) {
                        return MsgCode.MODEL_DESCRIPTION_EXIST;
                    } else {
                        if (sameModelEnglishNameModel != null) {
                            if (sameModelEnglishNameModel.getId() != model.getId()) {
                                return MsgCode.MODEL_ENGLISH_NAME_EXIST;
                            } else {
                                model.setSubsystem(Model.CENTRAL_PLATFORM);
                                modelRepo.save(model);
                                return MsgCode.SUCCEED;
                            }
                        }
                    }
                }
            }
        }
        model.setSubsystem(Model.CENTRAL_PLATFORM);
        modelRepo.save(model);
        return MsgCode.SUCCEED;

    }

    /**
     * 根据Id查找model
     */
    public Model findModelById(long id) {
        return modelRepo.findModelById(id);
    }

    /**
     * 根据算法名称查询算法
     */
    public Model findModelByName(String name) {
        return modelRepo.findModelByModelName(name);
    }

    public Model findModelByModelEnglishName(String englishName) {
        return modelRepo.findModelByModelEnglishName(englishName);
    }


    public Model getModelById(Long modelId) {
        Optional<Model> optional = modelRepo.findById(modelId);
        return optional.orElse(null);
    }

    /**
     * 查询当前设备已绑定的算法id
     */
    public List<Long> findModelIdByMonitorId(long monitorId) {
        return modelRepo.findModelIdByMonitorId(monitorId, Model.NOT_DELETE);
    }

    /**
     * 查询当前设备绑定的算法list
     */
    public List<Model> findModelsByMonitorId(long monitorId) {
        return modelRepo.findModelsByMonitorId(monitorId, Model.NOT_DELETE);
    }

    public List<Monitor> findMonitorsBymodelId(long modelId) {
        return modelRepo.findMonitorsByModelId(modelId, Monitor.NOT_DELETE);
    }

    /**
     * 根据modelName查询modelConfigList
     *
     * @param modelName [String]算法名称,unique
     * @return modelConfigList [List<ModelConfig>]
     */
    public List<ModelConfig> findModelConfigsByModelName(String modelName) {
        Model model = modelRepo.findModelByModelName(modelName);
        if (model == null) {
            return null;
        }
        return model.getModelConfigList();
    }

    public MineService findModelMineServiceByModelId(Model model) {
        return modelMineServiceRepo.findDistinctByModelContainingAndIsDelete(model, MineService.NOT_DELETE);
    }

    /**
     * 根据 modelId 删除在服务和算法关联表中相关信息
     *
     * @param model 算法
     */
    public void deleteModelMineServiceByModelId(Model model) {
        MineService mineService = modelMineServiceRepo.findDistinctByModelContainingAndIsDelete(model, MineService.NOT_DELETE);
        List<Model> models = mineService.getModel();
        if (models.removeIf(model1 -> model1.getId() == model.getId())) {
            System.out.println("Successfully removed Model with ID " + model.getId() + " from MineService");
        }
        mineService.setModel(models);
        modelMineServiceRepo.save(mineService);
    }


    /**
     * 软删除model,重命名删除的modelName
     *
     * @param modelId
     * @return isDelete
     */
    public boolean deleteModel(Long modelId) {
        Model model = modelRepo.findById(modelId).orElse(null);
        if (model != null) {
            // 查找monitor中除id外所有@Unique的属性
            List<String> uniqueFieldListExceptId = fieldRename.getUniqueFieldListExceptId(model.getClass());
            for (String uniqueFiled : uniqueFieldListExceptId) {
                try {
                    // 获取unique字段的值
                    String uniqueValue = (String) PropertyUtils.getProperty(model, uniqueFiled);
                    // 根据 uniqueFiled 调用对应repo查询方法
                    Method method = ModelRepo.class.getMethod("findModelListBy" + uniqueFiled.substring(0, 1).toUpperCase() + uniqueFiled.substring(1) + "Like", String.class, int.class);

                    //   Method method = ModelRepo.class.getMethod("findModelBy" + uniqueFiled.substring(0, 1).toUpperCase() + uniqueFiled.substring(1) + "LikeAndIsDelete", String.class, int.class);
                    List<Model> modelListByUniqueFiled = (List<Model>) method.invoke(modelRepo, uniqueValue + "%", Model.DELETE);
                    // 获取monitorProperty的值List
                    List<String> modelFiledList = new ArrayList<>();
                    for (Model model1 : modelListByUniqueFiled) {
                        modelFiledList.add((String) PropertyUtils.getProperty(model1, uniqueFiled));
                    }
                    // 重命名unique字段的值
                    String newName = fieldRename.renameProperty(uniqueValue, modelFiledList);
                    PropertyUtils.setProperty(model, uniqueFiled, newName);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            model.setIsDelete(Model.DELETE);
            modelRepo.save(model);
            return true;
        }
        return false;
    }

    private List<Field> getUniqueFields(Object object) {
        List<Field> uniqueFields = new ArrayList<>();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Unique.class)) {
                uniqueFields.add(field);
            }
        }
        return uniqueFields;
    }

    public List<Model> findModelbyMineServiceId(long mineServiceId) {
        return modelRepo.findModelByMineServiceId(mineServiceId, Model.NOT_DELETE);
    }

    /**
     * 模型是否有相关绑定
     *
     * @param model
     * @return
     */
    public Boolean isBindSth(Model model) {
        List<MonitorModelConfig> monitorModelConfigs = monitorModelConfigRepo.findMonitorModelConfigsByModelId(model.getId());
        MineService mineService = modelMineServiceRepo.findDistinctByModelContainingAndIsDelete(model, MineService.NOT_DELETE);
        return monitorModelConfigs.isEmpty() && mineService == null;
    }

    public Boolean hasTrainModelVersion(Model model) {
        return null;
    }


    public Msg<Model> uploadUrlImage(Long modelId, Long userId, String url, String showName, String level, String description, String use) {
        Model m = modelRepo.findModelById(modelId);
        System.out.println("开始上传到harbor");
        String newTag = null;
        Date createTime = new Date();
        newTag = "image" + createTime.getTime();
        Msg<Map<String, String>> mapMsg = harborController.urlUploadImage(url, showName, newTag);
        if (Objects.equals(mapMsg.getCode(), MsgCode.FAILED.getCode())) {
            return new Msg<>(MsgCode.UPLOAD_IMAGE_TO_HARBOR_FAILED);
        }
        if (Objects.equals(mapMsg.getCode(), MsgCode.SUCCEED.getCode())) {
            //根据所选镜像功能，新增镜像
            ModelVersion modelVersion = new ModelVersion();
            String name1 = showName + '_' + level;
            ModelVersion modelVersionInDatabase = modelVersionRepo.findModelVersionByNameAndIsDelete(name1, ModelVersion.NOT_DELETE);
            if (modelVersionInDatabase != null) {
                return new Msg<>(MsgCode.IMAGE_EXITED);
            }
            modelVersion.setName(name1);
            modelVersion.setShowName(showName);
            modelVersion.setCreateTime(createTime);
            modelVersion.setArchitecture("arm64");
            modelVersion.setUseGpu(mapMsg.getPayload().get("USE_GPU").equals("true"));
            modelVersion.setRoleId(Math.toIntExact(userId));
            modelVersion.setDescription(description);
            modelVersion.setUrl(showName + '_' + newTag);
            modelVersion.setLevel(level);
            modelVersion.setSize(2048);
            modelVersion.setStatus(ModelVersion.uploaded);
            modelVersion.setReuse(false);
            List<ModelConfig> modelConfigList = new ArrayList<>();
            mapMsg.getPayload().forEach((k, v) -> {
                        if (k.contains("HP_")) {
                            ModelConfig modelConfig = new ModelConfig();
                            modelConfig.setField(k);
                            modelConfig.setRequired(true);
                            modelConfig.setMsg(v);
                            modelConfigService.saveModelConfig(modelConfig);
                            modelConfigList.add(modelConfig);
                        }
                    }
            );
            modelVersion.setModelConfigList(modelConfigList);
            if (StringUtils.equals(use, "训练")) {
                modelVersion.setInferable(false);
                modelVersion.setInspectable(false);
                modelVersion.setTrainable(true);
                modelVersion.setAutoLabel(false);
                modelVersionService.saveModelVersion(modelVersion);
                m.setTrainModelVersion(modelVersion);
                saveModel(m);
                return new Msg<>(MsgCode.SUCCEED, m);
            } else if (StringUtils.equals(use, "推理")) {
                modelVersion.setInferable(true);
                modelVersion.setInspectable(false);
                modelVersion.setTrainable(false);
                modelVersion.setAutoLabel(false);
                modelVersionService.saveModelVersion(modelVersion);
                m.setDeployModelVersion(modelVersion);
                saveModel(m);
                return new Msg<>(MsgCode.SUCCEED, m);
            } else if (StringUtils.equals(use, "自动标注")) {
                modelVersion.setInferable(false);
                modelVersion.setInspectable(false);
                modelVersion.setTrainable(false);
                modelVersion.setAutoLabel(true);
                modelVersionService.saveModelVersion(modelVersion);
                m.setAutoLabelModelVersion(modelVersion);
                saveModel(m);
                return new Msg<>(MsgCode.SUCCEED, m);
            } else if (StringUtils.equals(use, "转换")) {
                modelVersion.setInferable(false);
                modelVersion.setInspectable(true);
                modelVersion.setTrainable(false);
                modelVersion.setAutoLabel(false);
                modelVersionService.saveModelVersion(modelVersion);
                m.setConvertModelVersion(modelVersion);
                saveModel(m);
                return new Msg<>(MsgCode.SUCCEED, m);
            }
        }
        return new Msg<>(MsgCode.FILE_NOT_EXITED);
    }

    /**
     * 算法商城上传镜像接口，方式为本地文件
     *
     * @param showName
     * @param level
     * @param fileName
     * @param description
     * @param use
     * @return
     */
    public Msg<Model> createLocal(Long userId, Long modelId, String showName, String level, String fileName, String description, String use) {
        Model m = modelRepo.findModelById(modelId);
        String directoryPath = modelImageRootPath + "/" + showName + '_' + level;
        String fileRealPath = directoryPath + "/" + showName + '_' + level;
        File tempFile = new File(fileRealPath);
        if (!tempFile.exists()) {
            tempFile.mkdirs();
        }
        MultipartFile file = modelUploadService.getMultipartFile_new(tempFile);
        Date createTime = new Date();
        String newTag = "image" + createTime.getTime();
        if (file != null) {
            System.out.println("开始上传到harbor");
            Msg<Map<String, String>> mapMsg = harborController.uploadImage(file, showName, newTag);
            if (Objects.equals(mapMsg.getCode(), MsgCode.FAILED.getCode())) {
                return new Msg<>(MsgCode.UPLOAD_IMAGE_TO_HARBOR_FAILED);
            }
            if (Objects.equals(mapMsg.getCode(), MsgCode.SUCCEED.getCode())) {
                //根据所选镜像功能，新增镜像
                ModelVersion modelVersion = new ModelVersion();
                String modelVersionName = showName + '_' + level;
                ModelVersion modelVersionInDatabase = modelVersionRepo.findModelVersionByNameAndIsDelete(modelVersionName, ModelVersion.NOT_DELETE);
                if (modelVersionInDatabase != null) {
                    return new Msg<>(MsgCode.IMAGE_EXITED);
                }
                modelVersion.setName(modelVersionName);
                modelVersion.setShowName(showName);
                modelVersion.setCreateTime(createTime);
                modelVersion.setArchitecture("arm64");
                modelVersion.setRoleId(Math.toIntExact(userId));
                modelVersion.setUseGpu(mapMsg.getPayload().get("USE_GPU").equals("true"));
                modelVersion.setDescription(description);
                modelVersion.setUrl(showName + '_' + newTag);
                modelVersion.setLevel(level);
                modelVersion.setStatus(ModelVersion.uploaded);
                modelVersion.setSize(2048);
                modelVersion.setReuse(false);
                List<ModelConfig> modelConfigList = new ArrayList<>();
                mapMsg.getPayload().forEach((k, v) -> {
                            if (k.contains("HP_")) {
                                ModelConfig modelConfig = new ModelConfig();
                                modelConfig.setField(k);
                                modelConfig.setRequired(true);
                                modelConfig.setMsg(v);
                                modelConfigService.saveModelConfig(modelConfig);
                                modelConfigList.add(modelConfig);
                            }
                        }
                );
                modelVersion.setModelConfigList(modelConfigList);
                if (StringUtils.equals(use, "训练")) {
                    modelVersion.setTrainable(true);
                    modelVersion.setInferable(false);
                    modelVersion.setInspectable(false);
                    modelVersion.setAutoLabel(false);
                    modelVersionService.saveModelVersion(modelVersion);
                    m.setTrainModelVersion(modelVersion);
                    saveModel(m);
                    return new Msg<>(MsgCode.SUCCEED, m);
                } else if (StringUtils.equals(use, "推理")) {
                    modelVersion.setInferable(true);
                    modelVersion.setInspectable(false);
                    modelVersion.setTrainable(false);
                    modelVersion.setAutoLabel(false);
                    modelVersionService.saveModelVersion(modelVersion);
                    m.setDeployModelVersion(modelVersion);
                    saveModel(m);
                    return new Msg<>(MsgCode.SUCCEED, m);
                } else if (StringUtils.equals(use, "自动标注")) {
                    modelVersion.setAutoLabel(true);
                    modelVersion.setInferable(false);
                    modelVersion.setInspectable(false);
                    modelVersion.setTrainable(false);
                    modelVersionService.saveModelVersion(modelVersion);
                    m.setAutoLabelModelVersion(modelVersion);
                    saveModel(m);
                    return new Msg<>(MsgCode.SUCCEED, m);
                } else if (StringUtils.equals(use, "转换")) {
                    modelVersion.setInspectable(true);
                    modelVersion.setInferable(false);
                    modelVersion.setTrainable(false);
                    modelVersion.setAutoLabel(false);
                    modelVersionService.saveModelVersion(modelVersion);
                    m.setConvertModelVersion(modelVersion);
                    saveModel(m);
                    return new Msg<>(MsgCode.SUCCEED, m);
                }
            }
        }
        return new Msg<>(MsgCode.FILE_NOT_EXITED);
    }

    /**
     * 获取所有未删除的算法
     */
    public List<Model> getNotDeletedModelList() {
        return this.modelRepo.findAll()
                .stream()
                .filter(model -> model.getIsDelete() == Model.NOT_DELETE)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有未被删除且绑定的算法
     */
    public List<Long> getAllBindModelIds() {
        return modelGenerationRepo.findAllBindModelIds();
    }

    /**
     * 获取基础算法数量（未删除的算法数）
     */
    public Long getBaseModelCount() {
        return modelRepo.countModelByIsDelete(Model.NOT_DELETE);
    }

    /**
     * 获取算法分类统计
     *
     * @return
     */
    public List<ModelClassificationStatistics> getModelClassigicationCount() {
        return modelRepo.findModelClassificationCount();
    }


    public Long getModelExploreByModelId(Long modelId) {
        Model model = modelRepo.findById(modelId).orElse(null);
        if (model != null) {
           ModelGeneration modelGeneration = modelGenerationRepo.findById(model.getGenerationId()).orElse(null);
            if (modelGeneration != null && modelGeneration.getModelExplore() != null) {
                return modelGeneration.getModelExplore().getId();
            }
        }
        return null;
    }

    public Model getModelByApplicationNameAndDevice(String applicationName, String deviceName) {
        ApplicationName applicationNameByApplicationName = applicationNameRepo.findApplicationByApplicationName(applicationName);
        Device deviceByDeviceName = deviceRepository.findDeviceByDeviceName(deviceName);
        return modelApplicationRepo.findModelApplicationByApplicationNameIdAndDeviceId(applicationNameByApplicationName.getId(), deviceByDeviceName.getId()).getModel();
    }
}
