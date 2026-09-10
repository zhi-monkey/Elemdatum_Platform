package org.dlut.adv.mineai.model.service;


import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.utils.FieldRename;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheUserFeign;
import org.dlut.adv.mineai.model.domain.entity.ModelVersionDefaultLabels;
import org.dlut.adv.mineai.model.kubernetes.controller.DeploymentController;
import org.dlut.adv.mineai.model.kubernetes.service.DeploymentService;
import org.dlut.adv.mineai.model.repository.ModelDeploymentRepo;
import org.dlut.adv.mineai.model.repository.ModelMineServiceRepo;
import org.dlut.adv.mineai.model.repository.ModelVersionDefaultLabelsRepo;
import org.dlut.adv.mineai.model.repository.ModelVersionRepo;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sunzhen
 */
@Service
public class ModelVersionService {

    @Value("${kubernetes.config}")
    private String k8sConfig;

    @Resource
    ModelVersionRepo modelVersionRepo;

    @Resource
    ModelVersionDefaultLabelsRepo modelVersionDefaultLabelsRepo;

    @Resource
    ModelApplicationService modelApplicationService;

    @Resource
    ModelJobService modelJobService;

    @Resource
    DeploymentService deploymentService;

    @Resource
    ModelDeploymentRepo modelDeploymentRepo;

    @Resource
    ModelMineServiceService modelMineServiceService;

    @Resource
    ModelMonitorService modelMonitorService;

    @Resource
    ModelMineServiceRepo modelMineServiceRepo;

    @Resource
    ModelDeploymentService modelDeploymentService;

    // @Resource
    // private ModelMonitorService modelMonitorService;

    @Resource
    private DeploymentController deploymentController;

    @Resource
    private ModelConfigService modelConfigService;

    @Resource
    DubheUserFeign dubheUserFeign;

    @Resource
    private DubheUtils dubheUtils;


    @Value("${zlm.rtsp.prefix}")
    private String rtspPrefix;


    public List<ModelVersion> getModelVersions() {
        // 查询所有 ModelVersion，并确保 chip 已经加载
        return modelVersionRepo.findAll();
    }

    FieldRename fieldRename = new FieldRename();

    /*public Page<ModelVersion> getVagueModelPage(Pageable pageable, String vagueInfo,String chipType) {
        return modelVersionRepo.findAll(new Specification<ModelVersion>() {
            @Override
            public Predicate toPredicate(Root<ModelVersion> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                // Join chip to ensure it's loaded
                Join<ModelVersion, Chip> chipJoin = root.join("chip", JoinType.LEFT); // LEFT JOIN chip

                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelVersion.NOT_DELETE));
                    predicates.add(cb.or(cb.like(root.<String>get("showName"), "%" + vagueInfo + "%"), cb.like(root.<String>get("description"), "%" + vagueInfo + "%")));
                }
                // 如果提供了 chipType，则对 chipType 进行查询
                if (StringUtils.isNotEmpty(chipType)) {
                    predicates.add(cb.like(chipJoin.get("chipType"), "%" + chipType + "%")); // 动态查询 chipType
                }
                predicates.add(cb.notLike(root.get("name"), "%-copy"));
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }*/

