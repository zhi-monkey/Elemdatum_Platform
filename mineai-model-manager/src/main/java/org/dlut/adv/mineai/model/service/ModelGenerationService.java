package org.dlut.adv.mineai.model.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.constant.ModelTypeConstant;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.utils.FieldRename;
import org.dlut.adv.mineai.core.vo.*;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.client.DubheUserFeign;
import org.dlut.adv.mineai.model.controller.ModelJobController;
import org.dlut.adv.mineai.model.domain.dto.*;
import org.dlut.adv.mineai.model.dto.*;
import org.dlut.adv.mineai.model.repository.ModelExploreRepo;
import org.dlut.adv.mineai.model.repository.ModelApplicationRepo;
import org.dlut.adv.mineai.model.repository.ModelGenerationRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.repository.PublishHyperParamsRepo;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.statusMachine.statemachine.StateMachineFactory;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.dlut.adv.mineai.model.vo.DatasetVersionDetailVO;
import org.dlut.adv.mineai.model.vo.DatasetVersionInfoVO;
import org.dlut.adv.mineai.model.vo.ModelGenerationBindInfo;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Service
public class ModelGenerationService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    @Value("${kubernetes.namespace}")
    String namespace;

    @Resource
    ModelGenerationRepo modelGenerationRepo;

    @Resource
    private ModelApplicationRepo modelApplicationRepo;

    @Resource
    ModelJobRepo modelJobRepo;

    @Resource
    @Lazy
    private ModelGenerationService modelGenerationService;

