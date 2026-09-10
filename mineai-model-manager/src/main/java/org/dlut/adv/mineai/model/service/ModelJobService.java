package org.dlut.adv.mineai.model.service;

import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.constant.RedisConstants;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.utils.RedisUtil;
import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.client.DubheUserFeign;
import org.dlut.adv.mineai.model.domain.dto.LabelDTO;
import org.dlut.adv.mineai.model.domain.dto.ModelConvertDTO;
import org.dlut.adv.mineai.model.domain.dto.ModelJobInfoCardDTO;
import org.dlut.adv.mineai.model.domain.vo.ModelValidationVO;
import org.dlut.adv.mineai.model.dto.DatasetLabelInfoDTO;
import org.dlut.adv.mineai.model.dto.JobPrecisionInfoDTO;
import org.dlut.adv.mineai.model.dto.ModelJobDTO;
import org.dlut.adv.mineai.model.dto.TrainingTaskOverviewDTO;
import org.dlut.adv.mineai.model.dto.UserResourceDTO;
import org.dlut.adv.mineai.model.kubernetes.controller.JobController;
import org.dlut.adv.mineai.model.kubernetes.service.JobService;
import org.dlut.adv.mineai.model.kubernetes.service.LogService;
import org.dlut.adv.mineai.model.repository.ModelGenerationRepo;
import org.dlut.adv.mineai.model.repository.ModelJobLogDataRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.repository.ModelRepo;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.statusMachine.statemachine.StateMachineFactory;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.MinioUtils;
import org.dlut.adv.mineai.model.utils.ModelConversionQueueManager;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.dlut.adv.mineai.model.vo.DatasetVersionDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ModelJobService {

    @Autowired
    private StateMachineFactory stateMachineFactory;

    @Value("${kubernetes.nameSuffix}")
    private String nameSuffix;

    @Value("${kubernetes.namespace}")
    private String namespace;

    @Resource
    ModelJobRepo modelJobRepo;

    @Resource
    ModelRepo modelRepo;

    @Resource
    RedisUtil redisUtil;

    @Resource
    private DubheUtils dubheUtils;

    @Resource
    private ModelGenerationRepo modelGenerationRepo;

    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private DubheUserFeign dubheUserFeign;

    @Resource
    @Lazy
    private ModelGenerationService modelGenerationService;

    @Resource
    private ModelExploreService modelExploreService;

    @Resource
    private ModelDatasetService modelDatasetService;

    @Resource
    private JobController jobController;

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private JobService jobService;
    @Lazy
    @Resource
    private ModelVersionService modelVersionService;

    @Resource
    private ExternalModelConverterService externalModelConverterService;
    @Resource
    AsyncTaskService asyncTaskService;

    private ModelConversionQueueManager modelConversionQueueManager = null;
    @Autowired
    private LogService logService;

    @Resource
    ModelJobLogDataRepo modelJobLogDataRepo;

    /**
     * 获取所有作业
     */
    public List<ModelJob> getModelJob() {
        return modelJobRepo.findAll();
    }

    public List<ModelJob> getModelJobWithNoDeleted() {
        return modelJobRepo.findAllJobWithNoDeleted();
    }

    public long getModelJobCountWithNoDeleted() {
        return modelJobRepo.countJobWithNoDeleted();
    }

    /**
     * 获取所有作业
     */
    public List<ModelJobDTO> getModelJobWithUser() {
        List<Object[]> results = modelJobRepo.findModelJobsWithUsersRaw();
        List<ModelJobDTO> modelJobDTOs = new ArrayList<>();

        for (Object[] result : results) {
            // 创建 ModelJob 实体
            ModelJob job = new ModelJob();
            job.setName((String) result[10]);
            job.setStatus((int) result[16]);
            job.setCreateTime((Timestamp) result[2]);
            String userName = (String) result[result.length - 1]; // 用户名
            Boolean idDelete = (Boolean) result[result.length - 2];
            ModelJobDTO dto = new ModelJobDTO(job, userName,idDelete);
            modelJobDTOs.add(dto);
        }

        return modelJobDTOs; // 返回 DTO 列表
    }


    /**
     * 获取所有作业（不稳定状态）
     */
    public List<ModelJob> getModelJobOnChange() {
        List<ModelJob> modelJobsT = modelJobRepo.findModelJobsByStatus(ModelJobStateCodeConstant.TRAINING);
        List<ModelJob> modelJobsC = modelJobRepo.findModelJobsByStatus(ModelJobStateCodeConstant.CONVERTING);
        List<ModelJob> modelJobsI = modelJobRepo.findModelJobsByStatus(ModelJob.INSPECTING);
        List<ModelJob> modelJobsS = modelJobRepo.findModelJobsByStatus(ModelJobStateCodeConstant.SPLITTING);
        List<ModelJob> modelJobsQ = modelJobRepo.findModelJobsByStatus(ModelJobStateCodeConstant.QUEUING);
        List<ModelJob> modelJobs = new ArrayList<>();

        modelJobs.addAll(modelJobsT);
        modelJobs.addAll(modelJobsC);
        modelJobs.addAll(modelJobsI);
        modelJobs.addAll(modelJobsS);
        modelJobs.addAll(modelJobsQ);

        return modelJobs;
    }

    public Map<String, Object> getOverviewTrainingStats() {
        // 使用JOIN查询一次性获取训练中任务及其userId
        List<Object[]> trainingJobsWithUserId = modelJobRepo.findTrainingJobsWithUserIdByStatus(
                ModelJobStateCodeConstant.TRAINING, ModelJob.TRAIN);

        // 使用JOIN查询一次性获取排队中任务及其userId
        List<Object[]> queuingJobsWithUserId = modelJobRepo.findTrainingJobsWithUserIdByStatus(
                ModelJobStateCodeConstant.QUEUING, ModelJob.TRAIN);

        // 提取所有任务和userId
        List<ModelJob> trainingJobs = trainingJobsWithUserId.stream()
                .map(result -> (ModelJob) result[0])
                .collect(Collectors.toList());

        List<ModelJob> queuingJobs = queuingJobsWithUserId.stream()
                .map(result -> (ModelJob) result[0])
                .collect(Collectors.toList());

        // 收集所有唯一的userId（去重，因为一个generation可能有多个job）
        Set<Long> userIds = new HashSet<>();
        trainingJobsWithUserId.forEach(result -> {
            Long userId = (Long) result[1];
            if (userId != null) {
                userIds.add(userId);
            }
        });
        queuingJobsWithUserId.forEach(result -> {
            Long userId = (Long) result[1];
            if (userId != null) {
                userIds.add(userId);
            }
        });

        // 批量查询用户信息（一次Feign调用）
        Map<Long, String> userIdToCreatorMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                List<Long> userIdList = new ArrayList<>(userIds);
                List<org.dlut.adv.mineai.model.dto.UserDTO> users = dubheUserFeign
                        .findUsersByIds(dubheUtils.getAuthorization(), userIdList)
                        .getData();
                if (users != null) {
                    users.forEach(user -> {
                        // 优先使用userName，如果没有则使用nickname
                        String creatorName = (user.getUsername() != null && !user.getUsername().isEmpty())
                                ? user.getUsername() : user.getNickName();
                        userIdToCreatorMap.put(user.getId(), creatorName);
                    });
                }
            } catch (Exception e) {
                log.warn("批量查询用户信息失败: {}", e.getMessage());
            }
        }

        // 创建jobId到userId的映射（用于后续查找创建者）
        Map<Long, Long> jobIdToUserIdMap = new HashMap<>();
        trainingJobsWithUserId.forEach(result -> {
            ModelJob job = (ModelJob) result[0];
            Long userId = (Long) result[1];
            if (userId != null) {
                jobIdToUserIdMap.put(job.getId(), userId);
            }
        });
        queuingJobsWithUserId.forEach(result -> {
            ModelJob job = (ModelJob) result[0];
            Long userId = (Long) result[1];
            if (userId != null) {
                jobIdToUserIdMap.put(job.getId(), userId);
            }
        });

        // 转换为增强的DTO列表
        List<TrainingTaskOverviewDTO> trainingJobDTOs = trainingJobs.stream()
                .map(job -> {
                    // 获取训练进度
                    Double progress = 0.0;
                    try {
                        ModelJobLogData logData = modelJobLogDataRepo.findByModelJob(job);
                        if (logData != null && logData.getProgress() != null) {
                            progress = logData.getProgress().doubleValue();
                        }
                    } catch (Exception e) {
                        log.warn("获取任务 {} 的进度失败: {}", job.getId(), e.getMessage());
                    }
                    // 获取创建者
                    Long userId = jobIdToUserIdMap.get(job.getId());
                    String creator = userId != null ? userIdToCreatorMap.getOrDefault(userId, "") : "";
                    return TrainingTaskOverviewDTO.fromModelJob(job, progress, creator);
                })
                .collect(Collectors.toList());

        List<TrainingTaskOverviewDTO> queuingJobDTOs = queuingJobs.stream()
                .map(job -> {
                    // 排队中的任务进度为0
                    // 获取创建者
                    Long userId = jobIdToUserIdMap.get(job.getId());
                    String creator = userId != null ? userIdToCreatorMap.getOrDefault(userId, "") : "";
                    return TrainingTaskOverviewDTO.fromModelJob(job, 0.0, creator);
                })
                .collect(Collectors.toList());

        long trainSucceededCount = modelJobRepo.countModelJobsByStatusAndJobType(
                ModelJobStateCodeConstant.TRAIN_SUCCEEDED, ModelJob.TRAIN);
        long trainFailedCount = modelJobRepo.countModelJobsByStatusAndJobType(
                ModelJobStateCodeConstant.TRAIN_FAILED, ModelJob.TRAIN);

        Map<String, Object> result = new HashMap<>(6);
        result.put("trainingCount", trainingJobs.size());
        result.put("queuingCount", queuingJobs.size());
        result.put("trainSucceededCount", trainSucceededCount);
        result.put("trainFailedCount", trainFailedCount);
        result.put("trainingJobs", trainingJobDTOs);
        result.put("queuingJobs", queuingJobDTOs);
        return result;
    }