    public Page<ModelVersion> getVagueModelPage(Pageable pageable, String vagueInfo, String chipType) {
        return modelVersionRepo.findAll(new Specification<ModelVersion>() {
            @Override
            public Predicate toPredicate(Root<ModelVersion> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();

                // Join chip to ensure it's loaded
                Join<ModelVersion, Chip> chipJoin = root.join("chip", JoinType.LEFT); // LEFT JOIN chip

                // 动态查询: isDelete字段为未删除状态
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelVersion.NOT_DELETE));
                predicates.add(cb.notLike(root.get("name"), "%-copy"));

                // 如果 vagueInfo 不为空，对 showName、description 和 chipType 进行模糊查询
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    Predicate showNamePredicate = cb.like(root.<String>get("showName"), "%" + vagueInfo + "%");
                    Predicate descriptionPredicate = cb.like(root.<String>get("description"), "%" + vagueInfo + "%");
                    Predicate levelPredicate = cb.like(root.<String>get("level"), "%" + vagueInfo + "%");
                    Predicate chipTypePredicate = cb.like(chipJoin.get("chipType"), "%" + vagueInfo + "%"); // 针对 chipType 进行模糊查询
                    // 将三个模糊查询条件合并为 OR 条件
                    predicates.add(cb.or(showNamePredicate, descriptionPredicate, chipTypePredicate, levelPredicate));
                }

                // 如果提供了具体的 chipType，也对 chipType 进行精确查询
                if (StringUtils.isNotEmpty(chipType)) {
                    predicates.add(cb.like(chipJoin.get("chipType"), "%" + chipType + "%")); // 动态查询 chipType
                }
                String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    predicates.add(cb.equal(root.get("userId"), UserContextHolder.getUserContext().getId()));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return cq.getRestriction();
            }
        }, pageable);
    }


    public List<ModelVersion> getModelVersion() {
        return modelVersionRepo.findAll();
    }


    /**
     * 删除版本
     */
    public boolean deleteModelVersion(ModelVersion modelVersion) {
        ModelVersion modelVersionInDatabase = modelVersionRepo.findModelVersionById(modelVersion.getId());
        if (isVersionExist(modelVersionInDatabase)) {
            // 查找modelVersion中除id外所有@Unique的属性
            List<String> uniqueFieldListExceptId = fieldRename.getUniqueFieldListExceptId(modelVersion.getClass());
            for (String uniqueFiled : uniqueFieldListExceptId) {
                try {
                    // 获取unique字段的值
                    String uniqueValue = (String) PropertyUtils.getProperty(modelVersion, uniqueFiled);
                    // 根据 uniqueFiled 调用对应repo查询方法
                    Method method = ModelVersionRepo.class.getMethod("findModelVersionListBy" + uniqueFiled.substring(0, 1).toUpperCase() + uniqueFiled.substring(1) + "Like", String.class, int.class);

                    //   Method method = ModelRepo.class.getMethod("findModelBy" + uniqueFiled.substring(0, 1).toUpperCase() + uniqueFiled.substring(1) + "LikeAndIsDelete", String.class, int.class);
                    List<ModelVersion> modelVersionListByUniqueFiled = (List<ModelVersion>) method.invoke(modelVersionRepo, uniqueValue + "%", ModelVersion.DELETE);
                    // 获取monitorProperty的值List
                    List<String> modelVersionFiledList = new ArrayList<>();
                    for (ModelVersion modelVersion1 : modelVersionListByUniqueFiled) {
                        modelVersionFiledList.add((String) PropertyUtils.getProperty(modelVersion1, uniqueFiled));
                    }
                    // 重命名unique字段的值
                    String newName = fieldRename.renameProperty(uniqueValue, modelVersionFiledList);
                    PropertyUtils.setProperty(modelVersion, uniqueFiled, newName);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            modelVersion.setIsDelete(ModelVersion.DELETE);
            modelVersionRepo.save(modelVersion);
            return true;
        }
        return false;
    }

    /**
     * 用于动态分页查找
     */
    public Page<ModelVersion> dynamicFindModelVersionPage(Pageable pageable, ModelVersion modelVersion, String chipType, String userName) {
        return modelVersionRepo.findAll(new Specification<ModelVersion>() {
            @Override
            public javax.persistence.criteria.Predicate toPredicate(Root<ModelVersion> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();

                // Join chip to ensure it's loaded
                Join<ModelVersion, Chip> chipJoin = root.join("chip", JoinType.LEFT); // LEFT JOIN chip

                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelVersion.NOT_DELETE));
                predicates.add(cb.notLike(root.get("name"), "%-copy"));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelVersion.getShowName())
                        && StringUtils.isBlank(modelVersion.getDescription()) && StringUtils.isBlank(userName);
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelVersion.getShowName())) {
                        predicates.add(cb.like(root.<String>get("showName"), "%" + modelVersion.getShowName() + "%"));
                    }
                    //根据描述查询
                    if (StringUtils.isNotBlank(modelVersion.getDescription())) {
                        predicates.add(cb.like(root.<String>get("description"), "%" + modelVersion.getDescription() + "%"));
                    }
                    // 根据userName查询
                    if (StringUtils.isNotBlank(userName)) {
                        List<Long> ids = dubheUserFeign.findIdsByUsernameLike(dubheUtils.getAuthorization(), userName).getData();
                        // 需要关联User表进行查询
                        if (ids != null && !ids.isEmpty()) {
                            predicates.add(root.get("userId").in(ids));
                        } else {
                            // 如果没查到任何用户，返回空结果（避免查询所有记录）
                            predicates.add(cb.equal(root.get("userId"), -1L));
                        }
                    }
                }
                // 如果提供了 chipType，则对 chipType 进行查询
                if (StringUtils.isNotEmpty(chipType)) {
                    predicates.add(cb.like(chipJoin.get("chipType"), "%" + chipType + "%")); // 动态查询 chipType
                }
                String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    predicates.add(cb.equal(root.get("userId"), UserContextHolder.getUserContext().getId()));
                }
                cq.where(predicates.toArray(new javax.persistence.criteria.Predicate[predicates.size()]));
                return cq.getRestriction();
            }
        }, pageable);
    }


    /**
     * 用于动态分页查找私有镜像
     */
    /* public Page<ModelVersion> dynamicFindModelVersionPagePrivate(Pageable pageable, ModelVersion modelVersion, Integer currentUserId) {*/
    public Page<ModelVersion> dynamicFindModelVersionPagePrivate(Pageable pageable, ModelVersion modelVersion, Integer currentUserId, String chipType) {
        return modelVersionRepo.findAll(new Specification<ModelVersion>() {
            @Override
            public javax.persistence.criteria.Predicate toPredicate(Root<ModelVersion> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();

                // Join chip to ensure it's loaded
                Join<ModelVersion, Chip> chipJoin = root.join("chip", JoinType.LEFT); // LEFT JOIN chip
                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelVersion.NOT_DELETE));
                predicates.add(cb.notLike(root.get("name"), "%-copy"));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelVersion.getShowName())
                        && StringUtils.isBlank(modelVersion.getDescription());
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelVersion.getShowName())) {
                        predicates.add(cb.like(root.<String>get("showName"), "%" + modelVersion.getShowName() + "%"));
                    }
                    //根据描述查询
                    if (StringUtils.isNotBlank(modelVersion.getDescription())) {
                        predicates.add(cb.like(root.<String>get("description"), "%" + modelVersion.getDescription() + "%"));
                    }
                }
                // 如果提供了 chipType，则对 chipType 进行查询
                if (StringUtils.isNotEmpty(chipType)) {
                    predicates.add(cb.like(chipJoin.get("chipType"), "%" + chipType + "%")); // 动态查询 chipType
                }

                // 添加 userId 与当前请求用户 ID 一致的条件
                predicates.add(cb.equal(root.get("userId"), currentUserId));

                cq.where(predicates.toArray(new javax.persistence.criteria.Predicate[predicates.size()]));
                return cq.getRestriction();
            }
        }, pageable);
    }


    /**
     * 用于动态分页查找公开镜像
     */
    public Page<ModelVersion> dynamicFindModelVersionPagePublic(Pageable pageable, ModelVersion modelVersion, String chipType) {
        return modelVersionRepo.findAll(new Specification<ModelVersion>() {
            @Override
            public javax.persistence.criteria.Predicate toPredicate(Root<ModelVersion> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();

                // Join chip to ensure it's loaded
                Join<ModelVersion, Chip> chipJoin = root.join("chip", JoinType.LEFT); // LEFT JOIN chip

                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelVersion.NOT_DELETE));
                predicates.add(cb.notLike(root.get("name"), "%-copy"));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelVersion.getShowName())
                        && StringUtils.isBlank(modelVersion.getDescription());
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelVersion.getShowName())) {
                        predicates.add(cb.like(root.<String>get("showName"), "%" + modelVersion.getShowName() + "%"));
                    }
                    //根据描述查询
                    if (StringUtils.isNotBlank(modelVersion.getDescription())) {
                        predicates.add(cb.like(root.<String>get("description"), "%" + modelVersion.getDescription() + "%"));
                    }

                }
                // 如果提供了 chipType，则对 chipType 进行查询
                if (StringUtils.isNotEmpty(chipType)) {
                    predicates.add(cb.like(chipJoin.get("chipType"), "%" + chipType + "%")); // 动态查询 chipType
                }

                // 添加 isPublic 为 true 的条件
                predicates.add(cb.isTrue(root.get("isPublic")));

                cq.where(predicates.toArray(new javax.persistence.criteria.Predicate[predicates.size()]));
                return cq.getRestriction();
            }
        }, pageable);
    }

    /**
     * 判断版本是否存在
     */
    public boolean isVersionExist(ModelVersion modelVersion) {
        return modelVersionRepo.findModelVersionByNameAndIsDelete(modelVersion.getName(), ModelVersion.NOT_DELETE) != null || modelVersionRepo.findModelVersionById(modelVersion.getId()) != null;
    }

    /**
     * 保存modelVersion
     *
     * @param modelVersion modelVersion
     */
    public void saveModelVersion(ModelVersion modelVersion) {
        modelVersionRepo.save(modelVersion);
    }

    /**
     * 获取训练情况
     */
    public String countVersionTrainNum(ModelVersion modelVersion) {
        int trainExecuting = modelJobService.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersion.getId(), 1, 2);
        int trainSucceeded = modelJobService.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersion.getId(), 1, 1);
        int trainFailed = modelJobService.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersion.getId(), 1, -1);
        int trainCancelled = modelJobService.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersion.getId(), 1, -2);
        int trainSum = modelJobService.countModelJobsByModelVersionIdAndJobType(modelVersion.getId(), 1);
        return String.valueOf(trainExecuting).concat("-").concat(String.valueOf(trainSucceeded)).concat("-").concat(String.valueOf(trainFailed)).concat("-").concat(String.valueOf(trainCancelled)).concat("-").concat(String.valueOf(trainSum));
    }

    /**
     * 获取质检情况
     */
    public String countVersionInspectNum(ModelVersion modelVersion) {
        int inspectExecuting = modelJobService.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersion.getId(), 2, 2);
        int inspectSucceeded = modelJobService.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersion.getId(), 2, 1);
        int inspectFailed = modelJobService.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersion.getId(), 2, -1);
        int inspectCancelled = modelJobService.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersion.getId(), 2, -2);
        int inspectSum = modelJobService.countModelJobsByModelVersionIdAndJobType(modelVersion.getId(), 2);
        return String.valueOf(inspectExecuting).concat("-").concat(String.valueOf(inspectSucceeded)).concat("-").concat(String.valueOf(inspectFailed)).concat("-").concat(String.valueOf(inspectCancelled)).concat("-").concat(String.valueOf(inspectSum));
    }

    /**
     * 更新模型版本
     */
    public boolean updateModelVersionByModelJob(ModelVersion modelVersion, ModelJob modelJob, int updateType) {
        if (modelVersion == null) {
            return false;
        } else {
            //首先把版本的训练情况和质检情况中的数据差分开
            String train = modelVersion.getTrainNum();
            String inspect = modelVersion.getInspectNum();
            String[] trainNums = train.split("-");
            String[] inspectNums = inspect.split("-");
            int trainExecuting = Integer.parseInt(trainNums[0]);
            int trainSucceeded = Integer.parseInt(trainNums[1]);
            int trainFailed = Integer.parseInt(trainNums[2]);
            int trainCancelled = Integer.parseInt(trainNums[3]);
            int trainSum = Integer.parseInt(trainNums[4]);
            int inspectExecuting = Integer.parseInt(inspectNums[0]);
            int inspectSucceeded = Integer.parseInt(inspectNums[1]);
            int inspectFailed = Integer.parseInt(inspectNums[2]);
            int inspectCancelled = Integer.parseInt(inspectNums[3]);
            int inspectSum = Integer.parseInt(inspectNums[4]);
            //新增modeljob和更新modeljob需要更改的数据不一样
            if (updateType == 1) {
                //新增作业的时候 正在作业+1 作业总数+1
                if (modelJob.getJobType() == 1) {
                    trainExecuting = trainExecuting + 1;
                    trainSum = trainSum + 1;
                }
                if (modelJob.getJobType() == 2) {
                    inspectExecuting = inspectExecuting + 1;
                    inspectSum = inspectSum + 1;
                }
            } else if (updateType == 2) {
                //更新作业，更新作业会涉及到作业的状态变化
                //状态变化几种情况  正在-作业成功  正在-中止作业
                //但是更新不仅仅是状态的改变，还有修改描述？  判断修改状态还是修改描述？？
                if (modelJob.getJobType() == ModelJob.TRAIN) {
                    trainExecuting = trainExecuting - 1;
                    if (modelJob.getStatus().equals(ModelJobStateCodeConstant.TRAIN_SUCCEEDED)) {
                        trainSucceeded = trainSucceeded + 1;
                    } else if (modelJob.getStatus().equals(ModelJobStateCodeConstant.TRAIN_FAILED)) {
                        trainFailed = trainFailed + 1;
                    } else if (modelJob.getStatus().equals(ModelJobStateCodeConstant.CANCELED)) {
                        trainCancelled = trainCancelled + 1;
                    } else {
                        // 默认情况下不执行任何操作
                    }

                }
                if (modelJob.getJobType() == ModelJob.INSPECT) {
                    inspectExecuting = inspectExecuting - 1;
                    if (modelJob.getStatus().equals(ModelJob.INSPECT_SUCCEEDED)) {
                        inspectSucceeded = inspectSucceeded + 1;
                    } else if (modelJob.getStatus().equals(ModelJob.INSPECT_FAILED)) {
                        inspectFailed = inspectFailed + 1;
                    } else if (modelJob.getStatus().equals(ModelJobStateCodeConstant.CANCELED)) {
                        inspectCancelled = inspectCancelled + 1;
                    } else {
                        // 默认情况下不执行任何操作
                    }

                }
            }
            //所有状态都改变  需要把训练情况和质检情况重新拼接
            modelVersion.setTrainNum(String.valueOf(trainExecuting).concat("-").concat(String.valueOf(trainSucceeded)).concat("-").concat(String.valueOf(trainFailed)).concat("-").concat(String.valueOf(trainCancelled)).concat("-").concat(String.valueOf(trainSum)));
            modelVersion.setInspectNum(String.valueOf(inspectExecuting).concat("-").concat(String.valueOf(inspectSucceeded)).concat("-").concat(String.valueOf(inspectFailed)).concat("-").concat(String.valueOf(inspectCancelled)).concat("-").concat(String.valueOf(inspectSum)));
            modelVersionRepo.save(modelVersion);
            return true;
        }
    }

    public boolean updateModelVersion(ModelVersion modelVersion) {
        ModelVersion modelVersionInDatabase = modelVersionRepo.findModelVersionById(modelVersion.getId());
        if (modelVersionInDatabase == null) {
            return false;
        } else {
            modelVersionRepo.save(modelVersionInDatabase);
            return true;
        }

    }

    /**
     * 根据modelVersion的id查询modelVersion
     *
     * @param id
     * @return ModelVersion
     */
    public ModelVersion findModelVersionById(long id) {
        return modelVersionRepo.findModelVersionById(id);
    }


    /**
     * 根据modelVersion查询ModelMineService
     *
     * @param model 算法
     */
    public MineService findModelMineServiceByModel(Model model) {
        return modelMineServiceRepo.findDistinctByModelContainingAndIsDelete(model, MineService.NOT_DELETE);
    }

    /**
     * 根据model删除在服务和算法关联表中相关信息
     *
     * @param model 算法
     */
    public void deleteModelMineServiceByModel(Model model) {
        MineService mineService = modelMineServiceRepo.findDistinctByModelContainingAndIsDelete(model, MineService.NOT_DELETE);
        List<Model> models = mineService.getModel();
        if (models.removeIf(model1 -> model1.getId() == model.getId())) {
            System.out.println("Successfully removed ModelVersion with ID " + model.getId() + " from MineService");
        }
        mineService.setModel(models);
        modelMineServiceRepo.save(mineService);
    }


    /**
     * 查询当前设备绑定的算法模型版本list
     */