//    @Resource
//    LogService logService;

    @Resource
    private ModelJobService modelJobService;

    @Resource
    private ModelService modelService;

    @Resource
    DubheUserFeign dubheUserFeign;

    @Resource
    private DubheUtils dubheUtils;

    @Resource
    private ModelExploreService modelExploreService;

    @Resource
    private ModelVersionService modelVersionService;

    @Resource
    private PublishHyperParamsRepo publishHyperParamsRepo;

    @Resource
    private HardwareParamsService hardwareParamsService;

    @Resource
    private ModelJobController modelJobController;

    @Resource
    private Executor jobTrackingExecutor;

    FieldRename fieldRename = new FieldRename();
    @Autowired
    private ModelExploreRepo modelExploreRepo;

    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private ModelApplicationZipLabelService modelapplicationziplabelService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ModelApplicationZipLabelService modelApplicationZipLabelService;


    /**
     * 保存生产任务
     *
     * @param modelGeneration
     */
    public void saveModelGeneration(ModelGeneration modelGeneration) {
        modelGenerationRepo.save(modelGeneration);
    }

    public ModelGeneration findModelGenerationByName(String name) {
        return modelGenerationRepo.findModelGenerationByName(name);
    }

    /**
     * 根据id获取生产任务实体
     *
     * @param id
     * @return
     */
    public ModelGeneration findModelGenerationById(Long id) {
        return modelGenerationRepo.getModelGenerationById(id);
    }

    public ModelGeneration getModelGenerationById(Long id) {
        return modelGenerationRepo.getModelGenerationById(id);
    }

    public Msg<String> updateModelGeneration(ModelGeneration modelGeneration) {
        if (StringUtils.isBlank(modelGeneration.getName())) {
            return new Msg<>(MsgCode.MODEL_GENERATION_EMPTY);
        }
        ModelGeneration sameNameModelGeneration = modelGenerationRepo.findModelGenerationByName(modelGeneration.getName());
        if (sameNameModelGeneration != null && sameNameModelGeneration.getId() != modelGeneration.getId()) {
            return new Msg<>(MsgCode.MODEL_GENERATION_NAME_EXIST);
        } else {
            try {
                modelGenerationRepo.save(modelGeneration);
            } catch (Exception e) {
                return new Msg<>(MsgCode.MODEL_GENERATION_UPDATE_FAILED);
            }
            return new Msg<>(MsgCode.SUCCEED);
        }
    }

    /**
     * 用于动态分页查找
     *
     * @param pageable
     * @param modelGeneration
     * @return
     */
    public Page<ModelGeneration> dynamicFindModelGenerationPage(Pageable pageable, ModelGeneration modelGeneration) {
        return modelGenerationRepo.findAll(new Specification<ModelGeneration>() {
            @Override
            public Predicate toPredicate(Root<ModelGeneration> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("isDelete"), false));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelGeneration.getName());
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelGeneration.getName())) {
                        predicates.add(cb.like(root.<String>get("name"), "%" + modelGeneration.getName() + "%"));
                    }
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 用于动态分页查找非引导类作业
     *
     * @param pageable
     * @param modelGeneration
     * @return
     */
    public Page<ModelGeneration> dynamicFindUnGuidedModelGenerationPage(Pageable pageable, ModelGeneration modelGeneration, String userName) {
        return modelGenerationRepo.findAll(new Specification<ModelGeneration>() {
            @Override
            public Predicate toPredicate(Root<ModelGeneration> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.<Integer>get("isGuided"), false));
                predicates.add(cb.equal(root.get("isDelete"), false));
                //根据 modelName 查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelGeneration.getName());
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelGeneration.getName())) {
                        predicates.add(cb.like(root.<String>get("name"), "%" + modelGeneration.getName() + "%"));
                    }
                }
                //根据 用户名 查询
                if (StringUtils.isNotBlank(userName)) {
                    List<Long> ids = dubheUserFeign.findIdsByUsernameLike(dubheUtils.getAuthorization(), userName).getData();
                    if (ids != null && !ids.isEmpty()) {
                        // 使用in方法查询多个用户ID
                        predicates.add(cb.in(root.get("userId")).value(ids));
                    } else {
                        // 如果没查到任何用户，返回空结果（避免查询所有记录）
                        predicates.add(cb.equal(root.get("userId"), -1L));
                    }
                }
                // 普通用户传参、管理者不传参：前端逻辑判断
                String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    predicates.add(cb.equal(root.get("userId"), UserContextHolder.getUserContext().getId()));
                }

                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 模糊查询非引导类作业
     */
    public Page<ModelGeneration> getVagueUnGuidedModelGenerationPage(Pageable pageable, String vagueInfo) {
        return modelGenerationRepo.findAll(new Specification<ModelGeneration>() {
            @Override
            public Predicate toPredicate(Root<ModelGeneration> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.<Integer>get("isGuided"), false));
                predicates.add(cb.equal(root.get("isDelete"), false));
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.or(cb.like(root.<String>get("name"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }


    /**
     * 用于动态分页查找引导类作业
     */
    public Page<ModelGeneration> dynamicFindGuidedModelGenerationPage(Pageable pageable, ModelGeneration modelGeneration) {
        return modelGenerationRepo.findAll(new Specification<ModelGeneration>() {
            @Override
            public Predicate toPredicate(Root<ModelGeneration> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.<Integer>get("isGuided"), true));
                predicates.add(cb.equal(root.get("isDelete"), false));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelGeneration.getName());
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelGeneration.getName())) {
                        predicates.add(cb.like(root.<String>get("name"), "%" + modelGeneration.getName() + "%"));
                    }
                }
                // 普通用户传参、管理者不传参：前端逻辑判断
                String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    predicates.add(cb.equal(root.get("userId"), UserContextHolder.getUserContext().getId()));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 模糊查询引导类作业
     */
    public Page<ModelGeneration> getVagueGuidedModelGenerationPage(Pageable pageable, String vagueInfo) {
        return modelGenerationRepo.findAll(new Specification<ModelGeneration>() {
            @Override
            public Predicate toPredicate(Root<ModelGeneration> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.<Integer>get("isGuided"), true));
                predicates.add(cb.equal(root.get("isDelete"), false));
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.or(cb.like(root.<String>get("name"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }


    public Page<ModelGeneration> dynamicFindPublishingModelGenerationPage(Pageable pageable, ModelGeneration modelGeneration) {
        return modelGenerationRepo.findAll(new Specification<ModelGeneration>() {
            @Override
            public Predicate toPredicate(Root<ModelGeneration> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.<Integer>get("status"), 10));
                predicates.add(cb.equal(root.get("isDelete"), false));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelGeneration.getName());
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelGeneration.getName())) {
                        predicates.add(cb.like(root.<String>get("name"), "%" + modelGeneration.getName() + "%"));
                    }
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 模糊查询
     */
    public Page<ModelGeneration> getVagueModelGenerationPage(Pageable pageable, String vagueInfo) {
        return modelGenerationRepo.findAll(new Specification<ModelGeneration>() {
            @Override
            public Predicate toPredicate(Root<ModelGeneration> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("isDelete"), false));
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.or(cb.like(root.<String>get("name"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 判断 modelGeneration 是否存在
     *
     * @param modelGeneration
     * @return
     */
    public boolean isModelGenerationExist(ModelGeneration modelGeneration) {
        if (StringUtils.isBlank(modelGeneration.getName())) {
            return false;
        }
        return modelGenerationRepo.findModelGenerationByName(modelGeneration.getName()) != null;
    }

//    public MsgCode deleteModelGeneration(long modelGenerationId) {
//        try {
//            ModelGeneration modelGenerationById = modelGenerationRepo.getModelGenerationById(modelGenerationId);
//            //modelGenerationById.setStatus(ModelGeneration.CANCELED);
//            modelGenerationRepo.save(modelGenerationById);
//            //modelGenerationRepo.delete(modelGeneration);
//            return MsgCode.SUCCEED;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return MsgCode.FAILED;
//        }
//    }

    public MsgCode deleteModelGeneration(long modelGenerationId) {
        try {
            ModelGeneration modelGeneration = modelGenerationRepo.getModelGenerationById(modelGenerationId);
            if (modelGeneration != null) {
                // 获取除id外的唯一字段列表
                List<String> uniqueFieldListExceptId = fieldRename.getUniqueFieldListExceptId(modelGeneration.getClass());
                for (String uniqueField : uniqueFieldListExceptId) {
                    try {
                        // 获取unique字段的值
                        String uniqueValue = (String) PropertyUtils.getProperty(modelGeneration, uniqueField);

                        // 根据uniqueField动态调用对应repo查询方法
                        Method method = ModelGenerationRepo.class.getMethod(
                                "findModelGenerationListBy" + uniqueField.substring(0, 1).toUpperCase() + uniqueField.substring(1) + "Like",
                                String.class, boolean.class
                        );

                        List<ModelGeneration> modelListByUniqueField = (List<ModelGeneration>) method.invoke(
                                modelGenerationRepo, uniqueValue + "%", true
                        );

                        // 获取现有的唯一字段值列表，用于生成新的名称
                        List<String> modelFieldList = new ArrayList<>();
                        for (ModelGeneration mg : modelListByUniqueField) {
                            modelFieldList.add((String) PropertyUtils.getProperty(mg, uniqueField));
                        }

                        // 重命名unique字段的值
                        String newName = fieldRename.renameProperty(uniqueValue, modelFieldList);
                        PropertyUtils.setProperty(modelGeneration, uniqueField, newName);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
                // 设置isDelete为删除状态
                modelGeneration.setIsDelete(true);
                // 清除与数据集版本的关联喘息
                modelGeneration.setTrainDatasetVersions(null);
                modelGenerationRepo.save(modelGeneration);
                // 检查镜像组的绑定
                if (modelGeneration.getModelExplore() != null) {
                    long modelExploreId = modelGeneration.getModelExplore().getId();
                    Integer count = modelGenerationRepo.countByModelExploreId(modelExploreId);
                    if (count == 0) {
                    // 如果没有绑定的就设置为未绑定状态
                    ModelExplore modelExplore = modelExploreRepo.findModelExploreById(modelExploreId);
                    modelExplore.setIsBoundWithGeneration(ModelExplore.NOT_BOUND);
                        modelExploreRepo.save(modelExplore);
                    }
                }

                return MsgCode.SUCCEED;
            }
            return MsgCode.FAILED;
        } catch (Exception e) {
            e.printStackTrace();
            return MsgCode.FAILED;
        }
    }


//    /**
//     * 比对两个modelVersion，如果前者优，则返回1，后者优返回-1，两者相同返回0
//     *
//     * @param
//     * @return
//     */
//    public int findBetterModelVersion(ModelJob modelJobA, ModelJob modelJobB) {
//        if (modelJobA == null && modelJobB != null) {
//            return -1;
//        } else if (modelJobA != null && modelJobB == null) {
//            return 1;
//        } else if (modelJobA == modelJobB && modelJobA == null) {
//            return 0;
//        }
//        List<String> accAList = logService.getTestedDetail(namespace, modelJobA.getName(), 100L, 1641280408415000000L, null).get("accuracy");
//        List<String> accBList = logService.getTestedDetail(namespace, modelJobB.getName(), 100L, 1641280408415000000L, null).get("accuracy");
//        double accAMax = accAList.stream().mapToDouble(Double::parseDouble).max().orElse(0);
//        double accBMax = accBList.stream().mapToDouble(Double::parseDouble).max().orElse(0);
//        return Double.compare(accAMax, accBMax);
//    }

//    public boolean compareModelGeneration(ModelGeneration modelGeneration) {
//        //获取modelGeneration的model，此处认为在创建modelGeneration时一定已经有了model或者同时创建并绑定了model
//        ModelExplore model = modelGeneration.getModelExplore();
//        //获取modelGeneration的质检modelVersion,将其与model 的质检模型进行比较
//        ModelVersion modelVersionTest = modelGeneration.getTestModelVersion();
//        //获取modelGeneration的部署modelVersion
//        ModelVersion modelVersionDeploy = modelGeneration.getDeployModelVersion();
//        ModelVersion modelVersionTrain = modelGeneration.getTrainModelVersion();
//        //获取model的A，B质检模型
//        ModelVersion modelVersionTestA = modelVersionRepo.findModelVersionByModelAndVersionLevelAndIsInspectableAndIsDelete
//                (model, ModelVersion.BASIC_VERSION, true, ModelVersion.NOT_DELETE);
//        ModelVersion modelVersionTestB = modelVersionRepo.findModelVersionByModelAndVersionLevelAndIsInspectableAndIsDelete
//                (model, ModelVersion.BEST_VERSION, true, ModelVersion.NOT_DELETE);
//        //获取model的B的部署模型
//        ModelVersion modelVersionDeployB = modelVersionRepo.findModelVersionByModelAndVersionLevelAndIsInferableAndIsDelete
//                (model, ModelVersion.BEST_VERSION, true, ModelVersion.NOT_DELETE);
//        ModelVersion modelVersionTrainB = modelVersionRepo.findModelVersionByModelAndVersionLevelAndIsTrainableAndIsDelete
//                (model, ModelVersion.BEST_VERSION, true, ModelVersion.NOT_DELETE);
//        //如果有A，并且有B，则与B比较,如果比B更优，则将B替换掉
//        //替换：将质检部署模型同时替换
////        if (modelVersionTestA != null) {
////            if (modelVersionTestB != null) {
////                int result = findBetterModelVersion(modelGeneration.getTestJob(), modelGeneration.getAnotherTestJob());
////                return result == 1;
////                //如果不存在B，则与A比较，如果优于A，则将其设为B
////            } else {
////                int result = findBetterModelVersion(modelGeneration.getTestJob(), modelGeneration.getAnotherTestJob());
////                return result == 1;
////            }
////            //如果不存在A，则判断是否有B，如果有B，则与B比较，优于B，则将其替换
////        } else {
////            if (modelVersionTestB != null) {
////                int result = findBetterModelVersion(modelGeneration.getTestJob(), modelGeneration.getAnotherTestJob());
////                return result == 1;
////            }
////            //如果没有B，则直接将其设置为B
////            else {
////                return true;
////            }
////        }
//        return false;
//    }

    /**
     * 根据cpu，内存，gpu，描述和参数判断两个job是否相同
     *
     * @param modelJobA
     * @param modelJobB
     * @return
     */
    private boolean equalModelJob(ModelJob modelJobA, ModelJob modelJobB) {
        if (modelJobA == null && modelJobB == null) {
            return true;
        } else if (modelJobA == null || modelJobB == null) {
            return false;
        }
        return StringUtils.equals(modelJobA.getMemory(), modelJobB.getMemory())
                && StringUtils.equals(modelJobA.getCpus(), modelJobB.getCpus())
                && StringUtils.equals(modelJobA.getGpus(), modelJobB.getGpus())
                && StringUtils.equals(modelJobA.getDescription(), modelJobB.getDescription())
                && (modelJobA.getParams() == null ? modelJobB.getParams() == null : modelJobA.getParams().equals(modelJobB.getParams()));
    }

    /**
     * 根据cpu，内存，gpu，描述和参数map创建新job
     *
     * @param modelJob
     * @return
     */
    private ModelJob copyModelJob(ModelJob modelJob) {
        ModelJob copyJob = new ModelJob();

        copyJob.setGpus(modelJob.getGpus());
        copyJob.setMemory(modelJob.getMemory());
        copyJob.setCpus(modelJob.getCpus());
        copyJob.setDescription(modelJob.getDescription());
        Map<String, String> params = new HashMap<>(modelJob.getParams());
        copyJob.setParams(params);

        return copyJob;
    }

    /**
     * 查询
     *
     * @param testJobId
     * @param anotherTestJobId
     * @return
     */
    public ModelGeneration findModelGenerationByTestJobAndAnotherTestJob(long testJobId, long anotherTestJobId) {
        ModelJob testJob = modelJobRepo.findModelJobById(testJobId);
        ModelJob anotherTestJob = modelJobRepo.findModelJobById(anotherTestJobId);
        return new ModelGeneration();
    }

    public ModelGeneration findModelGenerationByTestJob(long testJobId) {
        ModelJob testJob = modelJobRepo.findModelJobById(testJobId);
        return new ModelGeneration();
    }

    public ModelGeneration findGeneraByJob(ModelJob modelJob) {
        if (ModelJob.TRAIN == modelJob.getJobType()) {
            return new ModelGeneration();
        }
        return new ModelGeneration();
    }

    public Map<Long, Integer> getModelGenerationStatusById(List<Long> modelGenerationIds) {
        Map<Long, Integer> mapStatus = new HashMap<>();
        for (Long id : modelGenerationIds) {
            ModelGeneration generation = modelGenerationRepo.getModelGenerationById(id);
            if (generation == null) {
                continue;
            }
            int mgStatus;
            if (generation.getIsGuided()) {
                mgStatus = generation.getStatusForGuided();
            } else {
                mgStatus = generation.getStatusNew();
            }
            mapStatus.put(id, mgStatus);
        }
        return mapStatus;
    }


    public List<ModelGeneration> findAllByModelExploreId(long id) {
        ModelExplore model = new ModelExplore();
        model.setId(id);
        return modelGenerationRepo.findModelGenerationsByModelExplore(model);
    }

    /**
     * 获取 ModelGeneration
     * 优化：ModelGeneration 走聚簇索引，远程调用查询用户使用字典，缩短openfeign远调时间
     *
     * @return
     */
    public List<HashMap<String, Object>> getAllModelGeneration() {
        HashMap<Long, String> cacheMap = new HashMap<>();
        List<HashMap<String, Object>> result = modelGenerationRepo.findAll().stream()
                .map(modelGeneration -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("modelGenerationId", modelGeneration.getId());
                    map.put("modelGenerationName", modelGeneration.getName());
                    map.put("modelGenerationDescription", modelGeneration.getDescription());
                    map.put("modelGenerationCreateTime", modelGeneration.getCreateTime());

                    // 调用 getStatusNew() 获取新状态值
                    Integer statusNew = modelGeneration.getStatusNew();
                    String mgStatus = null;
                    if (statusNew.equals(ModelGeneration.EXECUTING)) {
                        mgStatus = "执行中";
                    } else if (statusNew.equals(ModelGeneration.PAUSED)) {
                        mgStatus = "暂停";
                    } else if (statusNew.equals(ModelGeneration.NOT_ACTIVE)) {
                        mgStatus = "未激活";
                    }

                    map.put("modelGenerationStatus", mgStatus);

                    if (cacheMap.get(modelGeneration.getUserId()) == null) {
                        cacheMap.put(modelGeneration.getUserId(), dubheUserFeign.findUserById(dubheUtils.getAuthorization(), modelGeneration.getUserId()).getData().getNickName());
                    }
                    map.put("modelGenerationCreator", cacheMap.get(modelGeneration.getUserId()));
                    return map;
                }).collect(Collectors.toList());
        return result;
    }

    /**
     * 保存算法的基础基础信息，同时保留各种参数信息
     *
     * @param modelGenerationId
     * @param modelJobId
     * @param bestWeightPath
     * @param modelName
     * @return
     */
    @Transactional
    public boolean publishModel2BaseAlgorithmLibAndSaveParameters(Long modelGenerationId, Long modelJobId, String bestWeightPath, String modelName, String description) {
        // 保存基础算法库中的基本信息
        ModelJob modelJob = modelJobService.getModelJobById(modelJobId);
        ModelGeneration modelGeneration = this.getModelGenerationById(modelGenerationId);
        ModelExplore modelExplore = modelGeneration.getModelExplore();
        Model sourceModel = modelGeneration.getModel();

        ModelVersion trainModelVersion = resolveTrainModelVersion(modelGeneration);
        ModelVersion convertModelVersion = resolveConvertModelVersion(modelGeneration);
        ModelVersion deployModelVersion = resolveDeployModelVersion(modelGeneration);
        Chip chip = convertModelVersion != null ? convertModelVersion.getChip() : null;

        if (trainModelVersion == null) {
            throw new RuntimeException("发布失败：未找到训练模型版本");
        }
        if (chip == null) {
            throw new RuntimeException("发布失败：未找到转换模型芯片信息");
        }

        String sourceModelEnglishName = modelExplore != null ? modelExplore.getModelName() : (sourceModel != null ? sourceModel.getModelEnglishName() : null);
        String sourceDescription = modelExplore != null ? modelExplore.getDescription() : (sourceModel != null ? sourceModel.getDescription() : null);

        Model model = extracted(modelGenerationId, bestWeightPath, modelName, modelJob, chip, sourceModelEnglishName, sourceDescription, description);
        model.setTrainModelVersion(trainModelVersion);
        model.setDeployModelVersion(deployModelVersion);
        model.setConvertModelVersion(convertModelVersion);
        model.setCreateUserId((long) UserContextHolder.getUserContext().getId());
        modelService.saveModel(model);
        // 存储镜像中的 训练 转换 参数
        List<PublishedHyperParams> publishedHyperParams = storeTrainAndConvertDefaultParameters(
                Arrays.asList(trainModelVersion, convertModelVersion),
                "HP",
                "CONVERT"
        );
        // 训练自定义参数
        log.info("trainModelJob: {}", modelJob.getId());
        Map<String, String> trainJobParamsCopy = new HashMap<>(modelJob.getParams());
        publishedHyperParams
                .forEach(p -> {
                    if (trainJobParamsCopy.containsKey(p.getParamName())) {
                        p.setTrainDefaultValue(trainJobParamsCopy.get(p.getParamName()));
                        publishHyperParamsRepo.save(p); // 防止直接删空而不更新
                        trainJobParamsCopy.remove(p.getParamName());
                    }
                });
        // 存在非 训练 和 转换 的其他参数
        if (!trainJobParamsCopy.isEmpty()) {
            trainJobParamsCopy.keySet().forEach(s -> {
                PublishedHyperParams p = new PublishedHyperParams();
                p.setParamName(s);
                p.setParamUsage(PublishedHyperParams.PARAM_TYPE_OTHER);
                p.setTrainDefaultValue(trainJobParamsCopy.get(s));
                publishHyperParamsRepo.save(p);
                publishedHyperParams.add(p);
            });
        }
        // 最新转换 modelJob 参数
        Long convertLatestModelJobId = modelJobRepo.findLatestJobWithLikeName(modelJobId + "-convert");
        log.info("convertLatestModelJobId: {}", convertLatestModelJobId);
        if (convertLatestModelJobId != null) {
            ModelJob convertModelJob = modelJobService.getModelJobById(convertLatestModelJobId);
            Map<String, String> convertJobParamsCopy = new HashMap<>(convertModelJob.getParams());
            publishedHyperParams.forEach(p -> {
                if (convertJobParamsCopy.containsKey(p.getParamName())) {
                    p.setTrainDefaultValue(convertJobParamsCopy.get(p.getParamName()));
                    publishHyperParamsRepo.save(p);
                    convertJobParamsCopy.remove(p.getParamName());
                }
            });
            if (!convertJobParamsCopy.isEmpty()) {
                convertJobParamsCopy.keySet().forEach(s -> {
                    PublishedHyperParams p = new PublishedHyperParams();
                    p.setParamName(s);
                    p.setParamUsage(PublishedHyperParams.PARAM_TYPE_OTHER);
                    p.setTrainDefaultValue(convertJobParamsCopy.get(s));
                    publishHyperParamsRepo.save(p);
                    publishedHyperParams.add(p);
                });
            }
        }
        model.setPublishedHyperParamsList(publishedHyperParams);
        // 修改 ModelJob 状态为发布
//        modelJob.setStatus(ModelJobStateCodeConstant.PUBLISHED);
//        ModelJob m = modelJobService.saveModelJob(modelJob);
        // 状态转换
        try {
            // 状态机创建
//            System.out.println("发布父任务状态机创建");
//            stateMachineFactory.createStateMachine(String.valueOf(starterJob.getId()));
            ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
            modelJobStateMachine.modelJobTrainPublishedEvent(Integer.parseInt(String.valueOf(modelJob.getId())));
            return true;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存 训练 转换 默认参数
     *
     * @param modeExploreId
     * @return
     */
    private List<PublishedHyperParams> storeTrainAndConvertDefaultParameters(List<ModelVersion> modelVersions, String trainPrefixStr, String convertPrefixStr) {
        List<Long> trainConvertVersionIds = modelVersions.stream()
                .filter(Objects::nonNull)
                .map(ModelVersion::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return trainConvertVersionIds.stream()
                .flatMap(id -> {
                    ModelVersion modelVersion = modelVersionService.findModelVersionById(id);
                    if (modelVersion == null || modelVersion.getModelConfigList() == null) {
                        return Stream.empty();
                    }
                    return modelVersion.getModelConfigList().stream()
                            .filter(modelConfig -> modelConfig.getField() != null &&
                                    (modelConfig.getField().startsWith(trainPrefixStr) ||
                                            modelConfig.getField().startsWith(convertPrefixStr)))
                            .map(modelConfig -> {
                                PublishedHyperParams p = new PublishedHyperParams();
                                p.setParamName(modelConfig.getField());
                                p.setParamDescription(modelConfig.getInputDescription());
                                p.setParamUsage(modelConfig.getField().startsWith(trainPrefixStr) ?
                                        PublishedHyperParams.PARAM_TYPE_TRAIN :
                                        PublishedHyperParams.PARAM_TYPE_CONVERT);
                                p.setImageDefaultValue(modelConfig.getDefaultNum());
                                p.setParamType(modelConfig.getType());
                                p.setParamRange(modelConfig.getMsg());
                                publishHyperParamsRepo.save(p);
                                return p;
                            });
                }).collect(Collectors.toList());
    }

    /**
     * 保存算法的基本信息
     *
     * @param modelGenerationId
     * @param bestWeightPath
     * @param modelName
     * @param modelJob
     * @param chip
     * @param modelExplore
     * @return
     */
    private static Model extracted(Long modelGenerationId, String bestWeightPath, String modelName, ModelJob modelJob, Chip chip, String modelEnglishName, String sourceDescription, String description) {
        Model model = new Model();
        model.setReleaseTime(new Date());
        model.setTrainModelJob(modelJob);
        model.setBestWeightPath(bestWeightPath);
        model.setGenerationId(modelGenerationId);
        model.setSubsystem(Model.CENTRAL_PLATFORM);
        model.setModelName(modelName);
        model.setChip(chip);
        model.setModelEnglishName(modelEnglishName);
        if (description != null && !description.isEmpty()) {
            model.setDescription(description);
        } else {
            model.setDescription(sourceDescription);
        }
        return model;
    }

    private ModelVersion resolveTrainModelVersion(ModelGeneration modelGeneration) {
        if (modelGeneration.getModelExplore() != null) {
            ModelExplore modelExplore = modelExploreService.getModelExploreById(modelGeneration.getModelExplore().getId());
            if (modelExplore != null) {
                return modelExplore.getTrainModelVersion();
            }
        }
        if (modelGeneration.getModel() != null) {
            Model model = modelService.getModelById(modelGeneration.getModel().getId());
            if (model != null) {
                if (model.getTrainModelVersion() != null) {
                    return model.getTrainModelVersion();
                }
                if (model.getTrainModelJob() != null) {
                    return model.getTrainModelJob().getModelVersion();
                }
            }
        }
        return null;
    }

    private ModelVersion resolveConvertModelVersion(ModelGeneration modelGeneration) {
        if (modelGeneration.getModelExplore() != null) {
            ModelExplore modelExplore = modelExploreService.getModelExploreById(modelGeneration.getModelExplore().getId());
            if (modelExplore != null) {
                return modelExplore.getConvertModelVersion();
            }
        }
        if (modelGeneration.getModel() != null) {
            Model model = modelService.getModelById(modelGeneration.getModel().getId());
            if (model != null) {
                return model.getConvertModelVersion();
            }
        }
        return null;
    }

    private ModelVersion resolveDeployModelVersion(ModelGeneration modelGeneration) {
        if (modelGeneration.getModelExplore() != null) {
            ModelExplore modelExplore = modelExploreService.getModelExploreById(modelGeneration.getModelExplore().getId());
            if (modelExplore != null) {
                return modelExplore.getDeployModelVersion();
            }
        }
        if (modelGeneration.getModel() != null) {
            Model model = modelService.getModelById(modelGeneration.getModel().getId());
            if (model != null) {
                return model.getDeployModelVersion();
            }
        }
        return null;
    }

    public Msg<String> bindHardwareParams(long modelGenerationId, long hardwareParamsId) {
        ModelGeneration modelGeneration = findModelGenerationById(modelGenerationId);
        HardwareParams hardwareParams = hardwareParamsService.findHardwareParamsById(hardwareParamsId);
        modelGeneration.setHardwareParams(hardwareParams);
        saveModelGeneration(modelGeneration);
        return new Msg<>(MsgCode.SUCCEED);
    }

//    public Msg<Long> startGuidedGeneration(GuidedGenerationStartDTO guidedGenerationStartDTO) {
//        //获取模型转换参数
//        ModelConvertDTO convertDTO = guidedGenerationStartDTO.getModelConvert();
//        //获取量化数据集相关参数
//        Map<String, String> quantizationParams = guidedGenerationStartDTO.getQuantizationParams();
//        ModelGeneration modelGeneration = guidedGenerationStartDTO.getModelGeneration();
//        if (!isModelGenerationExist(modelGeneration)) {
//            if (modelGeneration.getModelExplore() != null) {
//                // 获得生产任务中对应的算法的实体
//                ModelExplore modelExplore = modelExploreService.getModelExploreById(modelGeneration.getModelExplore().getId());
//                modelExplore.setIsBoundWithGeneration(ModelExplore.BOUND);
//                // 更新算法的绑定状态
//                modelExploreService.saveModel(modelExplore);
//            }
//            //System.out.println("guidedGenerationStartDTO.getHardwareParamsID() = " + guidedGenerationStartDTO.getHardwareParamsID());
//            //modelGeneration.setHardwareParams(hardwareParamsService.findHardwareParamsById(guidedGenerationStartDTO.getHardwareParamsID()));
//            //bindHardwareParams(modelGeneration.getId(), guidedGenerationStartDTO.getHardwareParamsID());
//            Map<String, String> hyperParams = new HashMap<>(guidedGenerationStartDTO.getTrainHyperParams());
//            modelGeneration.setHyperParams(hyperParams);
//
//            // 拿到appZipPath
//            modelGeneration.setModelApplication(guidedGenerationStartDTO.getModelApplication());
//            modelGeneration.setQuantizationParams(quantizationParams);
//            modelGeneration.setHardwareParams(hardwareParamsService.findHardwareParamsById(guidedGenerationStartDTO.getHardwareParamsID()));
//            saveModelGeneration(modelGeneration);
//            try {
//                modelJobService.createJob(modelGeneration.getId(), null, 1);
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println("创建训练作业失败");
//            }
//            if (convertDTO != null) {
//                Hibernate.initialize(convertDTO.getParams());
//                ModelGeneration modelGenerationByName = findModelGenerationByName(modelGeneration.getName());
//                ModelJob lastJob = modelJobService.getModelJobById(modelGenerationByName.getLatestJobId());
//
//                // 使用线程池跟踪 Job 状态
//                trackJobStatusAsync(lastJob, modelGenerationByName, convertDTO, quantizationParams);
//
//            }
//
//            return new Msg<>(MsgCode.SUCCEED, modelGeneration.getId());
//        } else {
//            return new Msg<>(MsgCode.MODEL_GENERATION_NAME_EXIST);
//        }
//    }

    public Msg<Long> startGuidedGeneration(GuidedGenerationStartDTO guidedGenerationStartDTO) {
        ModelGeneration modelGeneration = guidedGenerationStartDTO.getModelGeneration();
        if (!isModelGenerationExist(modelGeneration)) {
            if (modelGeneration.getModelExplore() != null) {
                // 获得生产任务中对应的算法的实体
                ModelExplore modelExplore = modelExploreService.getModelExploreById(modelGeneration.getModelExplore().getId());
                modelExplore.setIsBoundWithGeneration(ModelExplore.BOUND);


                if (modelExplore.getTrainModelVersion() != null) {
                    String annotationType = modelExplore.getTrainModelVersion().getAnnotationType();
                    String annotationFormat = modelExplore.getTrainModelVersion().getAnnotationFormat();
                    if (annotationType != null) {
                        modelGeneration.setAnnotationType(annotationType);
                    }
                    if (annotationFormat != null) {
                        modelGeneration.setAnnotationFormat(annotationFormat);
                    }
                }
                // 更新算法的绑定状态
                modelExploreService.saveModel(modelExplore);
            }
            //System.out.println("guidedGenerationStartDTO.getHardwareParamsID() = " + guidedGenerationStartDTO.getHardwareParamsID());
            //modelGeneration.setHardwareParams(hardwareParamsService.findHardwareParamsById(guidedGenerationStartDTO.getHardwareParamsID()));
            //bindHardwareParams(modelGeneration.getId(), guidedGenerationStartDTO.getHardwareParamsID());
            // 拿到appZipPath
            modelGeneration.setModelApplication(guidedGenerationStartDTO.getModelApplication());


            // 如果forkDatasetId是null代表不是fork任务，则执行原逻辑，直接创建新数据集，否则用fork的父任务数据集
            DatasetCreateDTO datasetCreateDTO = new DatasetCreateDTO();
            datasetCreateDTO.setName(guidedGenerationStartDTO.getDatasetName());
            datasetCreateDTO.setDataType(0);
            datasetCreateDTO.setType(0);
            // 根据annotationType设置annotateType：目标检测(Detection) -> 102，语义分割(Segmentation) -> 103
            int annotateType = ModelGeneration.ANNOTATION_TYPE_SEGMENTATION.equals(modelGeneration.getAnnotationType()) ? 103 : 102;
            datasetCreateDTO.setAnnotateType(annotateType);
            datasetCreateDTO.setIsGuided(true);
            datasetCreateDTO.setModule(0);
            datasetCreateDTO.setDatasetGroupName(guidedGenerationStartDTO.getDatasetGroupName());
            datasetCreateDTO.setIsCreateDatasetGroup(true);
            Long dataSetId = Long.parseLong(String.valueOf(dubheDataFeign.createDataset(dubheUtils.getCurrentAuthorization(), datasetCreateDTO).getData()));
            List<Long> labelIds =  modelapplicationziplabelService.getBoundLabelIds(guidedGenerationStartDTO.getModelApplication().getId());
            dubheDataFeign.bandLabels(dubheUtils.getCurrentAuthorization(), dataSetId, labelIds);
            modelGeneration.setTrainDatasetStart(dataSetId);
            modelGeneration.setTrainDataset(dataSetId);
            // labelList
            List<DatasetLabelInfoDTO> labelInfoDTOs = dubheDataFeign.getDatasetLabels(dubheUtils.getAuthorization(), dataSetId).getData();
            modelGeneration.setSelectedLabels(convertListToMapWithStream(labelInfoDTOs));
            saveModelGeneration(modelGeneration);

            return new Msg<>(MsgCode.SUCCEED, modelGeneration.getId());
        } else {
            return new Msg<>(MsgCode.MODEL_GENERATION_NAME_EXIST);
        }
    }

    // fork任务的开始逻辑
    public Msg<Long> startForkGuidedGeneration(GuidedGenerationStartDTO guidedGenerationStartDTO) {
        if (StringUtils.isBlank(guidedGenerationStartDTO.getDatasetGroupName())) {
            return new Msg<>(MsgCode.FAILED);
        }

        ModelGeneration modelGeneration = guidedGenerationStartDTO.getModelGeneration();
        if (!isModelGenerationExist(modelGeneration)) {
            if (modelGeneration.getModelExplore() != null) {
                // 获得生产任务中对应的算法的实体
                ModelExplore modelExplore = modelExploreService.getModelExploreById(modelGeneration.getModelExplore().getId());
                modelExplore.setIsBoundWithGeneration(ModelExplore.BOUND);
                // 从训练模型版本中获取标注类型和标注格式并设置到生产任务中
                if (modelExplore.getTrainModelVersion() != null) {
                    String annotationType = modelExplore.getTrainModelVersion().getAnnotationType();
                    String annotationFormat = modelExplore.getTrainModelVersion().getAnnotationFormat();
                    if (annotationType != null) {
                        modelGeneration.setAnnotationType(annotationType);
                    }
                    if (annotationFormat != null) {
                        modelGeneration.setAnnotationFormat(annotationFormat);
                    }
                }
                // 更新算法的绑定状态
                modelExploreService.saveModel(modelExplore);
            }
            // 拿到appZipPath
            modelGeneration.setModelApplication(guidedGenerationStartDTO.getModelApplication());

            DatasetCreateDTO datasetCreateDTO = new DatasetCreateDTO();
            datasetCreateDTO.setName(guidedGenerationStartDTO.getDatasetName());
            datasetCreateDTO.setDataType(0);
            datasetCreateDTO.setType(0);
            // 根据annotationType设置annotateType：目标检测(Detection) -> 102，语义分割(Segmentation) -> 103
            int annotateType = ModelGeneration.ANNOTATION_TYPE_SEGMENTATION.equals(modelGeneration.getAnnotationType()) ? 103 : 102;
            datasetCreateDTO.setAnnotateType(annotateType);
            datasetCreateDTO.setIsGuided(true);
            datasetCreateDTO.setModule(0);
            datasetCreateDTO.setDatasetGroupName(guidedGenerationStartDTO.getDatasetGroupName());
            datasetCreateDTO.setIsCreateDatasetGroup(false);
            Long datasetId = Long.parseLong(String.valueOf(dubheDataFeign.createDataset(dubheUtils.getCurrentAuthorization(), datasetCreateDTO).getData()));

            // 用fork的父任务数据集
            List<Long> labelIds =  modelapplicationziplabelService.getBoundLabelIds(guidedGenerationStartDTO.getModelApplication().getId());
            dubheDataFeign.bandLabels(dubheUtils.getCurrentAuthorization(), datasetId, labelIds);
            modelGeneration.setTrainDatasetStart(datasetId);
            modelGeneration.setTrainDataset(datasetId);
            modelGeneration.setUserId((long) UserContextHolder.getUserContext().getId());

            // 把上次的挂载的数据集版本挂载到新任务
            ModelGeneration lastModelGeneration = modelGenerationService.findModelGenerationById(guidedGenerationStartDTO.getModelGenerationId());
            List<Long> newTrainDatasetVersions = new ArrayList<>(lastModelGeneration.getTrainDatasetVersions());

            modelGeneration.setTrainDatasetVersions(newTrainDatasetVersions);


            saveModelGeneration(modelGeneration);
            return new Msg<>(MsgCode.SUCCEED, modelGeneration.getId());
        } else {
            return new Msg<>(MsgCode.MODEL_GENERATION_NAME_EXIST);
        }
    }

    public Msg<Long> startGuidedGenerationParams(GuidedGenerationStartDTO guidedGenerationStartDTO) {
        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(
                guidedGenerationStartDTO.getModelGeneration().getId());

        // 计算主数据集图片数量
        Integer datasetImageCount = 0;
        try {
            DataResponseBody<Integer> countResp =
                    dubheDataFeign.getDatasetImageCount(dubheUtils.getAuthorization(),
                            modelGeneration.getTrainDatasetStart());
            if (countResp != null && countResp.getData() != null) {
                datasetImageCount = countResp.getData();
            }
        } catch (Exception e) {
            datasetImageCount = 0;
        }

        List<Long> trainDatasetVersions = modelGeneration.getTrainDatasetVersions();
        boolean hasBoundVersions = trainDatasetVersions != null && !trainDatasetVersions.isEmpty();

        // 情况1：主数据集没有图片，但已绑定版本 -> 不再发布版本，直接开始训练
        if (datasetImageCount != null && datasetImageCount == 0 && hasBoundVersions) {
            modelGeneration.setSplitSize(guidedGenerationStartDTO.getModelGeneration().getSplitSize());
            modelGenerationService.bindHardwareParams(modelGeneration.getId(), guidedGenerationStartDTO.getHardwareParamsID());
            modelGeneration.setGpuCount(guidedGenerationStartDTO.getGpuCount());
            modelGeneration.setGpuMode(guidedGenerationStartDTO.getGpuMode());
            modelGenerationService.saveModelGeneration(modelGeneration);

            startModelTrainingProcess(guidedGenerationStartDTO, modelGeneration.getId());
            return new Msg<>(MsgCode.SUCCEED, modelGeneration.getId());
        }

        // 情况2：保持原有逻辑，仍然基于主数据集发布版本再训练
        DatasetVersionCreateDTO VersionDTO = new DatasetVersionCreateDTO();
        VersionDTO.setDatasetId(modelGeneration.getTrainDatasetStart());
        // 根据标注类型和格式组合设置正确的格式
        String format = "YOLO";
        String annotationType = modelGeneration.getAnnotationType();

        if ("YOLO".equals(format) && "Segmentation".equals(annotationType)) {
            format = "Segment-YOLO";
        }

        VersionDTO.setFormat(format);

        List<Long> labelIds = modelapplicationziplabelService.getBoundLabelIds(
                guidedGenerationStartDTO.getModelApplication().getId());
        List<LabelMappingDTO> labelMappingDTOS = new LinkedList<>();
        for (Long labelId : labelIds) {
            LabelMappingDTO labelMappingDTO = new LabelMappingDTO();
            labelMappingDTO.setSourceLabelId(labelId);
            labelMappingDTO.setTargetLabelId(labelId);
            labelMappingDTOS.add(labelMappingDTO);
        }
        VersionDTO.setLabelMappings(labelMappingDTOS);
        Long VersionID = dubheDataFeign.publish(dubheUtils.getCurrentAuthorization(), VersionDTO).getData();
        modelGeneration.setSplitSize(guidedGenerationStartDTO.getModelGeneration().getSplitSize());
        modelGenerationService.bindHardwareParams(modelGeneration.getId(), guidedGenerationStartDTO.getHardwareParamsID());
        modelGeneration.setTrainDataset(VersionID);
        modelGeneration.setGpuCount(guidedGenerationStartDTO.getGpuCount());
        modelGeneration.setGpuMode(guidedGenerationStartDTO.getGpuMode());

        // 拼versionList
        if (trainDatasetVersions == null) {
            trainDatasetVersions = new ArrayList<>();
        }
        trainDatasetVersions.add(VersionID);
        modelGeneration.setTrainDatasetVersions(trainDatasetVersions);
        modelGenerationService.saveModelGeneration(modelGeneration);

        // 启动异步监听数据集发布状态
        jobTrackingExecutor.execute(() -> {
            try {
                while (true) {
                    boolean isPublishing = dubheDataFeign.isPublishingByDatasetId(
                            dubheUtils.getAuthorization(),
                            VersionDTO.getDatasetId()
                    ).getData();
                    if (!isPublishing) {
                        System.out.println("数据集发布完成，开始模型训练流程");
                        Object dataset = dubheDataFeign.get(dubheUtils.getAuthorization(),
                                VersionDTO.getDatasetId()).getData();
                        Map<String, Object> data = (Map<String, Object>) dataset;
                        dubheDataFeign.publishDatasetVersion(dubheUtils.getAuthorization(),
                                VersionDTO.getDatasetId(),
                                (String) data.get("currentVersionName"));
                        startModelTrainingProcess(guidedGenerationStartDTO, modelGeneration.getId());
                        break;
                    }
                    Thread.sleep(3000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("数据集发布监听线程被中断");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("数据集发布状态检查异常");
            }
        });

        return new Msg<>(MsgCode.SUCCEED, modelGeneration.getId());

    }

    Integer getDatasetStatus(long datasetId) {
        Object dataset = dubheDataFeign.get(dubheUtils.getAuthorization(), datasetId).getData();
        Map<String, Object> data = (Map<String, Object>) dataset;
        Integer status = (Integer) data.get("status");
        return status;
    }

    public static Map<Integer, String> convertListToMapWithStream(List<DatasetLabelInfoDTO> labelInfoDTOS) {
        // 进行空值检查，返回一个不可变的空Map，更安全
        if (labelInfoDTOS == null || labelInfoDTOS.isEmpty()) {
            return Collections.emptyMap();
        }

        return IntStream.range(0, labelInfoDTOS.size()) // 1. 创建一个从 0 到 list.size()-1 的整数流
                .boxed() // 2. 将 IntStream 转换成 Stream<Integer>，因为 toMap 需要对象类型
                .collect(Collectors.toMap(
                        Function.identity(), // 3. Key: 使用索引本身作为Key (i -> i)
                        i -> labelInfoDTOS.get(i).getLabelName()  // 4. Value: 根据索引获取列表中的对象，并提取labelName
                ));
    }

    private void startModelTrainingProcess(GuidedGenerationStartDTO dto, Long generationID) {
        transactionTemplate.execute(status -> {
            try {
                ModelGeneration generation = modelGenerationService.getModelGenerationById(generationID);

                // 重新加载实体确保在事务上下文中
                generation = entityManager.merge(generation);

                // 初始化延迟加载属性
                Hibernate.initialize(generation.getHyperParams());
                Hibernate.initialize(generation.getQuantizationParams());

                // 更新实体状态
                generation.setHyperParams(new HashMap<>(dto.getTrainHyperParams()));
                generation.setQuantizationParams(dto.getQuantizationParams());
                generation.setHardwareParams(hardwareParamsService.findHardwareParamsById(dto.getHardwareParamsID()));

                saveModelGeneration(generation);

                try {
                    // 在事务中创建Job
                    modelJobService.createJob(generation.getId(), null, 1, ModelTypeConstant.OFFICIAL);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    e.printStackTrace();
                    System.out.println("创建训练作业失败");
                }

                if (dto.getModelConvert() != null) {
                    ModelConvertDTO convertDTO = dto.getModelConvert();
                    Hibernate.initialize(convertDTO.getParams());

                    // 重新获取最新实体状态
                    ModelGeneration latestGeneration = entityManager.merge(
                            findModelGenerationByName(generation.getName())
                    );

                    ModelJob lastJob = modelJobService.getModelJobById(latestGeneration.getLatestJobId());
                    trackJobStatusAsync(lastJob, latestGeneration, convertDTO, dto.getQuantizationParams());
                }
                return null;
            } catch (HibernateException e) {
                status.setRollbackOnly();
                System.out.println("模型训练流程异常: " + e.getMessage());
                return null;
            }
        });
    }


    // 使用线程池异步跟踪 Job 状态
    public void trackJobStatusAsync(ModelJob lastJob, ModelGeneration modelGeneration, ModelConvertDTO convertDTO, Map<String, String> quantizationParams) {
        jobTrackingExecutor.execute(() -> trackJobStatus(lastJob, modelGeneration, convertDTO, quantizationParams));
    }

    // 跟踪 Job 状态的具体逻辑
    private void trackJobStatus(ModelJob lastJob, ModelGeneration modelGeneration, ModelConvertDTO convertDTO, Map<String, String> quantizationParams) {
        System.out.println("开始跟踪作业状态，Job ID: " + lastJob.getId());

        while (true) {
            int status = modelJobService.getModelJobById(lastJob.getId()).getStatus();

            if (ModelJobStateCodeConstant.TRAIN_SUCCEEDED.equals(status) || ModelJobStateCodeConstant.TRAIN_FAILED.equals(status) || ModelJobStateCodeConstant.CANCELED.equals(status)) {
                System.out.println("Job 已完成，状态: " + status);
                if (ModelJobStateCodeConstant.TRAIN_SUCCEEDED.equals(status)) {
                    String weightPath = lastJob.getWeightPath();
                    LocalDateTime curTime = LocalDateTime.now();
                    // 拼接 Job 名称
                    String jobName = weightPath
                            + "-convert-"
                            + curTime.getYear() + "-"
                            + curTime.getMonthValue() + "-"
                            + curTime.getDayOfMonth() + "-"
                            + curTime.getHour() + "-"
                            + curTime.getMinute() + "-"
                            + curTime.getSecond();

                    convertDTO.setJobName(jobName);
                    convertDTO.setOutputWeightPath(jobName);
                    convertDTO.setWeightPath(weightPath);
                    convertDTO.setGpuNum(lastJob.getGpus());
                    convertDTO.setCpuNum(lastJob.getCpus());
                    ModelGeneration modelGenerationById = getModelGenerationById(modelGeneration.getId());
                    String convertImageURL = modelGenerationById.getModelExplore().getConvertModelVersion().getUrl();
                    convertDTO.setImage(convertImageURL);
                    modelJobService.createConvertJob(modelGeneration.getId(), convertDTO, quantizationParams);
                }
                break;
            }

            try {
                Thread.sleep(5000); // 每 5 秒查询一次状态
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("状态跟踪线程被中断");
                break;
            }
        }
    }

    public Msg<String> createGuidedFork(long modelGenerationID) {
        ModelGeneration originModelGeneration = getModelGenerationById(modelGenerationID);

        if (originModelGeneration != null && originModelGeneration.getIsGuided() &&
                (originModelGeneration.getStatusForGuided() == ModelGeneration.TRAIN_SUCCESS_GUIDED
                        || originModelGeneration.getStatusForGuided() == ModelGeneration.TRAIN_CONVERT_SUCCESS_GUIDED)) {
            ModelGeneration newforkModelGeneration = new ModelGeneration();
            newforkModelGeneration.setModelApplication(originModelGeneration.getModelApplication());
            newforkModelGeneration.setDatasetSource(originModelGeneration.getDatasetSource());
            newforkModelGeneration.setIsGuided(true);
            newforkModelGeneration.setIsReuse(true);
            newforkModelGeneration.setModel(originModelGeneration.getModel());
            newforkModelGeneration.setModelExplore(originModelGeneration.getModelExplore());
            newforkModelGeneration.setSplitSize(originModelGeneration.getSplitSize());
            newforkModelGeneration.setTrainDataset(originModelGeneration.getTrainDataset());
            newforkModelGeneration.setTestDataset(originModelGeneration.getTestDataset());
            newforkModelGeneration.setValDataset(originModelGeneration.getValDataset());
            newforkModelGeneration.setUserId((long) UserContextHolder.getUserContext().getId());
            // 自动生成唯一的名称
            String originName = originModelGeneration.getName();
            String newName = generateUniqueForkName(originName);
            newforkModelGeneration.setName(newName);

            ModelConvertDTO MCDTO = new ModelConvertDTO();
            MCDTO.setParams(originModelGeneration.getModelConvert().getParams());


            GuidedGenerationStartDTO GGDTO = new GuidedGenerationStartDTO();
            GGDTO.setModelGeneration(newforkModelGeneration);

            Map<String, String> hyperParams = new HashMap<>(originModelGeneration.getHyperParams());
            GGDTO.setTrainHyperParams(hyperParams);

            GGDTO.setModelConvert(MCDTO);
            GGDTO.setModelApplication(originModelGeneration.getModelApplication());
            GGDTO.setHardwareParamsID(originModelGeneration.getHardwareParams().getId());
            GGDTO.setModelConvert(MCDTO);
            startGuidedGeneration(GGDTO);
            return new Msg<>(MsgCode.SUCCEED, "SUCCEED");
        }
        return new Msg<>(MsgCode.FAILED, "FAILED");
    }

    private String generateUniqueForkName(String originName) {
        // 查询现有的 fork 名称
        List<String> existingNames = modelGenerationRepo.findAllNamesStartingWith(originName + "-fork");

        // 找到最大后缀
        int maxSuffix = 0;
        for (String name : existingNames) {
            if (name.startsWith(originName + "-fork")) {
                String suffix = name.substring((originName + "-fork").length());
                if (suffix.matches("\\d+")) { // 检查后缀是否是数字
                    maxSuffix = Math.max(maxSuffix, Integer.parseInt(suffix));
                }
            }
        }

        // 返回下一个可用的后缀
        return originName + "-fork" + (maxSuffix + 1);
    }

    public List<ModelGeneration> findGuidedModelGenerationSupportAutoLabel(Long dataSetId) {
        // 装 是否是引导式：labelsId
        GuidedAndLabelsVO guidedAndLabels = dubheDataFeign.guidedAndLabels(dubheUtils.getAuthorization(),dataSetId).getData();
        List<LabelDTO> thisLabels = guidedAndLabels.getLabels();
        if  (thisLabels.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取数据集的标注类型，并转换为ModelGeneration中的annotationType格式
        Integer datasetAnnotateType = guidedAndLabels.getAnnotateType();
        String targetAnnotationType = convertDatasetAnnotateTypeToGenerationType(datasetAnnotateType);

        HashSet<String> thisLabelNames = new HashSet<>(thisLabels.stream().map(LabelDTO::getName).collect(Collectors.toList()));
        List<ModelGeneration> modelGenerations = new ArrayList<>();

        List<ModelGeneration> modelGenerationList = new ArrayList<>();
        modelGenerationRepo.findGuidedGenerationsForAutoLabel(targetAnnotationType).forEach(modelGeneration -> {
            // SQL已过滤：!isDelete, isGuided=true, isReuse=true, isAutoLabel=true, annotationType匹配
            // 只需要在代码中处理状态条件
            if (modelGeneration.getStatusForGuided() == ModelGeneration.TRAIN_SUCCESS_GUIDED ||
                    modelGeneration.getStatusForGuided() == ModelGeneration.TRAIN_CONVERT_SUCCESS_GUIDED) {

                // 根据isDatasetGuided分类
                if (guidedAndLabels.getIsDatasetGuided()) {
                    modelGenerationList.add(modelGeneration);
                } else {
                    modelGenerations.add(modelGeneration);
                }
            }
        });
        List<ModelGeneration> mergedModelGenerations = modelGenerations;
        // 逻辑如下：把全部要判断的任务放一个列表，远程一次性拿到它们对应的标签名，遍历标签名把符合条件的标签名同序号的任务add到结果中
        // 对应要判断的任务的顺序的数据集id
        if (!modelGenerationList.isEmpty()) {
            List<Long> datasetStartList = modelGenerationList.stream()
                    .map(ModelGeneration::getTrainDatasetStart)
                    .collect(Collectors.toList());
            List<List<String>> labelNamesList = dubheDataFeign.guidedLabelsNames(dubheUtils.getAuthorization(),datasetStartList).getData();
            List<ModelGeneration> selectedModelGenerations = IntStream.range(0, labelNamesList.size())
                    .filter(i -> thisLabelNames.containsAll(labelNamesList.get(i)))
                    .mapToObj(modelGenerationList::get)
                    .collect(Collectors.toList());
            mergedModelGenerations = Stream.concat(modelGenerations.stream(), selectedModelGenerations.stream())
                    .collect(Collectors.toList());
        }
        return mergedModelGenerations;
    }

    // 获得数据集自动标注的所有标签
    public List<String> getLabelNamesByDataSetId(Long dataSetId) {
        // 1. 获取ModelGeneration列表
        List<ModelGeneration> modelGenerations = findGuidedModelGenerationSupportAutoLabel(dataSetId);

        if (modelGenerations.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集所有不为null的dataset ID
        List<Long> datasetIds = modelGenerations.stream()
                .map(ModelGeneration::getTrainDatasetStart)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 3. 如果没有有效的dataset ID，返回空列表
        if (datasetIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 批量获取标签名
        List<List<String>> allLabelNames = dubheDataFeign.guidedLabelsNames(
                dubheUtils.getAuthorization(), datasetIds
        ).getData();

        if (allLabelNames == null || allLabelNames.isEmpty()) {
            return Collections.emptyList();
        }

        // 5. 合并并返回所有标签名
        return allLabelNames.stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 将数据集的annotateType转换为ModelGeneration的annotationType
     * @param datasetAnnotateType 数据集标注类型：102-目标检测，103-语义分割
     * @return ModelGeneration的标注类型：Detection或Segmentation
     */
    private String convertDatasetAnnotateTypeToGenerationType(Integer datasetAnnotateType) {
        if (datasetAnnotateType == null) {
            return ModelGeneration.ANNOTATION_TYPE_DETECTION; // 默认为目标检测
        }
        switch (datasetAnnotateType) {
            case 103: // 语义分割
                return ModelGeneration.ANNOTATION_TYPE_SEGMENTATION;
            case 102: // 目标检测
            default:
                return ModelGeneration.ANNOTATION_TYPE_DETECTION;
        }
    }

    public List<ModelGeneration> findModelGenerationSupportAutoLabel(Long datasetId) {
        // 1. 获取当前用户信息 (这部分逻辑不变)
        Long currentUserId = Long.valueOf(UserContextHolder.getUserContext().getId());
        String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
        boolean isAdmin = "管理员".equals(roleName) || "管理人员".equals(roleName);

        // 2. 直接调用优化后的Repository方法，将所有过滤逻辑交给数据库
        List<ModelGeneration> allGenerations = modelGenerationRepo.findModelGenerationSupportAutoLabel(currentUserId, isAdmin);

        // 3. 如果提供了datasetId，则根据数据集的标注类型进行过滤
        if (datasetId != null) {
            try {
                // 获取数据集的标注类型
                GuidedAndLabelsVO guidedAndLabels = dubheDataFeign.guidedAndLabels(dubheUtils.getAuthorization(), datasetId).getData();
                Integer datasetAnnotateType = guidedAndLabels.getAnnotateType();
                String targetAnnotationType = convertDatasetAnnotateTypeToGenerationType(datasetAnnotateType);

                // 过滤出与数据集标注类型相同的训练任务
                return allGenerations.stream()
                        .filter(generation -> {
                            String generationAnnotationType = generation.getAnnotationType();
                            return generationAnnotationType != null && generationAnnotationType.equals(targetAnnotationType);
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("获取数据集标注类型失败，datasetId: {}, error: {}", datasetId, e.getMessage());
                // 如果获取失败，返回所有结果（保持向后兼容）
                return allGenerations;
            }
        }

        // 如果没有提供datasetId，返回所有结果（保持向后兼容）
        return allGenerations;
    }

    public List<ModelGeneration> findModelGenerationSupportAutoLabelByApplication(String applicationName, String deviceFirmware) {
        ModelApplication modelApplication = requireModelApplication(applicationName, deviceFirmware);
        String targetAnnotationType = resolveApplicationAnnotationType(modelApplication);
        return findModelGenerationSupportAutoLabel(null).stream()
                .filter(generation -> Objects.equals(generation.getAnnotationType(), targetAnnotationType))
                .collect(Collectors.toList());
    }

    public Page<GuidedTrainingInfoCardDTO> findGuidedTrainingInfoCardsByApplication(
            String applicationName,
            String deviceFirmware,
            int page,
            int pageSize,
            String name,
            String labels
    ) {
        ModelApplication modelApplication = requireModelApplication(applicationName, deviceFirmware);
        String targetAnnotationType = resolveApplicationAnnotationType(modelApplication);
        List<String> requiredLabelNames = getBoundLabelNames(modelApplication.getId());

        List<ModelGeneration> allSupportedGenerations = modelGenerationRepo.findGuidedGenerationsForAutoLabel(targetAnnotationType)
                .stream()
                .filter(generation -> generation.getStatusForGuided() == ModelGeneration.TRAIN_SUCCESS_GUIDED
                        || generation.getStatusForGuided() == ModelGeneration.TRAIN_CONVERT_SUCCESS_GUIDED)
                .filter(generation -> hasAnyMatchingLabel(generation.getSelectedLabels(), requiredLabelNames))
                .collect(Collectors.toList());

        if (name != null && !name.trim().isEmpty()) {
            String searchName = name.trim().toLowerCase();
            allSupportedGenerations = allSupportedGenerations.stream()
                    .filter(generation -> generation.getName() != null
                            && generation.getName().toLowerCase().contains(searchName))
                    .collect(Collectors.toList());
        }

        if (labels != null && !labels.trim().isEmpty()) {
            String searchLabel = labels.trim().toLowerCase();
            allSupportedGenerations = allSupportedGenerations.stream()
                    .filter(generation -> generation.getSelectedLabels() != null
                            && generation.getSelectedLabels().values().stream()
                            .filter(Objects::nonNull)
                            .anyMatch(label -> label.toLowerCase().contains(searchLabel)))
                    .collect(Collectors.toList());
        }

        allSupportedGenerations.sort(Comparator.comparing(ModelGeneration::getId).reversed());

        Pageable pageable = PageRequest.of(page, pageSize);
        int start = (int) pageable.getOffset();
        if (start >= allSupportedGenerations.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, allSupportedGenerations.size());
        }
        int end = Math.min(start + pageSize, allSupportedGenerations.size());
        List<GuidedTrainingInfoCardDTO> dtoList = allSupportedGenerations.subList(start, end).stream()
                .map(generation -> {
                    GuidedTrainingInfoCardDTO cardDTO = new GuidedTrainingInfoCardDTO();
                    cardDTO.setGenerationId(generation.getId());
                    cardDTO.setGenerationName(generation.getName());
                    try {
                        cardDTO.setModelInfo(modelJobService.getJobAccLossAndImgCountsByGuidedGenerationId(generation.getId(), false).getPayload());
                    } catch (Exception e) {
                        cardDTO.setModelInfo(new JobPrecisionInfoDTO(-1.0, -1.0, 0));
                    }
                    cardDTO.setLabelNames(generation.getSelectedLabels() == null
                            ? Collections.emptyList()
                            : new ArrayList<>(generation.getSelectedLabels().values()));
                    return cardDTO;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, allSupportedGenerations.size());
    }

    private ModelApplication requireModelApplication(String applicationName, String deviceFirmware) {
        ModelApplication modelApplication = modelApplicationRepo.findByApplicationNameAndDeviceAndFirmware(applicationName, deviceFirmware);
        if (modelApplication == null) {
            throw new IllegalArgumentException("未找到匹配的 ModelApplication: applicationName=" + applicationName + ", deviceFirmware=" + deviceFirmware);
        }
        return modelApplication;
    }

    private String resolveApplicationAnnotationType(ModelApplication modelApplication) {
        if (modelApplication != null && modelApplication.getModel() != null
                && modelApplication.getModel().getTrainModelVersion() != null
                && modelApplication.getModel().getTrainModelVersion().getAnnotationType() != null) {
            return modelApplication.getModel().getTrainModelVersion().getAnnotationType();
        }
        return ModelGeneration.ANNOTATION_TYPE_DETECTION;
    }

    private List<String> getBoundLabelNames(Long modelApplicationId) {
        List<Long> labelIds = modelapplicationziplabelService.getBoundLabelIds(modelApplicationId);
        if (labelIds == null || labelIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<LabelDTO> labels = dubheDataFeign.findLabelByIds(dubheUtils.getAuthorization(), labelIds).getData();
        if (labels == null) {
            return Collections.emptyList();
        }
        return labels.stream()
                .map(LabelDTO::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean hasAnyMatchingLabel(Map<Integer, String> candidateLabels, List<String> requiredLabelNames) {
        if (requiredLabelNames == null || requiredLabelNames.isEmpty()) {
            return true;
        }
        if (candidateLabels == null || candidateLabels.isEmpty()) {
            return false;
        }
        Set<String> candidateSet = candidateLabels.values().stream()
                .filter(Objects::nonNull)
                .map(label -> label.trim().toLowerCase())
                .collect(Collectors.toSet());
        return requiredLabelNames.stream()
                .filter(Objects::nonNull)
                .map(label -> label.trim().toLowerCase())
                .anyMatch(candidateSet::contains);
    }

    public String generateGuidedModelGenerationNameByApplicationName(String applicationName) {
        List<String> existingNames = modelGenerationRepo.findAllNamesStartingWith(applicationName + "-");

        int maxSuffix = 0;
        for (String name : existingNames) {
            if (name.startsWith(applicationName + "-")) {
                String suffix = name.substring((applicationName + "-").length());
                // 修改正则表达式匹配前导数字部分
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^\\d+").matcher(suffix);
                if (matcher.find()) {
                    try {
                        int current = Integer.parseInt(matcher.group());
                        maxSuffix = Math.max(maxSuffix, current);
                    } catch (NumberFormatException e) {
                        // 忽略格式错误的后缀
                    }
                }
            }
        }

        return applicationName + "-" + String.format("%02d", maxSuffix + 1);
    }

    public List<ModelGenerationBindInfo> checkBindRelationWithGeneration(
            @NotNull(message = "id不能为空") Long[] datasetIds
    ) {
        if (datasetIds == null || datasetIds.length == 0) {
            return Collections.emptyList();
        }

        List<Long> idList = Arrays.asList(datasetIds);
        List<ModelGenerationBindInfo> results = new ArrayList<>();

        List<DatasetVersionInfoVO> infoVOList = dubheDataFeign.getVersionIdsWithDatasetInfo(dubheUtils.getAuthorization(), idList).getData();

        infoVOList.forEach(infoVO -> {
            List<ModelGeneration> generations = modelGenerationRepo.findByTrainDatasetVersionsContains(infoVO.getDatasetVersionIds());
            if (generations.isEmpty()) {
                // 如果查出来的训练任务为空, 说明数据集下的版本没被任务关联, 则可以正常删除
                return;
            }
            generations.forEach(generation -> {
                if (generation.getIsDelete()) {
                    return;
                }
                results.add(
                        ModelGenerationBindInfo.builder()
                                .modelGenerationId(generation.getId())
                                .modelGenerationName(generation.getName())
                                .datasetId(infoVO.getDatasetId())
                                .datasetName(infoVO.getDatasetName())
                                .isGuided(generation.getIsGuided())
                                .build()
                );
            });
        });

        return results;
    }

    public Page<GuidedTrainingInfoCardDTO> findGuidedTrainingInfoCards(Long dataSetId, int page, int pageSize, String name, String labels) {
        // 1. 获取所有符合条件的引导式训练（复用您的现有复杂逻辑）
        List<ModelGeneration> allSupportedGenerations = this.findGuidedModelGenerationSupportAutoLabel(dataSetId);
        // 2. 如果提供了名称搜索条件，进行过滤
        if (name != null && !name.trim().isEmpty()) {
            String searchName = name.trim().toLowerCase();
            allSupportedGenerations = allSupportedGenerations.stream()
                    .filter(generation -> generation.getName() != null &&
                            generation.getName().toLowerCase().contains(searchName))
                    .collect(Collectors.toList());
        }

        // 3. 如果提供了标签搜索条件，进行过滤（筛选包含指定标签的记录）
        if (labels != null && !labels.trim().isEmpty()) {
            String searchLabel = labels.trim().toLowerCase();

            allSupportedGenerations = allSupportedGenerations.stream()
                    .filter(generation -> {
                        // 获取标签列表
                        Collection<String> labelValues = generation.getSelectedLabels().values();
                        if (labelValues == null || labelValues.isEmpty()) {
                            return false;
                        }
                        // 检查是否包含搜索的标签（不区分大小写）
                        return labelValues.stream()
                                .anyMatch(label -> label != null && label.toLowerCase().contains(searchLabel));
                    })
                    .collect(Collectors.toList());
        }

        // 按ID倒序排序
        allSupportedGenerations.sort(Comparator.comparing(ModelGeneration::getId).reversed());

        // 3. 手动进行内存分页
        Pageable pageable = PageRequest.of(page, pageSize);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageSize), allSupportedGenerations.size());

        // 如果起始位置超出列表大小，或分页内容为空，直接返回
        if (start >= allSupportedGenerations.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, allSupportedGenerations.size());
        }

        List<ModelGeneration> pageContent = allSupportedGenerations.subList(start, end);
        // 判断被标注的数据集是否是引导式数据集
        Boolean needToFilter = false;
        try {
            GuidedAndLabelsVO guidedAndLabels = dubheDataFeign.guidedAndLabels(dubheUtils.getAuthorization(), dataSetId).getData();
            needToFilter = guidedAndLabels.getIsDatasetGuided();
        } catch (Exception e) {
            log.error("Failed to get dataset guided info for datasetId: {}", dataSetId, e);
            return null;
        }

        // 5. 将分页后的 List<ModelGeneration> 转换为 List<GuidedTrainingInfoCardDTO>
        Boolean finalNeedToFilter = needToFilter;
        List<GuidedTrainingInfoCardDTO> dtoList = pageContent.stream().map(generation -> {
            GuidedTrainingInfoCardDTO cardDTO = new GuidedTrainingInfoCardDTO();
            cardDTO.setGenerationId(generation.getId());
            cardDTO.setGenerationName(generation.getName());

            // 6. 填充模型性能信息 (这个调用如果也慢，可以采用同样的方法进行批量优化)
            try {
                JobPrecisionInfoDTO precisionInfo = modelJobService.getJobAccLossAndImgCountsByGuidedGenerationId(generation.getId(), finalNeedToFilter).getPayload();
                cardDTO.setModelInfo(precisionInfo);
            } catch (Exception e) {
                cardDTO.setModelInfo(new JobPrecisionInfoDTO(-1.0, -1.0, 0));
            }

            // 7. 从预先获取的Map中查找标签信息
            cardDTO.setLabelNames(new ArrayList<>(generation.getSelectedLabels().values()));

            return cardDTO;
        }).collect(Collectors.toList());

        // 8. 构造并返回 Page 对象
        return new PageImpl<>(dtoList, pageable, allSupportedGenerations.size());
    }

    public void bindDatasetVersions(List<Long> datasetVersionIds, Long modelGenerationId) {
        ModelGeneration generation = modelGenerationService.findModelGenerationById(modelGenerationId);

        List<Long> mergedVersions = datasetVersionIds.stream()
                .distinct()
                .collect(Collectors.toList());

        generation.setTrainDatasetVersions(mergedVersions);
        modelGenerationService.saveModelGeneration(generation);
    }

    public List<ModelGenerationBoundVersionDTO> getModelGenerationBoundVersions(Long modelGenerationId) {
        ModelGeneration generation = modelGenerationService.findModelGenerationById(modelGenerationId);
        if (generation == null || generation.getTrainDatasetVersions() == null || generation.getTrainDatasetVersions().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> labelNames = new ArrayList<>();
        if (generation.getIsGuided()) {
            List<LabelDTO> guidedGenerationLabels = dubheDataFeign.findLabelByIds(dubheUtils.getAuthorization(), modelapplicationziplabelService.getBoundLabelIds(generation.getModelApplication().getId())).getData();
            labelNames = guidedGenerationLabels.stream().map(LabelDTO::getName).collect(Collectors.toList());
        }else {
            labelNames = new ArrayList<>(generation.getSelectedLabels().values());
        }
        // 1. 根据ID列表获取所有相关的数据集版本VO
        List<Long> trainDatasetVersionIds = generation.getTrainDatasetVersions();
        List<DatasetVersionDetailVO> versionLabelCountMapVos = dubheDataFeign.batchGetVersionsLabelCountMap(dubheUtils.getAuthorization(), trainDatasetVersionIds, labelNames).getData();
        DataResponseBody<List<DatasetVersionVO>> datasetVersionsByIdsRes = dubheDataFeign.findDatasetVersionsByIds(dubheUtils.getAuthorization(), trainDatasetVersionIds);
        if (datasetVersionsByIdsRes == null || datasetVersionsByIdsRes.getData() == null || datasetVersionsByIdsRes.getData().isEmpty()) {
            return Collections.emptyList();
        }
        List<DatasetVersionVO> allDatasetVersions = datasetVersionsByIdsRes.getData();

        // 获取标签信息
        List<LabelDTO> guidedGenerationLabels = dubheDataFeign.findLabelByIds(dubheUtils.getAuthorization(), modelapplicationziplabelService.getBoundLabelIds(generation.getModelApplication().getId())).getData();
        List<String> guidedGenerationLabelNames = guidedGenerationLabels.stream().map(LabelDTO::getName).collect(Collectors.toList());
        Set<String> lowerCaseGuidedLabelNames = guidedGenerationLabelNames.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Map<Long, Map<String, Integer>> labelCountMapById = versionLabelCountMapVos.stream()
                .collect(Collectors.toMap(
                        DatasetVersionDetailVO::getId,
                        vo -> vo.getLabelCountMap().entrySet().stream()
                                .filter(entry -> lowerCaseGuidedLabelNames.contains(entry.getKey().toLowerCase()))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue
                                )),
                        (existing, replacement) -> existing
                ));
        Map<Long, Integer> imgCountMap =  versionLabelCountMapVos.stream()
                .collect(Collectors.toMap(
                        DatasetVersionDetailVO::getId,
                        DatasetVersionDetailVO::getImportableImageCount,
                        (existing, replacement) -> existing
                ));
        Map<Long, Long> fileSizeMap = versionLabelCountMapVos.stream()
                .collect(Collectors.toMap(
                        DatasetVersionDetailVO::getId,
                        DatasetVersionDetailVO::getTotalFileSize,
                        (existing, replacement) -> existing
                ));

        allDatasetVersions.forEach(versionVO -> {
            Map<String, Integer> countMap = labelCountMapById.get(versionVO.getId());
            Integer currentImgCount = imgCountMap.get(versionVO.getId());
            Long currentVersionFileSize = fileSizeMap.get(versionVO.getId());

            versionVO.setLabelCountMap(countMap != null ? countMap : Collections.emptyMap());
            versionVO.setImportableImageCount(currentImgCount);
            versionVO.setTotalFileSize(currentVersionFileSize);
        });

        // 2. 将获取到的版本按其所属的 datasetId 进行分组，方便后续查找
        Map<Long, List<DatasetVersionVO>> versionsGroupedByDatasetId = allDatasetVersions.stream()
                .collect(Collectors.groupingBy(DatasetVersionVO::getDatasetId));

        List<Long> datasetIds = new ArrayList<>(versionsGroupedByDatasetId.keySet());

        // 3. 直接获取数据集与数据集组的映射关系
        DataResponseBody<List<DatasetGroupMappingVO>> response = dubheDataFeign.getDatasetGroupsMapByDatasetIds(dubheUtils.getAuthorization(), datasetIds);
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        List<DatasetGroupMappingVO> groupMappings = response.getData();

        // 4. 组装最终结果
        List<ModelGenerationBoundVersionDTO> resultList = new ArrayList<>();

        // 遍历新的 DTO 列表
        for (DatasetGroupMappingVO mapping : groupMappings) {
            DatasetGroupVO currentGroup = mapping.getGroup();
            List<DatasetVO> datasetsInGroup = mapping.getDatasets();

            for (DatasetVO datasetInGroup : datasetsInGroup) {
                Long currentDatasetId = datasetInGroup.getId();

                if (versionsGroupedByDatasetId.containsKey(currentDatasetId)) {
                    List<DatasetVersionVO> relevantVersions = versionsGroupedByDatasetId.get(currentDatasetId);

                    ModelGenerationBoundVersionDTO dto = new ModelGenerationBoundVersionDTO(currentGroup, datasetInGroup, relevantVersions);
                    resultList.add(dto);
                }
            }
        }

        return resultList;
    }


    public List<ModelGenerationBoundVersionDTO> getModelGenerationBoundVersionsWithoutLabelFilter(Long modelGenerationId) {
        ModelGeneration generation = modelGenerationService.findModelGenerationById(modelGenerationId);
        if (generation == null || generation.getTrainDatasetVersions() == null || generation.getTrainDatasetVersions().isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 根据ID列表获取所有相关的数据集版本VO
        List<Long> trainDatasetVersionIds = generation.getTrainDatasetVersions();
        List<DatasetVersionDetailVO> versionLabelCountMapVos = dubheDataFeign.batchGetVersionsLabelCountMap(dubheUtils.getAuthorization(), trainDatasetVersionIds, new ArrayList<>(generation.getSelectedLabels().values())).getData();
        DataResponseBody<List<DatasetVersionVO>> datasetVersionsByIdsRes = dubheDataFeign.findDatasetVersionsByIds(dubheUtils.getAuthorization(), trainDatasetVersionIds);
        if (datasetVersionsByIdsRes == null || datasetVersionsByIdsRes.getData() == null || datasetVersionsByIdsRes.getData().isEmpty()) {
            return Collections.emptyList();
        }
        List<DatasetVersionVO> allDatasetVersions = datasetVersionsByIdsRes.getData();

        Map<Long, Map<String, Integer>> labelCountMapById = versionLabelCountMapVos.stream()
                .collect(Collectors.toMap(
                        DatasetVersionDetailVO::getId,
                        DatasetVersionDetailVO::getLabelCountMap,
                        (existing, replacement) -> existing
                ));
        Map<Long, Integer> imgCountMap =  versionLabelCountMapVos.stream()
                .collect(Collectors.toMap(
                        DatasetVersionDetailVO::getId,
                        DatasetVersionDetailVO::getImportableImageCount,
                        (existing, replacement) -> existing
                ));
        Map<Long, Long> fileSizeMap = versionLabelCountMapVos.stream()
                .collect(Collectors.toMap(
                        DatasetVersionDetailVO::getId,
                        DatasetVersionDetailVO::getTotalFileSize,
                        (existing, replacement) -> existing
                ));

        allDatasetVersions.forEach(versionVO -> {
            Map<String, Integer> countMap = labelCountMapById.get(versionVO.getId());
            Integer currentImgCount = imgCountMap.get(versionVO.getId());
            Long currentVersionFileSize = fileSizeMap.get(versionVO.getId());

            versionVO.setLabelCountMap(countMap != null ? countMap : Collections.emptyMap());
            versionVO.setImportableImageCount(currentImgCount);
            versionVO.setTotalFileSize(currentVersionFileSize);
        });

        // 2. 将获取到的版本按其所属的 datasetId 进行分组，方便后续查找
        Map<Long, List<DatasetVersionVO>> versionsGroupedByDatasetId = allDatasetVersions.stream()
                .collect(Collectors.groupingBy(DatasetVersionVO::getDatasetId));

        List<Long> datasetIds = new ArrayList<>(versionsGroupedByDatasetId.keySet());

        // 3. 直接获取数据集与数据集组的映射关系
        DataResponseBody<List<DatasetGroupMappingVO>> response = dubheDataFeign.getDatasetGroupsMapByDatasetIds(dubheUtils.getAuthorization(), datasetIds);
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        List<DatasetGroupMappingVO> groupMappings = response.getData();

        // 4. 组装最终结果
        List<ModelGenerationBoundVersionDTO> resultList = new ArrayList<>();

        // 遍历新的 DTO 列表
        for (DatasetGroupMappingVO mapping : groupMappings) {
            DatasetGroupVO currentGroup = mapping.getGroup();
            List<DatasetVO> datasetsInGroup = mapping.getDatasets();

            for (DatasetVO datasetInGroup : datasetsInGroup) {
                Long currentDatasetId = datasetInGroup.getId();

                if (versionsGroupedByDatasetId.containsKey(currentDatasetId)) {
                    List<DatasetVersionVO> relevantVersions = versionsGroupedByDatasetId.get(currentDatasetId);

                    ModelGenerationBoundVersionDTO dto = new ModelGenerationBoundVersionDTO(currentGroup, datasetInGroup, relevantVersions);
                    resultList.add(dto);
                }
            }
        }

        return resultList;
    }

    public Msg<ModelGenerationOverviewDTO> getModelGenerationOverview(Long modelGenerationId) {
        ModelGeneration generation = modelGenerationService.getModelGenerationById(modelGenerationId);
        if (generation == null) {
            return new Msg<>(MsgCode.FAILED, null);
        }
        // 获取标签信息
        ModelApplication modelApplication = generation.getModelApplication();
        List<Long> labelIds = modelapplicationziplabelService.getBoundLabelIds(modelApplication.getId());
        List<LabelDTO> labelDTOs = dubheDataFeign.findLabelByIds(dubheUtils.getAuthorization(), labelIds).getData();
        labelDTOs.sort(Comparator.comparingLong(LabelDTO::getId));

        return new Msg<>(
                MsgCode.SUCCEED,
                ModelGenerationOverviewDTO
                        .builder()
                        .boundVersionsCount(generation.getTrainDatasetVersions().size())
                        .labels(labelDTOs).build()
        );
    }


    public List<ModelGenerationBindInfo> checkBindRelationWithDatasetVersion(
            @NotNull(message = "versionId不能为空") Long versionId
    ) {
        if (versionId == null) {
            return Collections.emptyList();
        }

        List<ModelGenerationBindInfo> results = new ArrayList<>();

        // 直接通过SQL查询绑定了该版本的所有任务
        List<ModelGeneration> generations =
                modelGenerationRepo.findByTrainDatasetVersionsContainsAndIsDeleteFalse(versionId);

        for (ModelGeneration generation : generations) {
            // 对于非引导式任务，直接视为被绑定
            if (Boolean.FALSE.equals(generation.getIsGuided())) {
                results.add(buildBindInfo(generation));
                continue;
            }

            // 对于引导式任务，只有处于特定状态才视为被绑定
            Integer statusForGuided = generation.getStatusForGuided();
            if (Objects.equals(statusForGuided, ModelGeneration.NOT_ACTIVE_GUIDED) ||
                    Objects.equals(statusForGuided, ModelGeneration.TRAINING_GUIDED)) {
                results.add(buildBindInfo(generation));
            }
        }

        return results;
    }

    /**
     * 根据应用ID查询所有绑定的生产任务（未删除）
     *
     * @param modelApplicationId 应用ID
     * @return 生产任务列表
     */
    public List<ModelGeneration> findByModelApplicationId(Long modelApplicationId) {
        if (modelApplicationId == null) {
            return Collections.emptyList();
        }
        return modelGenerationRepo.findByModelApplicationIdAndIsDeleteFalse(modelApplicationId);
    }

    /**
     * 找出阻塞取消发布的生产任务：状态处于数据准备中的任务
     *
     * @param modelApplicationId 应用ID
     * @return 阻塞任务列表
     */
    public List<ModelGeneration> findBlockingGenerationsForCancelRelease(Long modelApplicationId) {
        List<ModelGeneration> generations = findByModelApplicationId(modelApplicationId);
        if (generations == null || generations.isEmpty()) {
            return Collections.emptyList();
        }
        return generations.stream()
                .filter(generation -> Objects.equals(generation.getStatusNew(), ModelGeneration.NOT_ACTIVE))
                .collect(Collectors.toList());
    }

    private ModelGenerationBindInfo buildBindInfo(ModelGeneration generation) {
        return ModelGenerationBindInfo.builder()
                .modelGenerationId(generation.getId())
                .modelGenerationName(generation.getName())
                .datasetId(generation.getTrainDatasetStart())
                .isGuided(generation.getIsGuided())
                .build();
    }

    public long  countGenerationWithNoDeleted(){
        return modelGenerationRepo.countGenerationWithNoDeleted();
    }

}