//    public List<ModelJob> getModelJobByStatusSUCCEEDED() {
//        return modelJobRepo.findModelJobsByStatus(ModelJob.SUCCEEDED);
//    }

    /**
     * 判断作业是否存在
     */
    public boolean isModelJobExist(ModelJob modelJob) {
        return modelJobRepo.findModelJobById(modelJob.getId()) != null;
    }

    /**
     * 根据id查找作业
     *
     * @param id
     * @return
     */
    public ModelJob getModelJobById(Long id) {
        return modelJobRepo.findModelJobById(id);
    }

//    public List<ModelJob> getModelJobByModelVersionId(Long id) {
//        List<ModelJob> modelJobs = modelJobRepo.findModelJobsByModelVersionIdAndAndStatus(id, ModelJob.SUCCEEDED);
//        return modelJobs;
//    }

    /**
     * 中止作业
     */
    public boolean deleteJob(ModelJob modelJob) {
        if (isModelJobExist(modelJob)) {
            //TODO
            modelJobRepo.delete(modelJob);
            return true;
        }
        return false;
    }

    /**
     * 更新作业描述
     */
    public boolean updateJobDesc(ModelJob modelJob) {
        ModelJob modelJobInDatabase = modelJobRepo.findModelJobById(modelJob.getId());
        if (modelJobInDatabase == null) {
            return false;
        } else {
            //遍历modelJob中的属性，同时与modelJobInDatabase对应属性进行对比，如果有不同，则使用modelJobInDatabase.setXXX(modelJob.XXX)进行修改
            if (!Objects.equals(modelJob.getDescription(), modelJobInDatabase.getDescription())) {
                modelJobInDatabase.setDescription(modelJob.getDescription());
            }
            modelJobRepo.save(modelJobInDatabase);
            return true;
        }
    }

    /**
     * 更新作业状态
     */
    public boolean updateJobStatus(ModelJob modelJob) {
        ModelJob modelJobInDatabase = modelJobRepo.findModelJobById(modelJob.getId());
        if (modelJobInDatabase == null) {
            return false;
        } else {
            //遍历modelJob中的属性，同时与modelJobInDatabase对应属性进行对比，如果有不同，则使用modelJobInDatabase.setXXX(modelJob.XXX)进行修改
            if (modelJob.getStatus() != modelJobInDatabase.getStatus()) {
                modelJobInDatabase.setStatus(modelJob.getStatus());
            }
            modelJobRepo.save(modelJobInDatabase);
            return true;
        }
    }

    /**
     * 删除模型版本后，更新作业的对应模型版本的名字
     */
    public boolean updateJobModelVersionName(ModelVersion modelVersion, ModelJob modelJob) {
        if (modelJob == null) {
            return false;
        } else {
            ModelVersion OldModelVersion = modelJob.getModelVersion();
            if (modelVersion.getName() != OldModelVersion.getName()) {
                OldModelVersion.setName(modelVersion.getName());
            }
            modelJobRepo.save(modelJob);
            return true;
        }
    }


    /**
     * 作业分页查询
     *
     * @param pageable
     * @param modelJob
     * @return
     */
    public Page<ModelJob> getJobPage(Pageable pageable, ModelJob modelJob) {
        return modelJobRepo.findAll(new Specification<ModelJob>() {
            @Override
            public javax.persistence.criteria.Predicate toPredicate(Root<ModelJob> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
                if (modelJob.getModelVersion().getId() != 0) {
                    predicates.add(cb.equal(root.get("modelVersion").get("id"), modelJob.getModelVersion().getId()));
                }
                if (modelJob.getJobType() != 0) {
                    predicates.add(cb.equal(root.get("jobType"), modelJob.getJobType()));
                }
                if (modelJob.getStatus() != 0) {
                    predicates.add(cb.equal(root.get("status"), modelJob.getStatus()));
                }
                cq.where(predicates.toArray(new javax.persistence.criteria.Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }


    /**
     * 查询作业根据作业类型和状态
     */
    public int countModelJobsByModelVersionIdAndJobTypeAndStatus(long modelVersionId, int jobType, int status) {
        return modelJobRepo.countModelJobsByModelVersionIdAndJobTypeAndStatus(modelVersionId, jobType, status);
    }

    /**
     * 查询作业根据作业类型
     */
    public int countModelJobsByModelVersionIdAndJobType(long modelVersionId, int jobType) {
        return modelJobRepo.countModelJobsByModelVersionIdAndJobType(modelVersionId, jobType);
    }

    public List<ModelJob> findModelJobsByModelVersionIdAndAndStatus(long modelVersionId, int jobType) {
        return modelJobRepo.findModelJobsByModelVersionIdAndAndStatus(modelVersionId, jobType);
    }

    /**
     * 查询作业根据版本
     */
    public List<ModelJob> findModelJobsByModelVersionName(String modelVersionName) {
        return modelJobRepo.findModelJobsByModelVersionName(modelVersionName);
    }

//    public List<ModelJob> findModelJobsByModelVersionId(long modelVersionId) {
//        return modelJobRepo.findModelJobsByModelVersionIdAndAndStatus(modelVersionId, ModelJob.SUCCEEDED);
//    }

    /**
     * 新增modeljob
     */
    public ModelJob saveModelJob(ModelJob modelJob) {
        return modelJobRepo.save(modelJob);
    }

    /**
     * 根据作业查找到版本
     */
    public ModelVersion findModelVersionByModelJob(ModelJob modelJob) {
        ModelVersion modelVersion = modelJobRepo.findModelJobById(modelJob.getId()).getModelVersion();
        return modelVersion;
    }


    public List<ModelVersion> existsByModelVersionStatusAndJobStatus() {
        List<ModelVersion> modelVersionsT = modelJobRepo.existsByModelVersionStatusAndJobStatus(ModelVersion.NOT_DELETE, ModelJobStateCodeConstant.TRAINING);
        List<ModelVersion> modelVersionsC = modelJobRepo.existsByModelVersionStatusAndJobStatus(ModelVersion.NOT_DELETE, ModelJobStateCodeConstant.CONVERTING);
        List<ModelVersion> modelVersionsI = modelJobRepo.existsByModelVersionStatusAndJobStatus(ModelVersion.NOT_DELETE, ModelJob.INSPECTING);
        List<ModelVersion> modelVersions = new ArrayList<>();

        // 添加三个列表的元素到新的列表中
        modelVersions.addAll(modelVersionsT);
        modelVersions.addAll(modelVersionsC);
        modelVersions.addAll(modelVersionsI);
        return modelVersions;
    }

    public List<ModelValidationVO> queryModelValidationCache(Long jobId) {
        // 如果不为空，那么更新过期时间
        Set<Object> objects = redisUtil.zGet(RedisConstants.MODEL_VALIDATE_PREFIX + jobId.toString());
        if (objects != null && !objects.isEmpty()) {
            redisUtil.expire(jobId.toString(), RedisConstants.CACHE_TTL, TimeUnit.MINUTES);
            return objects.stream().map((o -> (ModelValidationVO) o)).collect(Collectors.toList());
        }
        return null;
    }

    public List<ModelConfig> getConvertModelConfigsByJobId(long jobId) {
        ModelJob modelJob = modelJobRepo.findModelJobById(jobId);
        if (modelJob == null) {
            throw new RuntimeException("未找到对应的作业，作业ID：" + jobId);
        }

        ModelGeneration modelGeneration = modelGenerationRepo.findById(modelJob.getModelGenerationId()).orElse(null);
        if (modelGeneration == null) {
            throw new RuntimeException("未找到对应的生产任务，作业ID：" + jobId);
        }

        ModelVersion convertModelVersion = getConvertModelVersion(modelGeneration);
        List<ModelConfig> resolvedConfigs = convertModelVersion == null ? null : convertModelVersion.getModelConfigList();
        if (resolvedConfigs == null || resolvedConfigs.isEmpty()) {
            throw new RuntimeException("未找到对应的模型配置，作业ID：" + jobId);
        }
        return resolvedConfigs; /*
        // 使用仓库中的方法获取 ModelConfig 列表
        List<ModelConfig> modelConfigs = modelJobRepo.findConvertModelConfigListByJobId(jobId);

        if (modelConfigs == null || modelConfigs.isEmpty()) {
            // 如果没有找到配置项，抛出异常或进行其他处理
            throw new RuntimeException("未找到对应的模型配置，作业ID：" + jobId);
        }

        return modelConfigs;
    */ }

    public Map<String, List<PublishedHyperParams>> getHyperParams(Long modelId) {
        Model model = modelRepo.findModelById(modelId);

        List<PublishedHyperParams> publishedHyperParamsList = model.getPublishedHyperParamsList();

        List<PublishedHyperParams> trainHyperParamsWithValue = new ArrayList<>();
        List<PublishedHyperParams> otherParamsWithValue = new ArrayList<>();
        List<PublishedHyperParams> convertHyperParamsWithValue = new ArrayList<>();
        for (PublishedHyperParams param : publishedHyperParamsList) {
            String paramName = param.getParamName();
            Integer paramUsage = param.getParamUsage();
            if ((paramName.startsWith("HP_") || paramName.startsWith("TRAIN_")) && Objects.equals(paramUsage, PublishedHyperParams.PARAM_TYPE_TRAIN)) {
                trainHyperParamsWithValue.add(param);
            } else if (paramName.startsWith("CONVERT_") && Objects.equals(paramUsage, PublishedHyperParams.PARAM_TYPE_CONVERT)) {
                convertHyperParamsWithValue.add(param);
            } else {
                if ("MODEL_WORKING_MODE".equals(paramName)) {
                    trainHyperParamsWithValue.add(param);
                }
                otherParamsWithValue.add(param);
            }
        }
        Map<String, List<PublishedHyperParams>> resultMap = new HashMap<>();
        resultMap.put("trainHyperParamsWithValue", trainHyperParamsWithValue);
        resultMap.put("convertHyperParamsWithValue", convertHyperParamsWithValue);
        resultMap.put("otherParamsWithValue", otherParamsWithValue);
        return resultMap;
    }


    public List<ModelJob> findModelJobsByModelGenerationId(Long modelGenerationId) {
        return modelJobRepo.findByModelGenerationId(modelGenerationId);
    }

    //    public String getModelExploreDescriptionByGenerationId(Long modelGenerationId) {
//        return modelJobRepo.findModelExploreDescriptionByGenerationId(modelGenerationId);
//    }
    public String getModelExploreDescriptionByGenerationId(Long modelGenerationId) {
        List<String> descriptions = modelJobRepo.findModelExploreDescriptionByGenerationId(modelGenerationId);
        // 如果存在多条记录，可以根据需求处理，例如取第一个
        return descriptions.isEmpty() ? null : descriptions.get(0);
    }

    @Transactional
    public Msg<String> createJob(Long modelGenerationId, Long modelJobId, Integer modelWorkingMode, String modelType) {

        ModelGeneration modelGenerationById = modelGenerationService.findModelGenerationById(modelGenerationId);
        if (modelGenerationById == null) {
            throw new RuntimeException("未找到对应的modelGeneration");
        }
        HardwareParams hardwareParams = modelGenerationById.getHardwareParams();
        // 用户的memory和cpu用量增加
        // 这里的判断为了兼容老的测试用例，顺便加强'鲁棒性'~
        if (hardwareParams != null) {
            log.info("由于开启训练任务分配cpu");
            String result = dubheUserFeign.addUserResource(
                    dubheUtils.getAuthorization(),
                    new UserResourceDTO(
                            modelGenerationById.getUserId(),
                            Long.parseLong(hardwareParams.getCpus()),
                            Long.parseLong(hardwareParams.getMemory().split("G")[0]),
                            Long.parseLong(hardwareParams.getGpuMemory().split("G")[0]),
                            Long.parseLong(hardwareParams.getVGpuCores())
                    )
            );
            if (Objects.equals(result, UserResourceDTO.CPU_USED_OVER_LIMIT)) {
                return new Msg<>(MsgCode.USER_CPU_USED_OVER_LIMIT);
            }
            if (Objects.equals(result, UserResourceDTO.MEMORY_USED_OVER_LIMIT)) {
                return new Msg<>(MsgCode.USER_MEMORY_USED_OVER_LIMIT);
            }
            if (Objects.equals(result, UserResourceDTO.GPU_MEMORY_USED_OVER_LIMIT)) {
                return new Msg<>(MsgCode.USER_GPU_USED_OVER_LIMIT);
            }
            if (Objects.equals(result, UserResourceDTO.VGPU_CORES_USED_OVER_LIMIT)) {
                return new Msg<>(MsgCode.USER_VGPU_CORES_USED_OVER_LIMIT);
            }
            if (Objects.equals(result, UserResourceDTO.DISTRIBUTED_FAILED_UNKNOWN_REASON)) {
                return new Msg<>(MsgCode.DISTRIBUTED_FAILED_UNKNOWN_REASON);
            }
        }

        ModelJob modelJob = new ModelJob();
        modelJob.setJobType(modelWorkingMode);
        modelJob.setGpus(hardwareParams == null ? null : hardwareParams.getGpus());
        modelJob.setCpus(hardwareParams == null ? null : hardwareParams.getCpus());
        modelJob.setMemory(hardwareParams == null ? null : hardwareParams.getMemory());
        modelJob.setGpuMemory(hardwareParams == null ? null : hardwareParams.getGpuMemory());
        Map<String, String> generationParams = new HashMap<>(modelGenerationById.getHyperParams());
        modelJob.setParams(generationParams);
        modelJob.setJobType(ModelJob.TRAIN);
        modelJob.setStatus(ModelJobStateCodeConstant.TRAINING);
        ModelJob modelJobSaved = saveModelJob(modelJob);
        modelJobSaved.setName("job-" + nameSuffix + '-' + modelJobSaved.getId());
        modelJobSaved.setWeightPath(modelJob.getName());
        modelJobSaved.setModelGenerationId(modelGenerationId);

        if (modelGenerationById.getTrainDatasetVersions() != null) {
            List<Long> versionsCopy = new ArrayList<>(modelGenerationById.getTrainDatasetVersions());
            modelJobSaved.setTrainDatasetVersions(versionsCopy);
        } else {
            modelJobSaved.setTrainDatasetVersions(new ArrayList<>());
        }
        // 创建集合副本，避免共享引用
        if (modelGenerationById.getSelectedLabels() != null) {
            Map<Integer, String> labelsCopy = new HashMap<>(modelGenerationById.getSelectedLabels());
            modelJobSaved.setSelectedLabels(labelsCopy);
        } else {
            modelJobSaved.setSelectedLabels(new HashMap<>());
        }
        modelJobSaved.setGpuMode(modelGenerationById.getGpuMode());
        modelJobSaved.setGpuCount(modelGenerationById.getGpuCount());

        saveModelJob(modelJobSaved);
        ModelVersion trainModelVersion = getTrainModelVersion(modelGenerationById);
        String url = trainModelVersion == null ? null : trainModelVersion.getUrl();
        if (url == null || url.trim().isEmpty()) {
            throw new RuntimeException("当前算法未配置可用训练镜像(trainModelVersion.url)");
        }
        String preWeightPath = null;
        if (modelJobId != null) {
            preWeightPath = getModelJobById(modelJobId).getWeightPath();
        }
        List<ModelJob> modelJobList = modelGenerationById.getModelJobList();
        if (modelJobList == null) {
            ArrayList<ModelJob> modelJobs = new ArrayList<>();
            modelJobs.add(modelJobSaved);
            modelGenerationById.setModelJobList(modelJobs);
        } else {
            modelJobList.add(modelJobSaved);
            modelGenerationById.setModelJobList(modelJobList);
        }
        long modelJobSavedId = modelJobSaved.getId();
        modelGenerationById.setLatestJobId(modelJobSavedId);
        ModelGeneration savedModelGeneration = modelGenerationRepo.save(modelGenerationById);
        ModelJob modelJobSaved2 = getModelJobById(modelJobSaved.getId());

        log.info("createJob: generationId={}, modelJobId={}, splitSize={}, trainImageUrl={}",
                modelGenerationId, modelJobSavedId, savedModelGeneration.getSplitSize(), url);

        if (savedModelGeneration.getSplitSize() != null && !savedModelGeneration.getSplitSize().isEmpty()) {
            log.info("createJob: splitSize={}, entering dataset-cut path", savedModelGeneration.getSplitSize());
            // 需要切分数据集：提交异步任务
            // 状态转换
            ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
            modelJobStateMachine.modelJobSplitEvent(Integer.parseInt(String.valueOf(modelJobSaved.getId())));

            String taskId = asyncTaskService.submitDatasetCutAndCreateJob(
                    namespace,
                    modelJobSaved2,
                    savedModelGeneration,
                    url,
                    hardwareParams,
                    preWeightPath,
                    modelType
            );
            logService.persistLogsFromLokiAsync("job-" + nameSuffix + '-' + modelJobSaved.getId());
            // 返回任务 ID，表示任务已提交
            return new Msg<>(MsgCode.SUCCEED, "数据集切分任务已提交，任务ID：" + taskId);
        }
//        else {
//            // 不需要切分数据集，直接创建任务
//            jobController.createTrainJob(
//                    namespace,
//                    modelJobSaved.getName(),
//                    modelJobSaved.getWeightPath(),
//                    url,
//                    trainDataset.getVersionUrl(),
//                    modelDatasetService.selectDatasetVersionById(modelGenerationById.getTestDataset()).getVersionUrl(),
//                    modelDatasetService.selectDatasetVersionById(modelGenerationById.getValDataset()).getVersionUrl(),
//                    "YOLO".equals(trainDataset.getFormat()) ? (trainDataset.getVersionUrl() + "/data.yaml") : null,
//                    hardwareParams == null ? null : hardwareParams.getGpus(),
//                    hardwareParams == null ? null : hardwareParams.getCpus(),
//                    hardwareParams == null ? null : hardwareParams.getMemory(),
//                    hardwareParams == null ? null : hardwareParams.getGpuMemory(),
//                    hardwareParams == null ? null : hardwareParams.getVGpuCores(),
//                    modelGenerationById.getHyperParams(),
//                    preWeightPath,
//                    modelType
//            );
//
//            // 返回成功消息
//            return new Msg<>(MsgCode.SUCCEED, "任务创建成功");
//        }
        logService.persistLogsFromLokiAsync("job-" + nameSuffix + '-' + modelJobSaved.getId());
        return new Msg<>(MsgCode.SUCCEED, "任务创建成功");
    }

    @Transactional
    public Msg<String> createConvertJob(Long modelGenerationId, ModelConvertDTO modelConvertDTO, Map<String, String> quantizationParams) {
        // 保存模型转换 modelJob
        ModelGeneration modelGenerationById = modelGenerationService.findModelGenerationById(modelGenerationId);

        if (modelGenerationById == null) {
            throw new RuntimeException("未找到对应的modelGeneration");
        }
        HardwareParams hardwareParams = modelGenerationById.getHardwareParams();
        // 用户的memory和cpu用量增加
        // 这里的判断为了兼容老的测试用例，顺便加强'鲁棒性'~
        if (hardwareParams != null) {
            log.info("由于开启转换任务分配cpu");
            String result = dubheUserFeign.addUserResource(
                    dubheUtils.getAuthorization(),
                    new UserResourceDTO(
                            modelGenerationById.getUserId(),
                            Long.parseLong(hardwareParams.getCpus()),
                            Long.parseLong(hardwareParams.getMemory().split("G")[0]),
                            Long.parseLong(hardwareParams.getGpuMemory().split("G")[0]),
                            Long.parseLong(hardwareParams.getVGpuCores())
                    )
            );
            if (Objects.equals(result, UserResourceDTO.CPU_USED_OVER_LIMIT)) {
                return new Msg<>(MsgCode.USER_CPU_USED_OVER_LIMIT);
            }
            if (Objects.equals(result, UserResourceDTO.MEMORY_USED_OVER_LIMIT)) {
                return new Msg<>(MsgCode.USER_MEMORY_USED_OVER_LIMIT);
            }
            if (Objects.equals(result, UserResourceDTO.GPU_MEMORY_USED_OVER_LIMIT)) {
                return new Msg<>(MsgCode.USER_GPU_USED_OVER_LIMIT);
            }
            if (Objects.equals(result, UserResourceDTO.VGPU_CORES_USED_OVER_LIMIT)) {
                return new Msg<>(MsgCode.USER_VGPU_CORES_USED_OVER_LIMIT);
            }
            if (Objects.equals(result, UserResourceDTO.DISTRIBUTED_FAILED_UNKNOWN_REASON)) {
                return new Msg<>(MsgCode.DISTRIBUTED_FAILED_UNKNOWN_REASON);
            }

        }

        ModelJob modelJob = new ModelJob();
        modelJob.setJobType(ModelJob.CONVERT);
        modelJob.setGpus(hardwareParams == null ? null : hardwareParams.getGpus());
        modelJob.setCpus(hardwareParams == null ? null : hardwareParams.getCpus());
        modelJob.setMemory(hardwareParams == null ? null : hardwareParams.getMemory());
        modelJob.setGpuMemory(hardwareParams == null ? null : hardwareParams.getGpuMemory());
        Map<String, String> generationParams = new HashMap<>(modelGenerationById.getHyperParams());
        modelJob.setParams(generationParams);
//        modelJob.setStatus(ModelJob.TRAINING);
        modelJob.setName(modelConvertDTO.getJobName());
        modelJob.setWeightPath(modelJob.getName());
        modelJob.setModelGenerationId(modelGenerationId);

        Map<String, String> convertDTOParams = modelConvertDTO.getParams();
        Map<String, String> params = new HashMap<>(convertDTOParams);
        //Map<String, String> params = modelConvertDTO.getParams();

        // 通过镜像获取到chipType
        ModelGeneration modelGeneration = modelGenerationService.getModelGenerationById(modelGenerationId);
        ModelVersion convertModelVersion = getConvertModelVersion(modelGeneration);
        if (convertModelVersion == null || convertModelVersion.getChip() == null) {
            throw new RuntimeException("未找到可用的转换镜像配置");
        }

        Chip convertImageChip = convertModelVersion.getChip();

        //外部转换相关逻辑，检查转换镜像的算例芯片字段，是否已经有对应的外部转换设备，如果有就加上外部转换的固定参数
        String convertImageChipType = convertImageChip.getChipType();

//        // 查找所有的 ExternalModelConverter
//        List<ExternalModelConverter> allDevices = externalModelConverterService.findAllDevices();
//
//        // 创建一个列表来存储支持该 chipType 的 converter
//        List<ExternalModelConverter> matchingConverters = new ArrayList<>();
//
//        // 遍历每个 ExternalModelConverter
//        for (ExternalModelConverter device : allDevices) {
//            List<Chip> chips = device.getChips();
//
//            // 遍历每个 Chip，检查其类型
//            for (Chip chip : chips) {
//                if (chip.getChipType().equals(convertImageChipType)) {
//                    matchingConverters.add(device);
//                    break; // 找到匹配的 chipType 后跳出循环
//                }
//            }
//        }
//
//        // 检查是否找到任何匹配的 converter
//        if (!matchingConverters.isEmpty()) {
//            // 创建一个 Random 对象
//            Random random = new Random();
//
//            // 随机选择一个匹配的 converter
//            int randomIndex = random.nextInt(matchingConverters.size());
//            ExternalModelConverter selectedConverter = matchingConverters.get(randomIndex);
//
//            params.put("CONVERT_SERVER_IP", selectedConverter.getIp());
//            params.put("CONVERT_SERVER_PORT", String.valueOf(selectedConverter.getPort()));
//        } else {
//            System.out.println("没有找到支持该 chipType 的转换器。进行非集群外转换");
//        }
//
        // 使用 ModelConversionQueueManager 查找并选择适用的负载最小的转换器
        boolean isOutsied = false;
        if (modelConversionQueueManager == null) {
            modelConversionQueueManager = new ModelConversionQueueManager(modelJobRepo, jobController, externalModelConverterService);
        }
        Optional<ExternalModelConverter> selectedConverterOpt = modelConversionQueueManager.findAndSelectConverter(convertImageChipType);
        if (selectedConverterOpt.isPresent()) {
            ExternalModelConverter selectedConverter = selectedConverterOpt.get();
            params.put("CONVERT_SERVER_IP", selectedConverter.getIp());
            params.put("CONVERT_SERVER_PORT", String.valueOf(selectedConverter.getPort()));
            isOutsied = true;
        } else {
            isOutsied = false;
            System.out.println("没有找到支持该 chipType 的转换器。进行非集群外转换");
        }

        // 设置目标平台
        params.put("CONVERT_TARGET_PLATFORM", convertImageChipType);

        // 根据 quantizationParams 的状态设置量化参数
        if (quantizationParams == null || quantizationParams.isEmpty()) {
            // 标准化训练走这里
            String isQuantized = params.get("isQuantized");
            params.put("MODE_QUANTIZATION_ON", isQuantized);

            // 只有当 isQuantized 不为 false 时才添加 MODE_QUANTIZATION_IMAGE_NUM
            if ("true".equals(isQuantized)) {
                params.put("MODE_QUANTIZATION_IMAGE_NUM", params.get("count"));
            }

            // 移除不再需要的参数
            params.remove("isQuantized");
            params.remove("count");
            params.remove("maxCount");
        } else {
            // 从 quantizationParams 中获取量化参数，引导式训练走这里
            String quantizationOn = quantizationParams.get("MODE_QUANTIZATION_ON");
            if ("true".equals(quantizationOn)) {
                params.put("MODE_QUANTIZATION_ON", "true");
                String imageNum = quantizationParams.get("MODE_QUANTIZATION_IMAGE_NUM");
                if (imageNum != null) {
                    params.put("MODE_QUANTIZATION_IMAGE_NUM", imageNum);
                }
            } else {
                params.put("MODE_QUANTIZATION_ON", "false");
            }
        }

        // 更新模型作业参数
        modelJob.setParams(params);
        modelConvertDTO.setParams(params);

        modelJob.setStatus(ModelJobStateCodeConstant.CREATE_CONVERT);
        saveModelJob(modelJob);
        // 状态机创建
//        System.out.println("转换任务状态机创建");
//        stateMachineFactory.createStateMachine((modelJob.getId())+"convert");
        // 状态转换
        ModelJobStateMachine modelJobConvertStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobConvertStateMachine.modelJobConvertEvent(Integer.parseInt(String.valueOf(modelJob.getId())));

        // 截取 modelConvertDTO.getJobName() 的前面部分
        String jobName = modelConvertDTO.getJobName();
        String extractedName = extractJobName(jobName);

        // 使用截取后的名称查找 ModelJob
        ModelJob starterJob = modelJobRepo.findModelJobByName(extractedName);
//        starterJob.setStatus(ModelJob.CONVERTING);
        // 状态机创建
//        System.out.println("转换任务父任务状态机创建");
//        stateMachineFactory.createStateMachine(String.valueOf(starterJob.getId()));
        // 状态转换
        ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobStateMachine.modelJobSonConvertEvent(Integer.parseInt(String.valueOf(starterJob.getId())));

//        saveModelJob(starterJob);

        String targetObjName = null;


        List<ModelJob> modelJobList = modelGenerationById.getModelJobList();

        if (modelJobList == null) {
            ArrayList<ModelJob> modelJobs = new ArrayList<>();
            modelJobs.add(modelJob);
            modelGenerationById.setModelJobList(modelJobs);
        } else {
            modelJobList.add(modelJob);
            modelGenerationById.setModelJobList(modelJobList);
        }
        ModelGeneration.ModelConversion modelConversion = new ModelGeneration.ModelConversion(
                modelConvertDTO.getJobName(),                // jobName
                modelConvertDTO.getImage(),     // image
                modelConvertDTO.getDatasetPath(),      // datasetPath
                modelConvertDTO.getWeightPath(),      // weightPath
                modelConvertDTO.getOutputWeightPath(),  // outputWeightPath
                modelConvertDTO.getGpuNum(),                     // gpuNum
                modelConvertDTO.getCpuNum(),                     // cpuNum
                modelConvertDTO.getMemoryNum(),                  // memoryNum
                modelConvertDTO.getParams()                   // params
        );
        modelGenerationById.setModelConvert(modelConversion);

        modelGenerationService.saveModelGeneration(modelGenerationById);


        // 是否进行数据集的量化
        if (Boolean.parseBoolean(params.get("MODE_QUANTIZATION_ON"))) {
            asyncTaskService.submitQuantizedAndCreateJob(modelJob,modelConvertDTO,Long.parseLong(params.get("MODE_QUANTIZATION_IMAGE_NUM")),isOutsied,selectedConverterOpt,true,starterJob.getTrainDatasetVersions());
        }else{
            asyncTaskService.submitQuantizedAndCreateJob(modelJob,modelConvertDTO, 0L,isOutsied,selectedConverterOpt,false,starterJob.getTrainDatasetVersions());
        }

        logService.persistLogsFromLokiAsync(modelConvertDTO.getJobName());

        return new Msg<>(MsgCode.SUCCEED);
    }

    private String extractJobName(String jobName) {
        if (jobName == null || jobName.isEmpty()) {
            return "";
        }
        // 截取到第四个连字符之前的部分
        int fourthHyphenIndex = -1;
        int hyphenCount = 0;
        for (int i = 0; i < jobName.length(); i++) {
            if (jobName.charAt(i) == '-') {
                hyphenCount++;
                if (hyphenCount == 4) {
                    fourthHyphenIndex = i;
                    break;
                }
            }
        }
        if (fourthHyphenIndex != -1) {
            return jobName.substring(0, fourthHyphenIndex);
        } else {
            return jobName; // 如果没有找到第四个连字符，返回整个字符串
        }
    }

    /**
     * 获取对应的 jobid 的训练集的个数
     *
     * @param jobId
     * @return
     */
    public Long getTrainSetCount(Long jobId) {

        AtomicReference<Long> total = new AtomicReference<>(0L);
        List<DatasetVersionDetailVO> versionLabelCountMapVos = dubheDataFeign.batchGetVersionsLabelCountMap(dubheUtils.getAuthorization(), this.getModelJobById(jobId).getTrainDatasetVersions(),  Collections.emptyList()).getData();
        if (versionLabelCountMapVos == null  || versionLabelCountMapVos.isEmpty()) {
            log.info("Job-"+ jobId +  "绑定数据集为空");
            return 0L;
        }
        versionLabelCountMapVos.forEach(Vo->{
            total.updateAndGet(v -> v + Vo.getImageCount());

        });
        return total.get();

    }


    /**
     * 获取对应的 datasetVersionId 的训练集的个数
     *
     * @param datasetVersionId
     * @return
     */
    public Long getTrainSetCountByDatasetVersionId(Long datasetVersionId) {
        DatasetVersionVO datasetVersion = dubheDataFeign.selectDatasetVersionById(dubheUtils.getAuthorization(), datasetVersionId).getData();
        String versionUrl = datasetVersion.getVersionUrl();
        long picNumber = 0;
        try {
            List<String> strings = minioUtils.listObjectsInFolder(minioUtils.getBucketName(), versionUrl + "/origin");
            picNumber = strings.stream().filter(s -> !s.endsWith(".txt")).count();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return picNumber;
    }

    public List<ModelJob> getConvertJobsByTrainJobID(Long trainJobID) {
        ModelJob trainJob = modelJobRepo.findModelJobById(trainJobID);
        String trainJobName = trainJob.getName();
        String prefix = trainJobName + "%";
        String substring = "%-convert-%";
        return modelJobRepo.findByNameStartingWithAndNameContaining(prefix, substring);

    }


    public int getQueuePositionByModelJobName(String modelJobName) {
        if (modelConversionQueueManager == null) {
            modelConversionQueueManager = new ModelConversionQueueManager(modelJobRepo, jobController, externalModelConverterService);
        }
        return modelConversionQueueManager.getTaskPositionInQueue(modelJobName);
    }


    public Msg<String> stopJob(Long modelGenerationId) {
        ModelGeneration modelGenerationById = modelGenerationService.findModelGenerationById(modelGenerationId);
        List<ModelJob> modelJobList = modelGenerationById.getModelJobList();

        if (modelJobList != null && modelJobList.size() != 0) {
            ModelJob modelJob = modelJobList.get(modelJobList.size() - 1); // 获取最后一个 ModelJob
//            System.out.println("modelJob.getName() = " + modelJob.getName());
//            System.out.println("modelJob.isExecuting() = " + modelJob.isExecuting());
            log.info("正在由于手动中断释放用户资源");
            releaseHardWareResource(modelJob);
            log.info("由于手动中断释放用户资源");
            //释放资源

            if (Objects.equals(modelJob.getStatus(), org.dlut.adv.mineai.core.constant.ModelJobStateCodeConstant.SPLITTING)){
                asyncTaskService.stopDatasetSplitTask(modelJob.getId());
                setCancelState(modelJob);
                // 保存 ModelGeneration 更新
                modelGenerationService.saveModelGeneration(modelGenerationById);
                return new Msg<>(MsgCode.SUCCEED);
            }
            String folderPath = "train_temp/" + modelJob.getId();
            try {

                asyncTaskService.asyncDeleteFolder(minioUtils.getBucketName(), folderPath);
                log.info("已触发异步删除文件夹: {}", folderPath);


            } catch (Exception e) {
                log.error("处理文件夹删除时发生错误: {}, 错误: {}", folderPath, e.getMessage());
            }


            //处理训练任务排队
            jobService.removeFromQueue(modelJob.getName());
            //处理集群外转换排队
            if (modelConversionQueueManager == null) {
                modelConversionQueueManager = new ModelConversionQueueManager(modelJobRepo, jobController, externalModelConverterService);
            }
            switch (modelConversionQueueManager.isTaskInQueueOrRunning(modelJob.getName())) {
                case 0: // 不是集群外转换
                    System.out.println("任务不是集群外转换: " + modelJob.getName());
                    if (modelJob.isExecuting()) {
                        jobController.deleteJob(namespace, modelJob.getName());
//                        modelJob.setStatus(ModelJob.CANCELED);
//                        saveModelJob(modelJob);
                        setCancelState(modelJob);
                    }
                    break;

                case 1: // 正在运行的集群外转换
                    System.out.println("任务正在运行的集群外转换: " + modelJob.getName());
                    if (modelJob.isExecuting()) {
                        jobController.deleteJob(namespace, modelJob.getName());
//                        modelJob.setStatus(ModelJob.CANCELED);
//                        saveModelJob(modelJob);
                        setCancelState(modelJob);
                    }
                    modelConversionQueueManager.removeTaskFromQueue(modelJob.getName());
                    break;

                case 2: // 在队列的集群外转换
                    System.out.println("任务在队列的集群外转换: " + modelJob.getName());
//                    modelJob.setStatus(ModelJob.CANCELED);
//                    saveModelJob(modelJob);
                    setCancelState(modelJob);
                    // 从队列中移除任务
                    modelConversionQueueManager.removeTaskFromQueue(modelJob.getName());
                    break;

                default:
                    System.out.println("未知状态");
                    break;
            }


        }

        // 保存 ModelGeneration 更新
        modelGenerationService.saveModelGeneration(modelGenerationById);
        return new Msg<>(MsgCode.SUCCEED);
    }

    private void setCancelState(ModelJob modelJob) {
        // 状态转换
        ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        if (modelJob.getJobType() != ModelJob.TRAIN) {
            modelJobStateMachine.modelJobTrainCanceledEvent(Integer.parseInt(String.valueOf(modelJob.getId())));
            modelJobStateMachine.modelJobSonTrainCanceledEvent(Integer.parseInt(String.valueOf(modelJob.getName().split("-")[3])));
        } else {
            modelJobStateMachine.modelJobTrainCanceledEvent(Integer.parseInt(String.valueOf(modelJob.getId())));
        }
    }

    private void releaseHardWareResource(ModelJob modelJob) {
        if (modelJob == null) {
            throw new IllegalArgumentException("modelJob cannot be null");
        }
        ModelGeneration modelGeneration = modelGenerationRepo.findById(modelJob.getModelGenerationId()).orElse(null);
        if (modelGeneration == null) {
            throw new IllegalArgumentException("modelGeneration cannot be null");
        }
        HardwareParams hardwareParams = modelGeneration.getHardwareParams();
        if (hardwareParams != null) {
            String jobCpus = modelJob.getCpus() != null ? modelJob.getCpus() : hardwareParams.getCpus();
            String jobMemory = modelJob.getMemory() != null ? modelJob.getMemory() : hardwareParams.getMemory();
            String jobGpuMemory = modelJob.getGpuMemory() != null ? modelJob.getGpuMemory() : hardwareParams.getGpuMemory();

            String memoryNum = jobMemory == null ? null : (jobMemory.contains("G") ? jobMemory.split("G")[0] : jobMemory);
            String gpuMemoryNum = jobGpuMemory == null ? null : (jobGpuMemory.contains("G") ? jobGpuMemory.split("G")[0] : jobGpuMemory);

            String result = dubheUserFeign.releaseUserResource(
                    dubheUtils.getAuthorization(),
                    new UserResourceDTO(
                            modelGeneration.getUserId(),
                            jobCpus == null ? 0L : Long.parseLong(jobCpus),
                            memoryNum == null ? 0L : Long.parseLong(memoryNum),
                            gpuMemoryNum == null ? 0L : Long.parseLong(gpuMemoryNum),
                            Long.parseLong(hardwareParams.getVGpuCores())
                    )
            );
            if (Objects.equals(result, UserResourceDTO.RELEASING_CPU_TO_LARGE)) {
                // throw new IllegalArgumentException("release user's cpu and memory failed because: " + result);
                log.info("release user's cpu and memory failed because: " + result);
            }
            if (Objects.equals(result, UserResourceDTO.RELEASING_MEMORY_TO_LARGE)) {
                // throw new IllegalArgumentException("release user's cpu and memory failed because: " + result);
                log.info("release user's cpu and memory failed because: " + result);
            }
            if (Objects.equals(result, UserResourceDTO.RELEASED_FAILED_UNKNOWN_REASON)) {
                // throw new IllegalArgumentException("release user's cpu and memory failed because: " + result);
                log.info("release user's cpu and memory failed because: " + result);
            }
        }

    }

    public ModelJob findModelJobByName(String jobName) {
        return modelJobRepo.findModelJobByName(jobName);
    }

    public Map<String, Double> getTrainAccAndRecall(ModelJob modelJob) {
        ModelJobLogData logData = modelJobLogDataRepo.findByModelJob(modelJob);
        Map<String, Double> map = new HashMap<>(2);
        map.put("newNowAcc", logData.getAccuracy().doubleValue() * 100);
        map.put("newNowRecall", logData.getRecall().doubleValue() * 100);
        return map;
    }

    public Msg<JobPrecisionInfoDTO> getJobAccLossAndImgCountsByJobName(String jobName) {
        ModelJob modelJob = modelJobRepo.findModelJobByName(jobName);
        return new Msg<>(MsgCode.SUCCEED, getPrecisionInfoForJob(modelJob));
    }


    /**
     * @param modelGenerationId id
     * @param needToFilter 是否需要过滤图片数量
     * @return {@link Msg }<{@link JobPrecisionInfoDTO }>
     */
    public Msg<JobPrecisionInfoDTO> getJobAccLossAndImgCountsByGuidedGenerationId(Long modelGenerationId, Boolean needToFilter) {
        ModelGeneration guidedGeneration = modelGenerationService.findModelGenerationById(modelGenerationId);
        if (guidedGeneration == null) {
            System.out.println("guidedGeneration == null = " + guidedGeneration == null);
            return new Msg<>(MsgCode.SUCCEED, new JobPrecisionInfoDTO(-1d, -1d, 0));
        }
        ModelJob modelJob = modelJobRepo.findModelJobById(guidedGeneration.getLatestJobId());
        if (needToFilter != null && needToFilter) {
            return new Msg<>(MsgCode.SUCCEED, getGuidedPrecisionInfoForJob(modelJob));
        }else {
            return new Msg<>(MsgCode.SUCCEED, getPrecisionInfoForJob(modelJob));
        }
    }

    /**
     * 根据 ModelJob 对象，获取其精度、召回率和图片数量。
     * @param modelJob 模型训练任务对象
     * @return JobPrecisionInfoDTO 包含精度信息的 DTO，如果 modelJob 或其详情无效，则返回默认值的 DTO
     */
    private JobPrecisionInfoDTO getPrecisionInfoForJob(ModelJob modelJob) {
        if (modelJob == null) {
            return new JobPrecisionInfoDTO(0d, 0d, 0);
        }
        Integer imgCount = 0;
        try {
            // 增加对 Feign 调用结果的空指针保护
            DataResponseBody<Integer> imgCountMsg = dubheDataFeign.getVersionsImgCount(dubheUtils.getCurrentAuthorization(), modelJob.getTrainDatasetVersions());
            if (imgCountMsg != null && imgCountMsg.getData() != null) {
                imgCount = imgCountMsg.getData();
            }
        } catch (Exception e) {
            // 记录 Feign 调用异常，但不影响主流程返回
            log.error("Failed to get image count for dataset version id: {}", modelJob.getTrainDatasetVersions(), e);
        }

        Map<String, Double> testedDetail = getTrainAccAndRecall(modelJob);
        if (testedDetail == null) {
            return new JobPrecisionInfoDTO(-1d, -1d, imgCount);
        }

        return new JobPrecisionInfoDTO(
                testedDetail.get("newNowAcc"),
                testedDetail.get("newNowRecall"),
                imgCount
        );
    }
    /**
     * 根据 ModelJob 对象，获取其精度、召回率和图片数量。
     * @param modelJob 模型训练任务对象
     * @return JobPrecisionInfoDTO 包含精度信息的 DTO，如果 modelJob 或其详情无效，则返回默认值的 DTO
     */
    private JobPrecisionInfoDTO getGuidedPrecisionInfoForJob(ModelJob modelJob) {
        if (modelJob == null) {
            return new JobPrecisionInfoDTO(0d, 0d, 0);
        }

        Integer imgCount = 0;
        try {
            // 增加对 Feign 调用结果的空指针保护
            DataResponseBody<Integer> imgCountMsg = dubheDataFeign.getFilteredVersionsImgCount(dubheUtils.getCurrentAuthorization(), modelJob.getTrainDatasetVersions(), new ArrayList<>(modelJob.getSelectedLabels().values()));
            if (imgCountMsg != null && imgCountMsg.getData() != null) {
                imgCount = imgCountMsg.getData();
            }
            System.out.println("imgCountMsg = " + imgCountMsg.getData());
        } catch (Exception e) {
            // 记录 Feign 调用异常，但不影响主流程返回
            log.error("Failed to get image count for dataset version id: {}", modelJob.getTrainDatasetVersionId(), e);
        }

        Map<String, Double> testedDetail = getTrainAccAndRecall(modelJob);
        if (testedDetail == null) {
            return new JobPrecisionInfoDTO(-1d, -1d, imgCount);
        }

        return new JobPrecisionInfoDTO(
                testedDetail.get("newNowAcc"),
                testedDetail.get("newNowRecall"),
                imgCount
        );
    }

    // 获取标准化任务的全部标签
    public List<String> getLabelNamesByModelGenerationId(Long modelGenerationId) {
        // 1. 查询所有符合条件的ModelJob
        List<ModelJob> modelJobs = modelJobRepo.findTrainingJobsByGenerationAndStatus(modelGenerationId);

        // 2. 提取selectedLabels值，过滤空值，去重
        return modelJobs.stream()
                .map(ModelJob::getSelectedLabels)
                .filter(Objects::nonNull)
                .flatMap(labels -> labels.values().stream())
                .filter(label -> label != null && !label.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    public Page<ModelJobInfoCardDTO> findModelJobInfoCards(Long modelGenerationId, int page, int pageSize, String labels) {
        // 1. 创建分页请求对象，默认按ID倒序，最新的任务排在前面
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        // 2. 创建 Specification (动态查询条件)
        Specification<ModelJob> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("modelGenerationId"), modelGenerationId));
            predicates.add(criteriaBuilder.equal(root.get("jobType"), ModelJob.TRAIN));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("status"), 2));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 3. 从数据库分页查询，这一步是高效的
        Page<ModelJob> modelJobPage = modelJobRepo.findAll(spec, pageable);

        // 获取当页的实体列表
        List<ModelJob> pageContent = modelJobPage.getContent();

        // 如果分页内容为空，直接返回一个空的DTO Page对象
        if (pageContent.isEmpty()) {
            return Page.empty(pageable);
        }

        List<ModelJobInfoCardDTO> dtoList = pageContent.stream().filter(modelJob -> {
            // 如果不需要标签过滤，返回true
            if (labels == null) {
                return true;
            }
            // 获取任务的标签列表
            Map<Integer, String> selectedLabels = modelJob.getSelectedLabels();
            if (selectedLabels == null || selectedLabels.isEmpty()) {
                return false;
            }
            Collection<String> labelValues = selectedLabels.values();
            // 检查是否包含搜索的标签（不区分大小写，支持部分匹配）
            return labelValues.stream()
                    .anyMatch(label -> label != null && label.toLowerCase().contains(labels));
        }).map(modelJob -> {
            ModelJobInfoCardDTO cardDTO = new ModelJobInfoCardDTO();
            cardDTO.setJobId(modelJob.getId());
            cardDTO.setJobName(modelJob.getName());

            try {
                JobPrecisionInfoDTO precisionInfo = getJobAccLossAndImgCountsByJobName(modelJob.getName()).getPayload();
                cardDTO.setModelInfo(precisionInfo);
            } catch (Exception e) {
                System.out.println("\"dssdas\" = " + "dssdas");
                cardDTO.setModelInfo(new JobPrecisionInfoDTO(-1.0, -1.0, 0));
            }

            cardDTO.setLabelNames(new ArrayList<>(modelJob.getSelectedLabels().values()));

            return cardDTO;
        }).collect(Collectors.toList());

        // 6. 使用转换后的DTO列表和原始的分页信息，手动创建一个新的Page对象并返回
        return new PageImpl<>(dtoList, modelJobPage.getPageable(), modelJobPage.getTotalElements());
    }

    /**
     * 根据数据集标签匹配查modelJob。
     * @param datasetId 数据集ID
     * @param page 页码
     * @param pageSize 页码大小
     * @return {@link Page }<{@link ModelJobInfoCardDTO }>
     */
    public Page<ModelJobInfoCardDTO> findModalJobInfoCardsByDatasetLabels(Long datasetId, int page, int pageSize) {
        // --- 步骤 1: 获取所有潜在的 ModelJob 和目标数据集的标签 ---

        // 1.1 获取目标数据集的所有标签名称，并放入Set中以便高效查询
        List<DatasetLabelInfoDTO> labelInfoDTOS = dubheDataFeign.getDatasetLabels(dubheUtils.getAuthorization(), datasetId).getData();
        // 使用Set以提高后续的查找效率 (O(1) vs O(n))
        Set<String> targetLabelNames = labelInfoDTOS.stream()
                .map(DatasetLabelInfoDTO::getLabelName)
                .collect(Collectors.toSet());

        // 如果目标数据集没有任何标签，那么不可能有任何匹配，直接返回空页面
        if (targetLabelNames.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), 0);
        }

        // 1.2 找出所有符合基础条件的job（不进行分页！）
        Specification<ModelJob> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("jobType"), ModelJob.TRAIN));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("status"), 2));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        List<ModelJob> allCandidateJobs = modelJobRepo.findAll(spec);

        // --- 步骤 2: 在内存中进行筛选 ---

        // 2.1 遍历所有候选Job，筛选出标签匹配的Job
        List<ModelJob> filteredJobs = new ArrayList<>();
        for (ModelJob modelJob : allCandidateJobs) {
            try {
                // 获取当前Job的训练数据集版本的所有标签
                List<LabelDTO> jobLabels = dubheDataFeign.getDatasetVersionLabelsByVersionId(dubheUtils.getAuthorization(), modelJob.getTrainDatasetVersionId()).getData();
                List<String> jobLabelNames = jobLabels.stream().map(LabelDTO::getName).collect(Collectors.toList());

                // 检查Job的标签列表和目标标签列表是否有任何交集
                // 使用 !Collections.disjoint 比流式操作 anyMatch 更高效
                if (!Collections.disjoint(jobLabelNames, targetLabelNames)) {
                    filteredJobs.add(modelJob);
                }
            } catch (Exception e) {
                log.error("Failed to get labels for model job id: {}", modelJob.getId(), e);
            }
        }

        // --- 步骤 3: 对筛选后的结果进行排序 ---

        // 按照ID倒序排序，与原始分页逻辑保持一致
        filteredJobs.sort(Comparator.comparing(ModelJob::getId).reversed());

        // --- 步骤 4: 手动进行内存分页 ---

        Pageable pageable = PageRequest.of(page, pageSize);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageSize), filteredJobs.size());

        // 如果起始位置超出列表大小，返回一个内容为空但分页信息正确的Page对象
        if (start > filteredJobs.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, filteredJobs.size());
        }

        // 获取当前页的内容
        List<ModelJob> pageContent = filteredJobs.subList(start, end);

        // --- 步骤 5: 将分页后的内容转换为DTO ---

        // 注意：这里可能需要再次获取标签，因为 DTO 中可能需要完整的 LabelDTO 对象。
        // 如果只需要 Job 信息，则不需要再次远程调用。
        // 假设 ModelJobInfoCardDTO 还需要标签列表。
        List<ModelJobInfoCardDTO> dtoList = pageContent.stream().map(modelJob -> {
            ModelJobInfoCardDTO cardDTO = new ModelJobInfoCardDTO();
            cardDTO.setJobId(modelJob.getId());
            cardDTO.setJobName(modelJob.getName());

            // 这一步在上面的筛选逻辑中已经执行过了。
            // 为了避免重复的网络调用，可以考虑在筛选时将Job和它的标签一起缓存起来。
            // 但为了代码简洁和逻辑清晰，这里我们再次调用，这在数据量不大时是可接受的。
            try {
                List<LabelDTO> labels = dubheDataFeign.getDatasetVersionLabelsByVersionId(dubheUtils.getAuthorization(), modelJob.getTrainDatasetVersionId()).getData();
                // 假设你的 DTO 有一个 setLabels 方法
                // cardDTO.setLabels(labels);
            } catch (Exception e) {
                // log.error("Failed to get labels again for model job id: {}", modelJob.getId(), e);
                // cardDTO.setLabels(Collections.emptyList());
            }

            return cardDTO;
        }).collect(Collectors.toList());

        // --- 步骤 6: 构造并返回最终的 Page 对象 ---

        // 使用 PageImpl 构造函数，它需要：
        // 1. 当前页的 DTO 列表 (dtoList)
        // 2. 分页请求信息 (pageable)
        // 3. 筛选后的总记录数 (filteredJobs.size())
        return new PageImpl<>(dtoList, pageable, filteredJobs.size());
    }

    private ModelVersion getTrainModelVersion(ModelGeneration modelGeneration) {
        if (modelGeneration == null) {
            return null;
        }
        if (modelGeneration.getModelExplore() != null) {
            ModelExplore modelExplore = modelExploreService.findModelExploreById(modelGeneration.getModelExplore().getId());
            return modelExplore == null ? null : modelExplore.getTrainModelVersion();
        }
        if (modelGeneration.getModel() != null) {
            Model model = modelRepo.findModelById(modelGeneration.getModel().getId());
            if (model == null) {
                return null;
            }
            if (model.getTrainModelVersion() != null) {
                return model.getTrainModelVersion();
            }
            if (model.getTrainModelJob() != null && model.getTrainModelJob().getModelVersion() != null) {
                return model.getTrainModelJob().getModelVersion();
            }
            if (model.getGenerationId() > 0) {
                ModelGeneration sourceGeneration = modelGenerationService.findModelGenerationById(model.getGenerationId());
                if (sourceGeneration != null) {
                    if (sourceGeneration.getModelExplore() != null) {
                        ModelExplore sourceExplore = modelExploreService.findModelExploreById(sourceGeneration.getModelExplore().getId());
                        if (sourceExplore != null && sourceExplore.getTrainModelVersion() != null) {
                            return sourceExplore.getTrainModelVersion();
                        }
                    }
                    if (sourceGeneration.getModel() != null) {
                        Model sourceModel = modelRepo.findModelById(sourceGeneration.getModel().getId());
                        if (sourceModel != null && sourceModel.getTrainModelVersion() != null) {
                            return sourceModel.getTrainModelVersion();
                        }
                    }
                }
            }
        }
        return null;
    }

    private ModelVersion getConvertModelVersion(ModelGeneration modelGeneration) {
        if (modelGeneration == null) {
            return null;
        }
        if (modelGeneration.getModelExplore() != null) {
            ModelExplore modelExplore = modelExploreService.findModelExploreById(modelGeneration.getModelExplore().getId());
            return modelExplore == null ? null : modelExplore.getConvertModelVersion();
        }
        if (modelGeneration.getModel() != null) {
            Model model = modelRepo.findModelById(modelGeneration.getModel().getId());
            if (model == null) {
                return null;
            }
            if (model.getConvertModelVersion() != null) {
                return model.getConvertModelVersion();
            }
            if (model.getGenerationId() > 0) {
                ModelGeneration sourceGeneration = modelGenerationService.findModelGenerationById(model.getGenerationId());
                if (sourceGeneration != null) {
                    if (sourceGeneration.getModelExplore() != null) {
                        ModelExplore sourceExplore = modelExploreService.findModelExploreById(sourceGeneration.getModelExplore().getId());
                        if (sourceExplore != null && sourceExplore.getConvertModelVersion() != null) {
                            return sourceExplore.getConvertModelVersion();
                        }
                    }
                    if (sourceGeneration.getModel() != null) {
                        Model sourceModel = modelRepo.findModelById(sourceGeneration.getModel().getId());
                        if (sourceModel != null && sourceModel.getConvertModelVersion() != null) {
                            return sourceModel.getConvertModelVersion();
                        }
                    }
                }
            }
        }
        return null;
    }
}