//    public List<ModelVersion> findModelVersionsByMonitorId(long monitorId) {
//        return modelVersionRepo.findModelVersionsByMonitorId(monitorId, ModelVersion.NOT_DELETE);
//    }

    /**
     * 查询当前算法模型绑定的监控设备
     */
//    public List<Monitor> findMonitorsBymodelVersionId(long modelVersionId) {
//        return modelVersionRepo.findMonitorsByModelVersionId(modelVersionId, Monitor.NOT_DELETE);
//    }

//    public List<JSONObject> findMonitorsInfoByModelVersionId(long modelVersionId) {
//        List<Monitor> monitors = modelVersionRepo.findMonitorsByModelVersionId(modelVersionId, Monitor.NOT_DELETE);
//        List<JSONObject> monitorList = new ArrayList<>();
//        for (Monitor monitor : monitors) {
//            JSONObject info = new JSONObject();
//            info.put("id", monitor.getId());
//            info.put("monitorName", monitor.getMonitorName());
//            info.put("name", monitor.getName());
//            String status = monitor.getStatusUsing() == 1 ? "运行中" : "暂停中";
//            info.put("status", status);
//            monitorList.add(info);
//        }
//        return monitorList;
//    }

    /**
     * 查询当前算法模型绑定的监控设备
     */
//    public List<ModelVersion> findModelVersionByMineServiceId(long mineServiceId) {
//        return modelVersionRepo.findModelVersionByMineServiceId(mineServiceId, ModelVersion.NOT_DELETE);
//    }


//    public List<JSONObject> findMonitorsInfoByModelVersionShowName(String modelVersionShowName) {
//
//        ModelVersion modelVersion = modelVersionRepo.findModelVersionByShowNameAndIsInferable(modelVersionShowName,true);
//        if (modelVersion != null){
//            System.out.println("modelVersion"+modelVersion.getName());
//            List<Monitor> monitors = modelVersionRepo.findMonitorsByModelVersionId(modelVersion.getId(),Monitor.NOT_DELETE);
//            List<JSONObject> monitorList = new ArrayList<>();
//            for (Monitor monitor : monitors) {
//                JSONObject info = new JSONObject();
//                info.put("id", monitor.getId());
//                info.put("monitorName", monitor.getMonitorName());
//                info.put("name", monitor.getName());
//                String status = monitor.getStatusUsing() == 1 ? "运行中" : "暂停中";
//                info.put("status", status);
//                monitorList.add(info);
//            }
//            return monitorList;
//        }
//        return null;
//    }

//    /**
//     * 判断算法版本是否有作业未完成
//     * return true 表示没有作业未完成，false表示有作业未完成
//     */
//    public Boolean isJobUndone(ModelVersion modelVersion) {
//        List<ModelJob> modelJobs = modelJobService.findModelJobsByModelVersionId(modelVersion.getId());
//        for (ModelJob modelJob : modelJobs) {
//            if (modelJob.getStatus() == 2) {
//                return false;
//            }
//        }
//        return true;
//    }

    /**
     * 判断算法版本是否有部署
     * return true 表示有部署，false表示无部署
     */
    public Boolean isDeploymentExist(ModelVersion modelVersion) {
        List<Deployment> deployments = modelDeploymentRepo.findDeploymentsByModelVersion(modelVersion);
        return !deployments.isEmpty();
    }

    /**
     * 判断算法版本是否可以删除
     */
//    public Boolean canDeleteModelVersion(ModelVersion modelVersion) {
//        Boolean canDelete;
//        canDelete = isBindSth(modelVersion) && isJobUndone(modelVersion) && !isDeploymentExist(modelVersion);
//        return canDelete;
//    }


    /**
     * 新部署镜像替换旧部署镜像的MineService
     */
    public Boolean replaceMineServiceOfDeploymentModelVersion(Model newModelVersion, Model existModelVersion) {
        MineService mineService = modelMineServiceService.findMineServiceByModelVersionId(existModelVersion.getId());
        if (mineService != null) {
            List<Model> modelList = mineService.getModel();
            modelList.removeIf(model -> existModelVersion.getId() == model.getId());
            modelList.add(newModelVersion);
            modelMineServiceService.save(mineService);

        }
        return true;

    }

    /**
     * 部署作业替换流程
     */
    public Boolean replaceModelDeployment(ModelVersion newModelVersion, ModelVersion existModelVersion) {
        List<Deployment> deploymentList = modelDeploymentService.findDeploymentsByModelVersion(existModelVersion);
        if (!deploymentList.isEmpty()) {
            for (Deployment dp : deploymentList) {
                Deployment deployment = new Deployment();
                deployment.setNamespace(dp.getNamespace());
                String deployName = "deploy-" + newModelVersion.getId() + '-' + dp.getController().getId() + '-' + dp.getMonitor().getId();
                deployment.setDeploymentName(deployName);
                deployment.setImage(newModelVersion.getName());
                deployment.setWeightPath(dp.getWeightPath());
                deployment.setWeightRootPath(dp.getWeightRootPath());
                deployment.setLabel(dp.getLabel());
                deployment.setCpuNum(dp.getCpuNum());
                deployment.setMemoryNum(dp.getMemoryNum());
                deployment.setGpuNum(dp.getGpuNum());
                deployment.setModelVersion(newModelVersion);
                deployment.setController(dp.getController());
                deployment.setMonitor(dp.getMonitor());
                Monitor monitor = modelMonitorService.getMonitorById(deployment.getMonitor().getId());
                Date nowDate = new Date();
                String storagePath = "modelAlert/" + "sceneId-" + monitor.getScene().getId() + "/" + "monitorId-" + monitor.getId() + "/" + "modelVersionId-" + newModelVersion.getId() + "/" + "date-" + new SimpleDateFormat("yyyyMMdd").format(nowDate);
                String description = "部署 " + new SimpleDateFormat("yyyyMMdd").format(nowDate) + " 算法ID_" + newModelVersion.getId() + " 摄像头ID_" + deployment.getMonitor().getId() + " 控制器ID_" + deployment.getController().getId();
                deployment.setDescription(description);
                Map<String, String> config = modelDeploymentService.saveConfig(newModelVersion.getId(), deployment.getMonitor());
                config.put("MODE_WORKING_MODE", "3");
                Msg<String> msg = deploymentController.createDeployment(deployment.getDeploymentName(), deployment.getImage(), deployment.getWeightRootPath(), deployment.getWeightPath(), deployment.getGpuNum(), deployment.getCpuNum(), deployment.getMemoryNum(), deployment.getLabel(), storagePath, config);
                if (Objects.equals(msg.getCode(), MsgCode.SUCCEED.getCode())) {
                    deploymentService.deleteDeploy(deploymentService.getApiClient(k8sConfig), dp.getNamespace(), dp.getDeploymentName());
                    modelDeploymentService.deleteDeployment(dp.getId());
                }
            }
        }
        return true;
    }

    /**
     * 获取所有训练功能的镜像
     *
     * @return
     */
    public List<ModelVersion> getTrainModelVersions(Long userId) {
        return modelVersionRepo.findAll(new Specification<ModelVersion>() {
            @Override
            public Predicate toPredicate(Root<ModelVersion> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelVersion.NOT_DELETE));
                //筛选已发布的算法
                predicates.add(cb.equal(root.<Boolean>get("isTrainable"), true));
                String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    Predicate isPublicOrBelongsToUser = cb.or(
                            cb.equal(root.get("isPublic"), true),
                            cb.equal(root.get("userId"), userId)
                    );
                    predicates.add(isPublicOrBelongsToUser);
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        });
    }

    /**
     * 获取所有部署功能的镜像
     *
     * @return
     */
    public List<ModelVersion> getDeployModelVersions(Long userId) {
        return modelVersionRepo.findAll(new Specification<ModelVersion>() {
            @Override
            public Predicate toPredicate(Root<ModelVersion> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelVersion.NOT_DELETE));
                //筛选已发布的算法
                predicates.add(cb.equal(root.<Boolean>get("isInferable"), true));
                Predicate isPublicOrBelongsToUser = cb.or(
                        cb.equal(root.get("isPublic"), true),
                        cb.equal(root.get("userId"), userId)
                );
                predicates.add(isPublicOrBelongsToUser);
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        });
    }

    /**
     * 获取所有部署功能的镜像
     *
     * @return
     */
    public List<ModelVersion> getConversionModelVersions(Long userId) {
        return modelVersionRepo.findAll(new Specification<ModelVersion>() {
            @Override
            public Predicate toPredicate(Root<ModelVersion> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelVersion.NOT_DELETE));
                // isInspectable在本项目是"可转换"
                predicates.add(cb.equal(root.<Boolean>get("isInspectable"), true));
                //筛选已发布的算法
                String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    Predicate isPublicOrBelongsToUser = cb.or(
                            cb.equal(root.get("isPublic"), true),
                            cb.equal(root.get("userId"), userId)
                    );
                    predicates.add(isPublicOrBelongsToUser);
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            }
        });
    }

    /**
     * 进行镜像的复制
     * ->
     */
    public void copyModelVersion(ModelExplore modelExplore) {
        // 复制训练镜像
        ModelVersion trainModelVersion = modelExplore.getTrainModelVersion();
        if (!Objects.isNull(trainModelVersion)) {
            ModelVersion cloneTrainModelVersion = new ModelVersion();
            BeanUtils.copyProperties(trainModelVersion, cloneTrainModelVersion, "id");
            cloneTrainModelVersion.setName(trainModelVersion.getName() + "-copy");
            List<ModelConfig> modelConfigList = trainModelVersion.getModelConfigList();
            ArrayList<ModelConfig> modelConfigs = new ArrayList<>();
            modelConfigList.forEach(modelConfig -> {
                ModelConfig newModelConfig = new ModelConfig();
                BeanUtils.copyProperties(modelConfig, newModelConfig, "id");
                modelConfigService.saveModelConfig(newModelConfig);
                // 复制
                modelConfigs.add(newModelConfig);
            });
            cloneTrainModelVersion.setModelConfigList(modelConfigs);
            this.saveModelVersion(cloneTrainModelVersion);
        }
        // 复制推理镜像
        ModelVersion deployModelVersion = modelExplore.getDeployModelVersion();
        if (!Objects.isNull(deployModelVersion)) {
            ModelVersion cloneDeployModelVersion = new ModelVersion();
            BeanUtils.copyProperties(deployModelVersion, cloneDeployModelVersion, "id");
            cloneDeployModelVersion.setName(deployModelVersion.getName() + "-copy");
            List<ModelConfig> modelConfigList = deployModelVersion.getModelConfigList();
            ArrayList<ModelConfig> modelConfigs = new ArrayList<>();
            modelConfigList.forEach(modelConfig -> {
                ModelConfig newModelConfig = new ModelConfig();
                BeanUtils.copyProperties(modelConfig, newModelConfig, "id");
                modelConfigService.saveModelConfig(newModelConfig);
                // 复制
                modelConfigs.add(newModelConfig);
            });
            cloneDeployModelVersion.setModelConfigList(modelConfigs);
            this.saveModelVersion(cloneDeployModelVersion);
        }
        // 复制转换镜像
        ModelVersion convertModelVersion = modelExplore.getDeployModelVersion();
        if (!Objects.isNull(convertModelVersion)) {
            ModelVersion cloneConvertModelVersion = new ModelVersion();
            BeanUtils.copyProperties(convertModelVersion, cloneConvertModelVersion, "id");
            cloneConvertModelVersion.setName(convertModelVersion.getName() + "-copy");
            List<ModelConfig> modelConfigList = convertModelVersion.getModelConfigList();
            ArrayList<ModelConfig> modelConfigs = new ArrayList<>();
            modelConfigList
                    .forEach(modelConfig -> {
                        ModelConfig newModelConfig = new ModelConfig();
                        BeanUtils.copyProperties(modelConfig, newModelConfig, "id");
                        modelConfigService.saveModelConfig(newModelConfig);
                        // 复制
                        modelConfigs.add(newModelConfig);
                    });
            cloneConvertModelVersion.setModelConfigList(modelConfigs);
            this.saveModelVersion(cloneConvertModelVersion);
        }
    }

    /**
     * 获取所有自动标注功能的镜像
     *
     * @return
     */
    public List<ModelVersion> getAutoLabelModelVersions() {
        return modelVersionRepo.findModelVersionsByAutoLabel(true, ModelVersion.NOT_DELETE);
    }

    /**
     * 更新指定 ModelVersion 的 isPublic 字段
     *
     * @param id       ModelVersion 的 ID
     * @param isPublic 要设置的 isPublic 值
     * @return 更新是否成功
     */
    public boolean updateIsPublic(Long id, boolean isPublic) {
        Optional<ModelVersion> optionalModelVersion = modelVersionRepo.findById(id);
        if (optionalModelVersion.isPresent()) {
            ModelVersion modelVersion = optionalModelVersion.get();
            modelVersion.setPublic(isPublic); // 设置 isPublic 字段
            modelVersionRepo.save(modelVersion); // 保存更新
            return true;
        }
        return false; // 如果找不到该 ID 的 ModelVersion，返回 false
    }

    public List<ModelVersion> findImageSupportAutoLabel() {
        return modelVersionRepo.findImageSupportAutoLabel();
    }

    /**
     * 根据标注类型查询支持自动标注的镜像列表
     *
     * @param annotationType 标注类型（Detection/Segmentation）
     * @return 镜像列表
     */
    public List<ModelVersion> findImageSupportAutoLabel(String annotationType) {
        if (annotationType == null || annotationType.trim().isEmpty()) {
            return modelVersionRepo.findImageSupportAutoLabel();
        }
        return modelVersionRepo.findImageSupportAutoLabelByType(annotationType);
    }

    public List<ModelVersion> findMatchedAutoLabelImages(String applicationName, String deviceFirmware, String annotationType) {
        if (StringUtils.isBlank(applicationName) || StringUtils.isBlank(deviceFirmware)) {
            return Collections.emptyList();
        }

        ModelApplication modelApplication;
        try {
            modelApplication = modelApplicationService.getUniqueModelApplication(applicationName, deviceFirmware);
        } catch (Exception e) {
            return Collections.emptyList();
        }
        if (modelApplication == null || modelApplication.getId() == null) {
            return Collections.emptyList();
        }

        Set<String> applicationLabelNames = modelApplicationService.getBoundLabels(modelApplication.getId())
                .stream()
                .map(label -> Objects.toString(label.get("name"), null))
                .map(this::normalizeLabelName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (applicationLabelNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<ModelVersion> candidates = findImageSupportAutoLabel(annotationType);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> candidateIds = candidates.stream()
                .map(ModelVersion::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (candidateIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Set<String>> imageLabelsByVersionId = modelVersionDefaultLabelsRepo.findRelationsByModelVersionIds(candidateIds)
                .stream()
                .filter(relation -> relation.getModelVersionId() != null && StringUtils.isNotBlank(relation.getLabelName()))
                .collect(Collectors.groupingBy(
                        ModelVersionDefaultLabels::getModelVersionId,
                        Collectors.mapping(relation -> normalizeLabelName(relation.getLabelName()), Collectors.toSet())
                ));

        return candidates.stream()
                .filter(candidate -> hasMatchedLabel(imageLabelsByVersionId.get(candidate.getId()), applicationLabelNames))
                .collect(Collectors.toList());
    }

    private boolean hasMatchedLabel(Set<String> imageLabels, Set<String> applicationLabelNames) {
        if (imageLabels == null || imageLabels.isEmpty()) {
            return false;
        }
        return imageLabels.stream().anyMatch(applicationLabelNames::contains);
    }

    private String normalizeLabelName(String labelName) {
        if (StringUtils.isBlank(labelName)) {
            return null;
        }
        return labelName.trim().toLowerCase(Locale.ROOT);
    }

    public ModelVersion findModelVersionByUrl(String imageUrl) {
        return modelVersionRepo.findModelVersionByUrl(imageUrl);
    }
}
