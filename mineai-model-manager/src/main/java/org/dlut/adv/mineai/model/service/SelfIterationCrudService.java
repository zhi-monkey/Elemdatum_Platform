package org.dlut.adv.mineai.model.service;

import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.core.vo.ProgressVO;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.dto.LabelDTO;
import org.dlut.adv.mineai.model.dto.SelfIterationTaskDTO;
import org.dlut.adv.mineai.model.repository.ModelApplicationRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.repository.ModelVersionRepo;
import org.dlut.adv.mineai.model.repository.SelfIterationJobRepo;
import org.dlut.adv.mineai.model.repository.SelfIterationTaskRepo;
import org.dlut.adv.mineai.model.repository.GpuUrlTargetRepo;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.vo.RtspCaptureExecutionVO;
import org.dlut.adv.mineai.model.vo.SelfIterationJobVO;
import org.dlut.adv.mineai.model.vo.SelfIterationTaskVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自迭代训练 CRUD Service
 * <p>负责父任务和子任务的增删查改，与流程驱动的 SelfIterationService 分离。</p>
 */
@Service
@Slf4j
public class SelfIterationCrudService {

    private static final String INITIAL_AUTO_LABEL_SOURCE_BOUND_DEFAULT = "BOUND_DEFAULT";
    private static final String INITIAL_AUTO_LABEL_SOURCE_DEFAULT_IMAGE = "DEFAULT_IMAGE";
    private static final String INITIAL_AUTO_LABEL_SOURCE_TRAINED_MODEL = "TRAINED_MODEL";
    private static final String INITIAL_AUTO_LABEL_TRAIN_SOURCE_STANDARD = "STANDARD";
    private static final String INITIAL_AUTO_LABEL_TRAIN_SOURCE_GUIDED = "GUIDED";

    @Resource
    private SelfIterationTaskRepo taskRepo;

    @Resource
    private SelfIterationJobRepo jobRepo;

    @Resource
    private ModelApplicationRepo modelApplicationRepo;

    @Resource
    private ModelJobRepo modelJobRepo;

    @Resource
    private ModelVersionRepo modelVersionRepo;

    @Resource
    private HardwareParamsService hardwareParamsService;

    @Resource
    private GpuUrlTargetService gpuUrlTargetService;

    @Resource
    private GpuUrlTargetRepo gpuUrlTargetRepo;

    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private DubheUtils dubheUtils;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private SelfIterationService selfIterationService;

    @Value("${datasetRootPath}")
    private String datasetRootPath;

    @Value("${container.weightPath}")
    private String weightPath;

    @Resource
    private ModelApplicationZipLabelService zipLabelService;

    @Resource
    private ModelGenerationService modelGenerationService;

    // ===================== Task CRUD =====================

    /**
     * 创建父任务
     * <p>参考引导式训练流程，自动创建 RtspCaptureTask，从 ModelApplication 自动填充配置</p>
     */
    @Transactional
    public SelfIterationTaskVO createTask(SelfIterationTaskDTO dto) {
        validateIterationStopConfig(dto);

        // 获取当前用户 ID
        Long userId = Long.valueOf(UserContextHolder.getUserContext().getId());

        // 根据 applicationName 和 deviceFirmware 查找 ModelApplication
        ModelApplication modelApplication = modelApplicationRepo.findByApplicationNameAndDeviceAndFirmware(
                dto.getApplicationName(),
                dto.getDeviceFirmware()
        );
        if (modelApplication == null) {
            throw new IllegalArgumentException("未找到匹配的 ModelApplication：applicationName="
                    + dto.getApplicationName() + ", deviceFirmware=" + dto.getDeviceFirmware());
        }

        // 查找 HardwareParams（如果前端没传，使用默认值）
        HardwareParams hardwareParams = null;
        if (dto.getHardwareParamsId() != null) {
            hardwareParams = hardwareParamsService.findHardwareParamsById(dto.getHardwareParamsId());
            if (hardwareParams == null) {
                throw new IllegalArgumentException("HardwareParams 不存在：id=" + dto.getHardwareParamsId());
            }
        } else {
            // 使用默认硬件配置
            hardwareParams = hardwareParamsService.getDefaultHardwareParams();
        }

        // 确定数据集组名称
        String datasetGroupName = dto.getDatasetGroupName();
        if ((datasetGroupName == null || datasetGroupName.trim().isEmpty()) && dto.getDatasetGroupId() == null) {
            datasetGroupName = "iter-" + dto.getTaskName() + "-" + System.currentTimeMillis();
        }

        // 校验数据源可用性（必须在线且未删除，且不能重复选择）
        validateDataSources(dto.getDataSources());
        Integer iterationStartImageQuantity = resolveIterationStartImageQuantity(dto, null);

        // 从 ModelApplication 获取绑定的标签 ID 列表（dubhe-data 全局 label.id）
        List<Long> boundLabelIds = zipLabelService.getBoundLabelIds(modelApplication.getId());
        if (boundLabelIds == null) boundLabelIds = new ArrayList<>();

        // 构建 index→name Map（对齐 ModelGeneration.selectedLabels），供父任务持久化和前端展示
        Map<Integer, String> selectedLabels = buildSelectedLabels(boundLabelIds);

        // 确定标注类型（102=目标检测，103=语义分割）
        int annotateType = resolveAnnotateType(modelApplication);

        // 为每个数据源创建 RtspCaptureTask（HTTP类型）
        List<Long> captureTaskIds = new ArrayList<>();
        if (dto.getDataSources() != null && !dto.getDataSources().isEmpty()) {
            for (int i = 0; i < dto.getDataSources().size(); i++) {
                SelfIterationTaskDTO.DataSourceConfig dataSource = dto.getDataSources().get(i);

                DubheDataFeign.RtspCaptureTaskCreateDTO captureTaskDTO = new DubheDataFeign.RtspCaptureTaskCreateDTO();
                captureTaskDTO.setTaskName("capture-" + dto.getTaskName() + "-" + (i + 1));
                captureTaskDTO.setSourceType("HTTP");
                captureTaskDTO.setHttpCameraId(dataSource.getHttpCameraId());
                captureTaskDTO.setDatasetGroupId(dto.getDatasetGroupId());
                captureTaskDTO.setDatasetGroupName(datasetGroupName);
                captureTaskDTO.setCaptureInterval(dataSource.getCaptureInterval());
                captureTaskDTO.setImageQuantity(dataSource.getImageQuantity());
                captureTaskDTO.setAnnotateType(annotateType);
                // 传入 label ID 列表，dubhe-data 创建数据集后调 LabelService.save() 绑定标签（等价于 bandLabels）
                captureTaskDTO.setLabelIds(!boundLabelIds.isEmpty() ? boundLabelIds : null);

                try {
                    DataResponseBody<?> captureTaskResp = dubheDataFeign.createRtspCaptureTask(
                            dubheUtils.getCurrentAuthorization(), captureTaskDTO);
                    if (captureTaskResp == null || !captureTaskResp.succeed()) {
                        throw new RuntimeException("创建 RtspCaptureTask 失败：" +
                                (captureTaskResp != null ? captureTaskResp.getMsg() : "response 为空"));
                    }

                    // 处理返回值：可能是 Long 或 Boolean
                    Object data = captureTaskResp.getData();
                    Long captureTaskId = null;
                    if (data instanceof Long) {
                        captureTaskId = (Long) data;
                    } else if (data instanceof Integer) {
                        captureTaskId = ((Integer) data).longValue();
                    } else if (data instanceof String) {
                        String str = ((String) data).trim();
                        if (!str.isEmpty() && str.matches("^-?\\d+$")) {
                            captureTaskId = Long.parseLong(str);
                        }
                    } else if (data instanceof Boolean && (Boolean) data) {
                        // 如果返回 true，说明创建成功但没有返回ID，按任务名回查真实ID
                        captureTaskId = resolveCaptureTaskIdByTaskName(captureTaskDTO.getTaskName());
                        if (captureTaskId == null) {
                            log.warn("RtspCaptureTask 创建成功但未回查到ID，使用临时标识：taskName={}", captureTaskDTO.getTaskName());
                            captureTaskId = -1L;
                        }
                    } else {
                        throw new RuntimeException("创建 RtspCaptureTask 返回数据格式异常：" + data);
                    }

                    captureTaskIds.add(captureTaskId);
                } catch (Exception e) {
                    log.error("创建 RtspCaptureTask 失败：{}", e.getMessage(), e);
                    throw new RuntimeException("创建数据回流任务失败：" + e.getMessage(), e);
                }
            }
        } else {
            throw new IllegalArgumentException("至少需要配置一个数据源");
        }

        // 构建 Task 实体
        SelfIterationTask task = new SelfIterationTask();
        task.setName(dto.getTaskName());
        task.setDescription(dto.getDescription());
        task.setModelApplication(modelApplication);
        task.setDataSourceIds(captureTaskIds);
        task.setDeleteRawAfterCollect(dto.isDeleteRawAfterCollect());
        task.setRequireManualReview(dto.isRequireManualReview());
        task.setAutoDeployEnabled(dto.isAutoDeployEnabled());
        task.setMaxRounds(dto.getMaxRounds());
        task.setIterationStartImageQuantity(iterationStartImageQuantity);
        task.setTargetAccuracy(dto.getTargetAccuracy());
        task.setTargetRecall(dto.getTargetRecall());
        setConfidenceThreshold(task, dto.getConfidenceThreshold());
        task.setReuseAnnotationModel(dto.getReuseAnnotationModel() != null ? dto.getReuseAnnotationModel() : "ALWAYS");
        task.setReuseTrainModel(dto.getReuseTrainModel() != null ? dto.getReuseTrainModel() : "ALWAYS");
        task.setHardwareParams(hardwareParams);
        task.setGpuMode(dto.getGpuMode() != null ? dto.getGpuMode() : "single-exclusive");
        task.setGpuCount(dto.getGpuCount());
        task.setSplitSize(dto.getSplitSize());
        task.setInferenceDeviceIds(normalizeInferenceDeviceIds(dto.getInferenceDeviceIds()));
        task.setGpuUrlTargetIds(gpuUrlTargetService.normalizeIds(dto.getGpuUrlTargetIds()));
        task.setUserId(userId);
        task.setStatus(SelfIterationTask.STATUS_IDLE);
        task.setCurrentRound(0);
        task.setAllDatasetVersionIds(new ArrayList<>());

        // 从 ModelApplication → Model 自动填充配置
        Model model = modelApplication.getModel();
        if (model != null) {
            applyInitialAutoLabelConfig(task, dto, modelApplication, model, false);

            // 从 Model.trainModelVersion 获取 annotationType
            ModelVersion trainModelVersion = model.getTrainModelVersion();
            Map<String, String> hyperParams = dto.getTrainParams() != null
                    ? new java.util.HashMap<>(dto.getTrainParams())
                    : new java.util.HashMap<>();
            if (trainModelVersion != null) {
                String annotationType = trainModelVersion.getAnnotationType();
                if (annotationType != null) {
                    hyperParams.put("ANNOTATION_TYPE", annotationType);
                }
                task.setModelType(trainModelVersion.getUrl() != null ? "PRETRAINED" : "SCRATCH");
            } else {
                task.setModelType("OFFICIAL");
            }
            task.setHyperParams(hyperParams);
        } else {
            applyInitialAutoLabelConfig(task, dto, modelApplication, null, false);
            task.setHyperParams(dto.getTrainParams());
            task.setModelType("OFFICIAL");
        }

        // 转换参数
        task.setConvertParams(dto.getConvertParams());
        task.setAuthCode(resolveAuthCode(dto));

        // 标签信息（index→name，对齐 ModelGeneration.selectedLabels，保存后供前端展示）
        task.setSelectedLabels(selectedLabels.isEmpty() ? null : selectedLabels);

        task = taskRepo.save(task);
        log.info("创建自迭代任务成功：taskId={}, captureTaskIds={}", task.getId(), captureTaskIds);
        return toTaskVO(task);
    }

    /**
     * 更新父任务（仅 IDLE 状态允许修改）
     */
    @Transactional
    public SelfIterationTaskVO updateTask(SelfIterationTaskDTO dto) {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("更新任务时 id 不能为空");
        }
        validateIterationStopConfig(dto);

        SelfIterationTask task = requireVisibleTask(dto.getId());

        if (task.getStatus() == SelfIterationTask.STATUS_COMPLETED
                || task.getStatus() == SelfIterationTask.STATUS_CANCELLED
                || task.getStatus() == SelfIterationTask.STATUS_FAILED) {
            throw new IllegalStateException("当前任务状态不允许修改配置，当前状态：" + task.getStatus());
        }

        // 允许修改的字段
        task.setName(dto.getTaskName());
        task.setDescription(dto.getDescription());
        task.setDeleteRawAfterCollect(dto.isDeleteRawAfterCollect());
        task.setRequireManualReview(dto.isRequireManualReview());
        task.setAutoDeployEnabled(dto.isAutoDeployEnabled());
        task.setMaxRounds(dto.getMaxRounds());
        task.setIterationStartImageQuantity(resolveIterationStartImageQuantity(dto, task));
        task.setTargetAccuracy(dto.getTargetAccuracy());
        task.setTargetRecall(dto.getTargetRecall());
        setConfidenceThreshold(task, dto.getConfidenceThreshold());
        task.setReuseAnnotationModel(dto.getReuseAnnotationModel() != null ? dto.getReuseAnnotationModel() : "ALWAYS");
        task.setReuseTrainModel(dto.getReuseTrainModel() != null ? dto.getReuseTrainModel() : "ALWAYS");
        task.setGpuMode(dto.getGpuMode() != null ? dto.getGpuMode() : "single-exclusive");
        task.setGpuCount(dto.getGpuCount());
        task.setSplitSize(dto.getSplitSize());
        task.setHyperParams(dto.getTrainParams());
        task.setConvertParams(dto.getConvertParams());
        task.setAuthCode(resolveAuthCode(dto));
        task.setInferenceDeviceIds(normalizeInferenceDeviceIds(dto.getInferenceDeviceIds()));
        task.setGpuUrlTargetIds(gpuUrlTargetService.normalizeIds(dto.getGpuUrlTargetIds()));

        // 若 applicationName/deviceFirmware 变更，重新查找并更新 ModelApplication
        ModelApplication currentApp = task.getModelApplication();
        String currentAppName = currentApp != null && currentApp.getApplicationName() != null
                ? currentApp.getApplicationName().getApplicationName() : null;
        String currentDeviceFirmware = currentApp != null && currentApp.getDevice() != null
                ? currentApp.getDevice().getDeviceName() + "-" + currentApp.getDevice().getFirmwareVersion() : null;

        if (!dto.getApplicationName().equals(currentAppName)
                || !dto.getDeviceFirmware().equals(currentDeviceFirmware)) {
            ModelApplication modelApplication = modelApplicationRepo.findByApplicationNameAndDeviceAndFirmware(
                    dto.getApplicationName(),
                    dto.getDeviceFirmware()
            );
            if (modelApplication == null) {
                throw new IllegalArgumentException("未找到匹配的 ModelApplication：applicationName="
                        + dto.getApplicationName() + ", deviceFirmware=" + dto.getDeviceFirmware());
            }
            task.setModelApplication(modelApplication);

            Model model = modelApplication.getModel();
            applyInitialAutoLabelConfig(task, dto, modelApplication, model, false);
        } else {
            // applicationName/deviceFirmware 未变更，但用户指定了新镜像
            applyInitialAutoLabelConfig(task, dto, currentApp, currentApp != null ? currentApp.getModel() : null, true);
        }

        // 若 hardwareParamsId 变更
        if (dto.getHardwareParamsId() != null && !dto.getHardwareParamsId().equals(task.getHardwareParams().getId())) {
            HardwareParams hardwareParams = hardwareParamsService.findHardwareParamsById(dto.getHardwareParamsId());
            if (hardwareParams == null) {
                throw new IllegalArgumentException("HardwareParams 不存在：id=" + dto.getHardwareParamsId());
            }
            task.setHardwareParams(hardwareParams);
        }

        task = taskRepo.save(task);
        return toTaskVO(task);
    }

    /**
     * 删除父任务（仅 IDLE/COMPLETED/CANCELLED/FAILED 状态允许）
     */
    @Transactional
    public void deleteTask(Long taskId) {
        SelfIterationTask task = requireVisibleTask(taskId);

        int status = task.getStatus();
        if (status != SelfIterationTask.STATUS_IDLE
                && status != SelfIterationTask.STATUS_COMPLETED
                && status != SelfIterationTask.STATUS_CANCELLED
                && status != SelfIterationTask.STATUS_FAILED) {
            throw new IllegalStateException("运行中的任务不允许删除，当前状态：" + status);
        }

        // 级联删除已配置（CascadeType.ALL），直接删除父任务
        taskRepo.deleteById(taskId);
    }

    /**
     * 查询单个 Task 详情（含 Job 列表）
     */
    public SelfIterationTaskVO getTask(Long taskId) {
        SelfIterationTask task = requireVisibleTask(taskId);
        return toTaskVO(task);
    }

    /**
     * 查询当前用户的 Task 列表（分页，可按状态筛选）
     */
    public Page<SelfIterationTaskVO> listTasks(int page, int pageSize, Integer status) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.DESC, "id");

        Page<SelfIterationTask> taskPage;
        List<SelfIterationTask> visibleTasks = listVisibleTasks();
        if (status != null) {
            List<SelfIterationTask> filtered = visibleTasks.stream()
                    .filter(t -> t.getStatus() == status)
                    .collect(Collectors.toList());
            List<SelfIterationTask> pageContent = pageSlice(filtered, pageable);
            taskPage = new PageImpl<>(pageContent, pageable, filtered.size());
        } else {
            List<SelfIterationTask> pageContent = pageSlice(visibleTasks, pageable);
            taskPage = new PageImpl<>(pageContent, pageable, visibleTasks.size());
        }

        List<SelfIterationTaskVO> voList = taskPage.getContent().stream()
                .map(this::toTaskVO)
                .collect(Collectors.toList());
        return new PageImpl<>(voList, pageable, taskPage.getTotalElements());
    }

    /**
     * 查询所有 Task 列表（不分页，返回 VO）
     */
    public List<SelfIterationTaskVO> listAllTasks() {
        List<SelfIterationTask> tasks = listVisibleTasks();
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<SelfIterationJob>> jobsByTaskId = new HashMap<>();
        for (SelfIterationJob job : jobRepo.findAll()) {
            if (job.getSelfIterationTask() == null) {
                continue;
            }
            Long taskId = job.getSelfIterationTask().getId();
            jobsByTaskId.computeIfAbsent(taskId, k -> new ArrayList<>()).add(job);
        }
        for (List<SelfIterationJob> jobs : jobsByTaskId.values()) {
            jobs.sort((a, b) -> Integer.compare(a.getRound(), b.getRound()));
        }

        return tasks.stream()
                .map(task -> toTaskVO(task, jobsByTaskId.get(task.getId()), false))
                .collect(Collectors.toList());
    }

    // ===================== Job 查询（只读） =====================

    /**
     * 查询某 Task 下所有 Job（按轮次升序）
     */
    public List<SelfIterationJobVO> listJobs(Long taskId) {
        requireVisibleTask(taskId);
        List<SelfIterationJob> jobs = jobRepo.findBySelfIterationTask_IdOrderByRoundAsc(taskId);
        return jobs.stream().map(this::toJobVO).collect(Collectors.toList());
    }

    /**
     * 查询单个 Job 详情
     */
    public SelfIterationJobVO getJob(Long jobId) {
        SelfIterationJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在：id=" + jobId));
        assertVisibleTask(job.getSelfIterationTask());
        return toJobVO(job);
    }

    // ===================== 前端表单选项接口 =====================

    /**
     * 获取创建任务表单的选项数据
     * TODO: 实现完整的选项查询逻辑，当前返回空结构
     */
    public Map<String, Object> getFormOptions() {
        Map<String, Object> options = new LinkedHashMap<>();
        Long currentUserId = null;
        try {
            currentUserId = Long.valueOf(UserContextHolder.getUserContext().getId());
        } catch (Exception e) {
            log.warn("获取当前用户ID失败，formOptions 返回精简选项", e);
        }

        options.put("modelApplicationOptions", listModelApplicationOptions());
        options.put("hardwareParamsOptions", listHardwareParamsOptions(currentUserId));
        options.put("dataSourceOptions", listDataSourceOptions());
        options.put("inferenceDeviceOptions", listInferenceDeviceOptions());
        options.put("gpuUrlTargetOptions", listGpuUrlTargetOptions());

        // 参数模板依赖具体应用模型，创建页在选择应用后由前端按既有接口动态查询
        options.put("trainParamConfigList", new ArrayList<>());
        options.put("convertParamConfigList", new ArrayList<>());
        return options;
    }

    /**
     * 获取子任务详细信息（只读，不触发状态推进）
     * 前端轮询使用此接口，避免重复触发状态推进
     */
    public Map<String, Object> getJobDetailReadOnly(Long jobId) {
        SelfIterationJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在：id=" + jobId));
        SelfIterationTask task = job.getSelfIterationTask();
        assertVisibleTask(task);

        List<CaptureRuntimeSnapshot> snapshots = loadCaptureRuntimeSnapshots(job.getCaptureExecutionIds());

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", job.getId());
        detail.put("parentTaskId", task != null ? task.getId() : null);
        detail.put("round", job.getRound());
        detail.put("status", mapJobStatusToUi(job.getStatus()));
        detail.put("currentStep", mapPhaseToUi(job.getPhase()));
        detail.put("stepStatus", buildStepStatus(job));
        detail.put("createTime", job.getCreateTime());
        detail.put("updateTime", job.getUpdateTime());
        detail.put("needManualReview",
                (task != null && task.isRequireManualReview())
                        || job.getStatus() == SelfIterationJob.STATUS_WAITING_REVIEW);

        detail.put("dataCollection", buildDataCollectionDetail(job, snapshots));
        detail.put("dataAnnotation", buildDataAnnotationDetail(task, job, snapshots));
        detail.put("modelIteration", buildModelIterationDetail(task, job));
        detail.put("modelDispatch", buildModelDispatchDetail(task, job));

        return detail;
    }

    /**
     * 获取子任务详细信息（包含各阶段详情）
     * TODO: 实现完整的详情查询逻辑，当前返回基础信息
     */
    public Map<String, Object> getJobDetail(Long jobId) {
        // 拉取详情前先刷新采集进度，保证前端看到实时状态
        try {
            selfIterationService.refreshCollectProgress(jobId);
        } catch (Exception e) {
            log.warn("刷新采集进度失败：jobId={}", jobId, e);
        }

        // 拉取详情前刷新训练/转换阶段进度，兜底推进模型迭代链路
        try {
            selfIterationService.refreshModelIterationProgress(jobId);
        } catch (Exception e) {
            log.warn("刷新模型迭代进度失败：jobId={}", jobId, e);
        }

        SelfIterationJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("子任务不存在：id=" + jobId));
        SelfIterationTask task = job.getSelfIterationTask();
        assertVisibleTask(task);

        List<CaptureRuntimeSnapshot> snapshots = loadCaptureRuntimeSnapshots(job.getCaptureExecutionIds());

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", job.getId());
        detail.put("parentTaskId", task != null ? task.getId() : null);
        detail.put("round", job.getRound());
        detail.put("status", mapJobStatusToUi(job.getStatus()));
        detail.put("currentStep", mapPhaseToUi(job.getPhase()));
        detail.put("stepStatus", buildStepStatus(job));
        detail.put("createTime", job.getCreateTime());
        detail.put("updateTime", job.getUpdateTime());
        detail.put("needManualReview",
                (task != null && task.isRequireManualReview())
                        || job.getStatus() == SelfIterationJob.STATUS_WAITING_REVIEW);

        detail.put("dataCollection", buildDataCollectionDetail(job, snapshots));
        detail.put("dataAnnotation", buildDataAnnotationDetail(task, job, snapshots));
        detail.put("modelIteration", buildModelIterationDetail(task, job));
        detail.put("modelDispatch", buildModelDispatchDetail(task, job));

        return detail;
    }

    private Map<String, Object> buildStepStatus(SelfIterationJob job) {
        Map<String, Object> stepStatus = new LinkedHashMap<>();
        String dataCollection = "wait";
        String dataAnnotation = "wait";
        String modelIteration = "wait";
        String modelDispatch = "wait";

        if (job.getPhase() == SelfIterationJob.PHASE_DATA_COLLECT) {
            dataCollection = job.getStatus() == SelfIterationJob.STATUS_COLLECT_FAILED ? "error" : "process";
        } else if (job.getPhase() == SelfIterationJob.PHASE_DATA_ANNOTATE) {
            dataCollection = "finish";
            dataAnnotation = job.getStatus() == SelfIterationJob.STATUS_ANNOTATE_FAILED ? "error" : "process";
        } else if (job.getPhase() == SelfIterationJob.PHASE_MODEL_TRAIN) {
            dataCollection = "finish";
            dataAnnotation = "finish";
            modelIteration = (job.getStatus() == SelfIterationJob.STATUS_TRAIN_FAILED
                    || job.getStatus() == SelfIterationJob.STATUS_PACKAGE_FAILED) ? "error" : "process";
        } else if (job.getPhase() == SelfIterationJob.PHASE_MODEL_DEPLOY) {
            dataCollection = "finish";
            dataAnnotation = "finish";
            modelIteration = "finish";
            modelDispatch = "process";
        }

        if (job.getStatus() == SelfIterationJob.STATUS_COMPLETED) {
            dataCollection = "finish";
            dataAnnotation = "finish";
            modelIteration = "finish";
            modelDispatch = "finish";
        }

        stepStatus.put("dataCollection", dataCollection);
        stepStatus.put("dataAnnotation", dataAnnotation);
        stepStatus.put("modelIteration", modelIteration);
        stepStatus.put("modelDispatch", modelDispatch);
        return stepStatus;
    }

    private Map<String, Object> buildDataCollectionDetail(SelfIterationJob job, List<CaptureRuntimeSnapshot> snapshots) {
        Map<String, Object> section = new LinkedHashMap<>();

        List<Map<String, Object>> cameras = new ArrayList<>();
        List<Map<String, Object>> datasets = new ArrayList<>();
        Set<Long> datasetIdSet = new HashSet<>();

        long collectedImageCount = 0L;
        long targetImageCount = 0L;
        long totalImageCount = 0L;
        int successCount = 0;

        for (CaptureRuntimeSnapshot snapshot : snapshots) {
            Map<String, Object> cameraItem = new LinkedHashMap<>();
            cameraItem.put("rtspSourceName", snapshot.captureTaskName != null ? snapshot.captureTaskName : "采集任务-" + snapshot.captureTaskId);
            cameraItem.put("cameraName", snapshot.httpCameraId != null ? ("HTTP摄像机-" + snapshot.httpCameraId) : "HTTP摄像机-未知");
            cameraItem.put("requiredImages", snapshot.imageQuantity == null ? 0 : snapshot.imageQuantity);
            cameraItem.put("collectedImages", snapshot.capturedCount == null ? 0 : snapshot.capturedCount);
            cameraItem.put("sampleInterval", snapshot.captureInterval == null ? 0 : snapshot.captureInterval);
            cameraItem.put("sampleStatus", mapCaptureStatusToUi(snapshot.executionStatus));
            cameraItem.put("executionId", snapshot.executionId);
            cameraItem.put("captureTaskId", snapshot.captureTaskId);
            cameraItem.put("datasetId", snapshot.datasetId);
            cameraItem.put("message", snapshot.message);
            cameras.add(cameraItem);

            if (snapshot.capturedCount != null) {
                collectedImageCount += Math.max(snapshot.capturedCount, 0);
            }
            if (snapshot.imageQuantity != null) {
                targetImageCount += Math.max(snapshot.imageQuantity, 0);
            }
            if (snapshot.executionStatus != null && snapshot.executionStatus == 2) {
                successCount++;
            }

            if (snapshot.datasetId != null && datasetIdSet.add(snapshot.datasetId)) {
                Map<String, Object> ds = new LinkedHashMap<>();
                ds.put("datasetId", snapshot.datasetId);
                ds.put("datasetName", snapshot.datasetName != null ? snapshot.datasetName : ("dataset-" + snapshot.datasetId));
                ds.put("imageCount", snapshot.datasetImageCount == null ? 0 : snapshot.datasetImageCount);
                ds.put("videoCount", 0);
                datasets.add(ds);
                totalImageCount += snapshot.datasetImageCount == null ? 0 : snapshot.datasetImageCount;
            }
        }

        String datasetGroupName = snapshots.stream()
                .map(s -> s.datasetGroupName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (cameras.isEmpty() && job.getSelfIterationTask() != null && job.getSelfIterationTask().getDataSourceIds() != null) {
            for (Long captureTaskId : job.getSelfIterationTask().getDataSourceIds()) {
                if (captureTaskId == null || captureTaskId <= 0) {
                    continue;
                }
                CaptureRuntimeSnapshot snapshot = new CaptureRuntimeSnapshot();
                snapshot.captureTaskId = captureTaskId;
                try {
                    DataResponseBody<DubheDataFeign.RtspCaptureTaskVO> taskResp = dubheDataFeign.getRtspCaptureTaskDetail(
                            dubheUtils.getAuthorization(), captureTaskId);
                    if (taskResp != null && taskResp.succeed() && taskResp.getData() != null) {
                        DubheDataFeign.RtspCaptureTaskVO captureTask = taskResp.getData();
                        snapshot.captureTaskName = captureTask.getTaskName();
                        snapshot.httpCameraId = captureTask.getHttpCameraId();
                        snapshot.imageQuantity = captureTask.getImageQuantity();
                        snapshot.captureInterval = captureTask.getCaptureInterval();
                        if (captureTask.getDatasetGroupId() != null) {
                            snapshot.datasetGroupName = "数据集组-" + captureTask.getDatasetGroupId();
                        }
                        if (datasetGroupName == null && snapshot.datasetGroupName != null) {
                            datasetGroupName = snapshot.datasetGroupName;
                        }
                        if (snapshot.imageQuantity != null) {
                            targetImageCount += Math.max(snapshot.imageQuantity, 0);
                        }
                    }
                } catch (Exception e) {
                    log.warn("查询采集任务详情失败：captureTaskId={}", captureTaskId, e);
                }

                Map<String, Object> cameraItem = new LinkedHashMap<>();
                cameraItem.put("rtspSourceName", snapshot.captureTaskName != null ? snapshot.captureTaskName : "采集任务-" + captureTaskId);
                cameraItem.put("cameraName", snapshot.httpCameraId != null ? ("HTTP摄像机-" + snapshot.httpCameraId) : "HTTP摄像机-未知");
                cameraItem.put("requiredImages", snapshot.imageQuantity == null ? 0 : snapshot.imageQuantity);
                cameraItem.put("collectedImages", 0);
                cameraItem.put("sampleInterval", snapshot.captureInterval == null ? 0 : snapshot.captureInterval);
                cameraItem.put("sampleStatus", "pending");
                cameraItem.put("executionId", null);
                cameraItem.put("captureTaskId", captureTaskId);
                cameraItem.put("datasetId", null);
                cameraItem.put("message", null);
                cameras.add(cameraItem);
            }
        }

        if (datasetGroupName == null) {
            datasetGroupName = "未绑定数据集组";
        }

        Map<String, Object> groupNode = new LinkedHashMap<>();
        groupNode.put("datasetGroupName", datasetGroupName);
        groupNode.put("datasets", datasets);

        List<Map<String, Object>> datasetCascade = new ArrayList<>();
        datasetCascade.add(groupNode);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalDatasetCount", datasets.size());
        overview.put("totalImageCount", totalImageCount > 0 ? totalImageCount : targetImageCount);
        overview.put("totalVideoCount", 0);
        overview.put("collectedImageCount", collectedImageCount);

        section.put("cameras", cameras);
        section.put("datasetCascade", datasetCascade);
        section.put("overview", overview);
        section.put("canRetry", job.getStatus() == SelfIterationJob.STATUS_COLLECT_FAILED);
        section.put("successCount", successCount);
        section.put("totalCount", Math.max(snapshots.size(), cameras.size()));
        return section;
    }

    private Map<String, Object> buildDataAnnotationDetail(SelfIterationTask task, SelfIterationJob job, List<CaptureRuntimeSnapshot> snapshots) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("baselineModelName", resolveBaselineModelName(task, job));
        List<String> labels = new ArrayList<>();
        if (task != null && task.getSelectedLabels() != null && !task.getSelectedLabels().isEmpty()) {
            labels.addAll(task.getSelectedLabels().values());
        }
        section.put("labels", labels);
        section.put("confidenceThreshold", resolveConfidenceThreshold(task));
        boolean canOperate = (task != null && task.isRequireManualReview())
                || (job != null && job.getStatus() == SelfIterationJob.STATUS_WAITING_REVIEW);
        if (canOperate && task != null && selfIterationService.isReviewProcessing(task.getId())) {
            canOperate = false;
        }
        section.put("canOperate", canOperate);

        List<Map<String, Object>> datasetStatuses = new ArrayList<>();
        List<Long> datasetIds = snapshots.stream()
                .map(snapshot -> snapshot.datasetId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, ProgressVO> progressMap = loadDatasetProgressMap(datasetIds);
        Map<Long, Map<String, Object>> statusMap = loadDatasetStatusMap(datasetIds);
        Set<Long> datasetIdSet = new HashSet<>();
        for (CaptureRuntimeSnapshot snapshot : snapshots) {
            if (snapshot.datasetId == null || !datasetIdSet.add(snapshot.datasetId)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            ProgressVO progress = progressMap.get(snapshot.datasetId);
            int annotatedImages = calculateAnnotatedImages(progress);
            int totalImages = calculateTotalImages(progress, snapshot.datasetImageCount);
            item.put("datasetName", snapshot.datasetName != null ? snapshot.datasetName : ("dataset-" + snapshot.datasetId));
            item.put("annotatedImages", annotatedImages);
            item.put("totalImages", totalImages);
            item.put("extractedVideos", 0);
            item.put("totalVideos", 0);
            item.put("annotationType", "auto");
            item.put("status", mapDatasetAnnotationStatus(statusMap.get(snapshot.datasetId), progress, job));
            datasetStatuses.add(item);
        }
        section.put("datasetStatuses", datasetStatuses);
        return section;
    }

    private Map<Long, ProgressVO> loadDatasetProgressMap(List<Long> datasetIds) {
        if (datasetIds == null || datasetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            DataResponseBody<Map<Long, ProgressVO>> resp = dubheDataFeign.getDatasetsProgress(
                    dubheUtils.getAuthorization(), datasetIds);
            if (resp != null && resp.succeed() && resp.getData() != null) {
                return resp.getData();
            }
        } catch (Exception e) {
            log.warn("查询数据集标注进度失败：datasetIds={}", datasetIds, e);
        }
        return Collections.emptyMap();
    }

    private Map<Long, Map<String, Object>> loadDatasetStatusMap(List<Long> datasetIds) {
        if (datasetIds == null || datasetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            DataResponseBody<Map<Long, Map<String, Object>>> resp = dubheDataFeign.getDatasetStatus(
                    dubheUtils.getAuthorization(), datasetIds);
            if (resp != null && resp.succeed() && resp.getData() != null) {
                return resp.getData();
            }
        } catch (Exception e) {
            log.warn("查询数据集状态失败：datasetIds={}", datasetIds, e);
        }
        return Collections.emptyMap();
    }

    private int calculateAnnotatedImages(ProgressVO progress) {
        if (progress == null) {
            return 0;
        }
        long annotated = safeLong(progress.getFinished())
                + safeLong(progress.getAutoFinished())
                + safeLong(progress.getFinishAutoTrack())
                + safeLong(progress.getAnnotationNotDistinguishFile());
        return annotated > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) annotated;
    }

    private int calculateTotalImages(ProgressVO progress, Integer fallbackTotal) {
        if (progress == null) {
            return fallbackTotal == null ? 0 : Math.max(fallbackTotal, 0);
        }
        long total = safeLong(progress.getFinished())
                + safeLong(progress.getAutoFinished())
                + safeLong(progress.getFinishAutoTrack())
                + safeLong(progress.getAnnotationNotDistinguishFile())
                + safeLong(progress.getUnfinished());
        if (total <= 0) {
            return fallbackTotal == null ? 0 : Math.max(fallbackTotal, 0);
        }
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : Math.max(value, 0L);
    }

    private String mapDatasetAnnotationStatus(Map<String, Object> statusInfo, ProgressVO progress, SelfIterationJob job) {
        Integer status = extractInteger(statusInfo, "status");
        if (status != null) {
            if (status == 103) {
                return "processing";
            }
            if (status == 104) {
                return job != null && job.getStatus() == SelfIterationJob.STATUS_WAITING_REVIEW
                        ? "waiting_review" : "completed";
            }
            if (status == 105) {
                return "completed";
            }
            if (status == 203 || status == 303) {
                return "failed";
            }
        }
        int totalImages = calculateTotalImages(progress, null);
        if (totalImages > 0 && calculateAnnotatedImages(progress) >= totalImages) {
            return job != null && job.getStatus() == SelfIterationJob.STATUS_WAITING_REVIEW
                    ? "waiting_review" : "completed";
        }
        return "pending";
    }

    private Integer extractInteger(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.valueOf((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String resolveBaselineModelName(SelfIterationTask task, SelfIterationJob job) {
        if (task == null) {
            return "-";
        }

        String autoLabelImageName = resolveAutoLabelImageName(task.getAutoLabelImageUrl());
        if (autoLabelImageName != null) {
            boolean alwaysUseDefaultModel = "NEVER".equals(task.getReuseAnnotationModel());
            boolean isFirstRound = job != null && job.getRound() == 1;
            if (alwaysUseDefaultModel || isFirstRound) {
                return autoLabelImageName;
            }
        }

        if (task.getModelApplication() != null && task.getModelApplication().getApplicationTaskName() != null) {
            return task.getModelApplication().getApplicationTaskName();
        }
        return "-";
    }

    private String resolveAutoLabelImageName(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        ModelVersion modelVersion = modelVersionRepo.findModelVersionByUrl(imageUrl);
        if (modelVersion == null) {
            return imageUrl;
        }

        if (modelVersion.getName() != null && !modelVersion.getName().trim().isEmpty()) {
            return modelVersion.getName();
        }
        if (modelVersion.getShowName() != null && !modelVersion.getShowName().trim().isEmpty()) {
            return modelVersion.getShowName();
        }
        return imageUrl;
    }

    private Map<String, Object> buildModelIterationDetail(SelfIterationTask task, SelfIterationJob job) {
        Map<String, Object> section = new LinkedHashMap<>();
        Map<String, Object> train = new LinkedHashMap<>();
        train.put("status", mapJobStatusToUi(job.getStatus()));
        train.put("startTime", "-");
        train.put("endTime", "-");
        train.put("epoch", 0);
        train.put("learningRate", 0);
        train.put("batchSize", 0);
        train.put("map50", job.getTrainAccuracy() == null ? 0 : job.getTrainAccuracy());
        train.put("loss", 0);
        train.put("params", toParamItems(task != null ? task.getHyperParams() : null));

        Map<String, Object> convert = new LinkedHashMap<>();
        convert.put("status", mapJobStatusToUi(job.getStatus()));
        convert.put("framework", "-");
        convert.put("inputSize", "-");
        convert.put("outputType", "-");
        convert.put("startTime", "-");
        convert.put("endTime", "-");
        convert.put("durationSeconds", 0);
        convert.put("params", toParamItems(task != null ? task.getConvertParams() : null));

        List<Map<String, Object>> packages = new ArrayList<>();

        section.put("train", train);
        section.put("convert", convert);
        section.put("packages", packages);
        section.put("trainJobId", job.getTrainJobId());
        section.put("convertJobId", job.getConvertJobId());
        section.put("packageTaskId", job.getPackageTaskId());
        section.put("appZipPath", task.getModelApplication() != null
                ? task.getModelApplication().getAppZipPath() : null);
        return section;
    }

    private Map<String, Object> buildModelDispatchDetail(SelfIterationTask task, SelfIterationJob job) {
        Map<String, Object> section = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();

        List<Map<String, Object>> targets = buildDispatchTargets(task, job);
        long successCount = targets.stream().filter(t -> "success".equals(t.get("status"))).count();
        long failedCount = targets.stream().filter(t -> "failed".equals(t.get("status"))).count();

        summary.put("successCount", successCount);
        summary.put("failedCount", failedCount);
        summary.put("dispatchTime", job.getStatus() == SelfIterationJob.STATUS_COMPLETED
                ? String.valueOf(job.getUpdateTime()) : "-");

        String modelName = resolveDispatchModelName(job);
        summary.put("modelName", modelName == null ? "-" : modelName);
        summary.put("modelVersion", "round-" + job.getRound());
        summary.put("sourceIteration", job.getRound());
        section.put("summary", summary);

        section.put("autoDeployEnabled", task != null && task.isAutoDeployEnabled());
        section.put("jobId", job.getId());
        section.put("status", resolveDispatchTargetStatus(job.getStatus()));
        section.put("targets", targets);
        section.put("metrics", new ArrayList<>());

        List<String> suggestions = new ArrayList<>();
        if (targets.isEmpty()) {
            suggestions.add("未绑定模型接收地址，请在父任务配置中绑定模型接收地址。 ");
        }
        if (job.getStatus() == SelfIterationJob.STATUS_PENDING_DEPLOY && task != null && !task.isAutoDeployEnabled()) {
            suggestions.add("手动下发模式：打包已完成，请点击立即下发按钮将模型推送到模型接收地址。");
        }
        section.put("suggestions", suggestions);
        return section;
    }

    private List<Map<String, Object>> buildDispatchTargets(SelfIterationTask task, SelfIterationJob job) {
        List<Map<String, Object>> targets = new ArrayList<>();
        if (task == null) {
            return targets;
        }

        String targetStatus = resolveDispatchTargetStatus(job.getStatus());

        for (GpuUrlTarget target : gpuUrlTargetService.findAvailableByIds(task.getGpuUrlTargetIds())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("targetName", target.getName() + " (" + target.getIp() + ":" + target.getPort() + ")");
            item.put("targetType", "gpuUrl");
            item.put("status", targetStatus);
            item.put("gpuUrlTargetId", target.getId());
            item.put("gpuUrl", target.getGpuUrl());
            item.put("ip", target.getIp());
            item.put("port", target.getPort());
            item.put("platformIp", target.getPlatformIp());
            item.put("platformPort", target.getPlatformPort());
            targets.add(item);
        }

        if (task.getInferenceDeviceIds() == null || task.getInferenceDeviceIds().isEmpty()) {
            return targets;
        }

        try {
            DataResponseBody<com.baomidou.mybatisplus.extension.plugins.pagination.Page<DubheDataFeign.InferenceDeviceVO>> resp =
                    dubheDataFeign.pageInferenceDevices(dubheUtils.getCurrentAuthorization(), 1L, 1000L, null);
            if (resp == null || !resp.succeed() || resp.getData() == null || resp.getData().getRecords() == null) {
                return targets;
            }
            Set<Long> idSet = new LinkedHashSet<>(task.getInferenceDeviceIds());
            for (DubheDataFeign.InferenceDeviceVO device : resp.getData().getRecords()) {
                if (device == null || device.getId() == null || !idSet.contains(device.getId())) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("targetName", device.getDeviceName() + " (" + device.getInferenceIp() + ":" + device.getInferencePort() + ")");
                item.put("targetType", "device");
                item.put("status", targetStatus);
                item.put("deviceId", device.getId());
                item.put("inferenceIp", device.getInferenceIp());
                item.put("inferencePort", device.getInferencePort());
                targets.add(item);
            }
        } catch (Exception e) {
            log.warn("构建模型接收地址列表失败：jobId={}", job.getId(), e);
        }
        return targets;
    }

    private String resolveDispatchTargetStatus(int jobStatus) {
        if (jobStatus == SelfIterationJob.STATUS_COMPLETED) {
            return "success";
        }
        if (jobStatus == SelfIterationJob.STATUS_PENDING_DEPLOY) {
            return "pending";
        }
        if (jobStatus == SelfIterationJob.STATUS_PACKAGE_FAILED || jobStatus == SelfIterationJob.STATUS_TRAIN_FAILED) {
            return "failed";
        }
        return "pending";
    }

    private String resolveDispatchModelName(SelfIterationJob job) {
        if (job.getConvertJobId() == null) {
            return null;
        }
        try {
            ModelJob convertJob = modelJobRepo.findModelJobById(job.getConvertJobId());
            if (convertJob == null) {
                return null;
            }

            String weightDir = convertJob.getWeightPath();
            if (weightDir == null || weightDir.trim().isEmpty()) {
                return null;
            }

            List<File> candidateDirs = new ArrayList<>();
            candidateDirs.add(new File(datasetRootPath + "/" + weightPath + "/" + weightDir));

            String trainJobName = extractTrainJobNameFromConvertName(convertJob.getName());
            if (trainJobName != null) {
                candidateDirs.add(new File(datasetRootPath + "/" + weightPath + "/" + trainJobName + "/" + weightDir));
                File trainRoot = new File(datasetRootPath + "/" + weightPath + "/" + trainJobName);
                File[] possibleConvertDirs = trainRoot.listFiles(
                        file -> file.isDirectory() && file.getName().startsWith(trainJobName + "-convert-")
                );
                if (possibleConvertDirs != null && possibleConvertDirs.length > 0) {
                    Arrays.sort(possibleConvertDirs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                    candidateDirs.addAll(Arrays.asList(possibleConvertDirs));
                }
            }

            for (File dir : candidateDirs) {
                if (dir == null || !dir.isDirectory()) {
                    continue;
                }
                File preferred = findPreferredConvertedModel(dir);
                if (preferred != null) {
                    return preferred.getName();
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("解析下发模型信息失败：jobId={}", job.getId(), e);
            return null;
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

    private File findPreferredConvertedModel(File dir) {
        File[] files = dir.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            return null;
        }

        for (File file : files) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".rknn") || name.endsWith(".engine") || name.endsWith(".bmodel") || name.endsWith(".onnx")) {
                return file;
            }
        }
        return files[0];
    }

    private List<Map<String, Object>> toParamItems(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", entry.getKey());
            item.put("label", entry.getKey());
            item.put("value", entry.getValue());
            item.put("type", "string");
            item.put("required", true);
            list.add(item);
        }
        return list;
    }

    /**
     * 根据已获取的标签 ID 列表，构建 index→name Map（对齐 ModelGeneration.selectedLabels）。
     * Feign 查出标签名后按 0-based 顺序构建 Map&lt;Integer, String&gt;。
     */
    private Map<Integer, String> buildSelectedLabels(List<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return new java.util.HashMap<>();
        }
        try {
            List<LabelDTO> labelDTOs =
                    dubheDataFeign.findLabelByIds(dubheUtils.getCurrentAuthorization(), labelIds).getData();
            if (labelDTOs == null || labelDTOs.isEmpty()) {
                return new java.util.HashMap<>();
            }
            Map<Integer, String> map = new java.util.LinkedHashMap<>();
            for (int i = 0; i < labelDTOs.size(); i++) {
                map.put(i, labelDTOs.get(i).getName());
            }
            return map;
        } catch (Exception e) {
            log.warn("构建 selectedLabels 失败，使用空 Map：原因：{}", e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    /**
     * 首轮自动标注镜像：用户指定优先；未指定时沿用后续轮次的绑定训练镜像解析链路。
     */
    private void applyInitialAutoLabelConfig(SelfIterationTask task,
                                             SelfIterationTaskDTO dto,
                                             ModelApplication modelApplication,
                                             Model model,
                                             boolean preserveWhenUnspecified) {
        String source = normalizeInitialAutoLabelSource(dto.getInitialAutoLabelSource(), dto.getAutoLabelImageUrl());
        boolean hasExplicitConfig = hasText(dto.getInitialAutoLabelSource())
                || hasText(dto.getAutoLabelImageUrl())
                || dto.getInitialAutoLabelModelGenerationId() != null
                || hasText(dto.getInitialAutoLabelStandardJobName());
        if (preserveWhenUnspecified && !hasExplicitConfig) {
            return;
        }

        if (INITIAL_AUTO_LABEL_SOURCE_TRAINED_MODEL.equals(source)) {
            applyTrainedInitialAutoLabelConfig(task, dto, modelApplication, model);
            return;
        }

        if (INITIAL_AUTO_LABEL_SOURCE_DEFAULT_IMAGE.equals(source)) {
            String imageUrl = dto.getAutoLabelImageUrl();
            if (!hasText(imageUrl)) {
                throw new IllegalArgumentException("选择默认镜像作为首轮自动标注模型时，autoLabelImageUrl 不能为空");
            }
            task.setAutoLabelImageUrl(resolveInitialAutoLabelImageUrl(imageUrl, modelApplication, model));
            task.setInitialAutoLabelSource(INITIAL_AUTO_LABEL_SOURCE_DEFAULT_IMAGE);
            task.setInitialAutoLabelTrainSource(null);
            task.setInitialAutoLabelModelGenerationId(null);
            task.setInitialAutoLabelStandardJobName(null);
            return;
        }

        task.setAutoLabelImageUrl(resolveInitialAutoLabelImageUrl(null, modelApplication, model));
        task.setInitialAutoLabelSource(INITIAL_AUTO_LABEL_SOURCE_BOUND_DEFAULT);
        task.setInitialAutoLabelTrainSource(null);
        task.setInitialAutoLabelModelGenerationId(null);
        task.setInitialAutoLabelStandardJobName(null);
    }

    private String normalizeInitialAutoLabelSource(String source, String autoLabelImageUrl) {
        if (hasText(source)) {
            return source.trim();
        }
        return hasText(autoLabelImageUrl)
                ? INITIAL_AUTO_LABEL_SOURCE_DEFAULT_IMAGE
                : INITIAL_AUTO_LABEL_SOURCE_BOUND_DEFAULT;
    }

    private void applyTrainedInitialAutoLabelConfig(SelfIterationTask task,
                                                    SelfIterationTaskDTO dto,
                                                    ModelApplication modelApplication,
                                                    Model model) {
        if (dto.getInitialAutoLabelModelGenerationId() == null) {
            throw new IllegalArgumentException("选择训练模型作为首轮自动标注模型时，ModelGeneration ID 不能为空");
        }
        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(dto.getInitialAutoLabelModelGenerationId());
        if (modelGeneration == null || modelGeneration.getModelExplore() == null
                || modelGeneration.getModelExplore().getTrainModelVersion() == null) {
            throw new IllegalArgumentException("未找到可用于首轮自动标注的训练模型");
        }

        ModelVersion trainModelVersion = modelGeneration.getModelExplore().getTrainModelVersion();
        if (!trainModelVersion.isAutoLabel()) {
            throw new IllegalArgumentException("所选训练模型镜像不支持自动标注");
        }
        if (!hasText(trainModelVersion.getUrl())) {
            throw new IllegalArgumentException("所选训练模型镜像 URL 为空");
        }

        String trainSource = hasText(dto.getInitialAutoLabelTrainSource())
                ? dto.getInitialAutoLabelTrainSource().trim()
                : (Boolean.TRUE.equals(modelGeneration.getIsGuided())
                ? INITIAL_AUTO_LABEL_TRAIN_SOURCE_GUIDED
                : INITIAL_AUTO_LABEL_TRAIN_SOURCE_STANDARD);
        String standardJobName = hasText(dto.getInitialAutoLabelStandardJobName())
                ? dto.getInitialAutoLabelStandardJobName().trim()
                : null;
        if (INITIAL_AUTO_LABEL_TRAIN_SOURCE_STANDARD.equals(trainSource)) {
            if (!hasText(standardJobName)) {
                throw new IllegalArgumentException("选择标准化训练模型时，训练任务不能为空");
            }
            ModelJob modelJob = modelJobRepo.findModelJobByName(standardJobName);
            if (modelJob == null) {
                throw new IllegalArgumentException("所选标准化训练任务不存在：" + standardJobName);
            }
        } else {
            trainSource = INITIAL_AUTO_LABEL_TRAIN_SOURCE_GUIDED;
            standardJobName = null;
        }

        task.setAutoLabelImageUrl(trainModelVersion.getUrl().trim());
        task.setInitialAutoLabelSource(INITIAL_AUTO_LABEL_SOURCE_TRAINED_MODEL);
        task.setInitialAutoLabelTrainSource(trainSource);
        task.setInitialAutoLabelModelGenerationId(modelGeneration.getId());
        task.setInitialAutoLabelStandardJobName(standardJobName);
    }

    private String resolveInitialAutoLabelImageUrl(String requestedImageUrl, ModelApplication modelApplication, Model model) {
        if (hasText(requestedImageUrl)) {
            return requestedImageUrl.trim();
        }

        String imageUrl = resolveBoundTrainAutoLabelImageUrl(modelApplication, model);
        if (!hasText(imageUrl)) {
            log.warn("ModelApplication[{}] 未按应用固件绑定链路解析到默认自动标注镜像，且用户未指定镜像，modelId={}",
                    modelApplication != null ? modelApplication.getId() : null,
                    model != null ? model.getId() : null);
        }
        return imageUrl;
    }

    private String resolveBoundTrainAutoLabelImageUrl(ModelApplication modelApplication, Model model) {
        if (model == null || model.getGenerationId() <= 0) {
            log.warn("ModelApplication[{}] 绑定模型缺少 generationId，无法解析默认自动标注镜像，modelId={}",
                modelApplication != null ? modelApplication.getId() : null,
                model != null ? model.getId() : null);
            return null;
        }

        try {
            ModelGeneration sourceGeneration = modelGenerationService.findModelGenerationById(model.getGenerationId());
            if (sourceGeneration == null || sourceGeneration.getModelExplore() == null) {
                log.warn("ModelApplication[{}] 绑定模型生产任务缺少 modelExplore，generationId={}",
                        modelApplication != null ? modelApplication.getId() : null, model.getGenerationId());
                return null;
            }

            return resolveUsableAutoLabelImageUrl(
                    sourceGeneration.getModelExplore().getTrainModelVersion(),
                    modelApplication,
                    "model.generation.modelExplore.trainModelVersion");
        } catch (Exception e) {
            log.warn("ModelApplication[{}] 解析生产任务训练镜像失败，generationId={}",
                    modelApplication != null ? modelApplication.getId() : null,
                    model.getGenerationId(),
                    e);
            return null;
        }
    }

    private String resolveUsableAutoLabelImageUrl(ModelVersion modelVersion, ModelApplication modelApplication, String source) {
        if (modelVersion == null) {
            return null;
        }
        if (!modelVersion.isAutoLabel()) {
            log.warn("ModelApplication[{}] {} 不支持自动标注，modelVersionId={}, url={}",
                    modelApplication != null ? modelApplication.getId() : null,
                    source,
                    modelVersion.getId(),
                    modelVersion.getUrl());
            return null;
        }
        if (!hasText(modelVersion.getUrl())) {
            log.warn("ModelApplication[{}] {} 自动标注镜像 url 为空，modelVersionId={}",
                    modelApplication != null ? modelApplication.getId() : null,
                    source,
                    modelVersion.getId());
            return null;
        }
        log.info("ModelApplication[{}] 使用 {} 作为默认自动标注镜像，modelVersionId={}, imageUrl={}",
                modelApplication != null ? modelApplication.getId() : null,
                source,
                modelVersion.getId(),
                modelVersion.getUrl());
        return modelVersion.getUrl().trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 根据 ModelApplication 的 trainModelVersion.annotationType 确定标注类型值。
     * <ul>
     *   <li>"Segmentation" → 103（语义分割）</li>
     *   <li>其他（包含 null / "Detection"）→ 102（目标检测，默认）</li>
     * </ul>
     */
    private int resolveAnnotateType(ModelApplication modelApplication) {
        try {
            Model model = modelApplication.getModel();
            if (model != null) {
                ModelVersion trainModelVersion = model.getTrainModelVersion();
                if (trainModelVersion != null && "Segmentation".equalsIgnoreCase(trainModelVersion.getAnnotationType())) {
                    return 103;
                }
            }
        } catch (Exception e) {
            log.warn("解析 annotateType 失败，使用默认值 102：{}", e.getMessage());
        }
        return 102;
    }

    private Double resolveConfidenceThreshold(SelfIterationTask task) {        if (task == null) {
            return 0.5;
        }
        try {
            Object value = task.getClass().getMethod("getConfidenceThreshold").invoke(task);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Compatible with old mineai-core entities that do not have confidenceThreshold.
        }
        return 0.5;
    }

    private void setConfidenceThreshold(SelfIterationTask task, Double threshold) {
        if (task == null || threshold == null) {
            return;
        }
        try {
            task.getClass().getMethod("setConfidenceThreshold", Double.class).invoke(task, threshold);
        } catch (ReflectiveOperationException ignored) {
            // Compatible with old mineai-core entities that do not have confidenceThreshold.
        }
    }

    private String mapCaptureStatusToUi(Integer status) {
        if (status == null) return "running";
        if (status == 2) return "success";
        if (status == 3 || status == 4) return "failed";
        return "running";
    }

    private String mapPhaseToUi(int phase) {
        switch (phase) {
            case SelfIterationJob.PHASE_DATA_COLLECT:
                return "dataCollection";
            case SelfIterationJob.PHASE_DATA_ANNOTATE:
                return "dataAnnotation";
            case SelfIterationJob.PHASE_MODEL_TRAIN:
                return "modelIteration";
            case SelfIterationJob.PHASE_MODEL_DEPLOY:
                return "modelDispatch";
            default:
                return "dataCollection";
        }
    }

    private String mapJobStatusToUi(int status) {
        switch (status) {
            case SelfIterationJob.STATUS_CREATED:
                return "pending";
            case SelfIterationJob.STATUS_COLLECTING:
                return "collecting";
            case SelfIterationJob.STATUS_COLLECT_FAILED:
                return "failed";
            case SelfIterationJob.STATUS_AUTO_LABELING:
                return "annotating";
            case SelfIterationJob.STATUS_WAITING_REVIEW:
                return "waiting_review";
            case SelfIterationJob.STATUS_ANNOTATE_FAILED:
                return "failed";
            case SelfIterationJob.STATUS_TRAINING:
                return "training";
            case SelfIterationJob.STATUS_CONVERTING:
                return "converting";
            case SelfIterationJob.STATUS_TRAIN_FAILED:
                return "failed";
            case SelfIterationJob.STATUS_PACKAGING:
                return "packaging";
            case SelfIterationJob.STATUS_PACKAGE_FAILED:
                return "failed";
            case SelfIterationJob.STATUS_PENDING_DEPLOY:
                return "dispatching";
            case SelfIterationJob.STATUS_COMPLETED:
                return "completed";
            default:
                return "failed";
        }
    }

    private List<CaptureRuntimeSnapshot> loadCaptureRuntimeSnapshots(List<Long> executionIds) {
        if (executionIds == null || executionIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<CaptureRuntimeSnapshot> snapshots = new ArrayList<>();
        for (Long executionId : executionIds) {
            CaptureRuntimeSnapshot snapshot = new CaptureRuntimeSnapshot();
            snapshot.executionId = executionId;
            try {
                DataResponseBody<RtspCaptureExecutionVO> executionResp = dubheDataFeign.getCaptureExecution(
                        dubheUtils.getAuthorization(), executionId);
                if (executionResp != null && executionResp.succeed() && executionResp.getData() != null) {
                    RtspCaptureExecutionVO execution = executionResp.getData();
                    snapshot.captureTaskId = execution.getTaskId();
                    snapshot.datasetId = execution.getDatasetId();
                    snapshot.executionStatus = execution.getStatus();
                    snapshot.capturedCount = execution.getCapturedCount();
                    snapshot.message = execution.getMessage();

                    if (snapshot.captureTaskId != null) {
                        DataResponseBody<DubheDataFeign.RtspCaptureTaskVO> taskResp = dubheDataFeign.getRtspCaptureTaskDetail(
                                dubheUtils.getAuthorization(), snapshot.captureTaskId);
                        if (taskResp != null && taskResp.succeed() && taskResp.getData() != null) {
                            DubheDataFeign.RtspCaptureTaskVO captureTask = taskResp.getData();
                            snapshot.captureTaskName = captureTask.getTaskName();
                            snapshot.httpCameraId = captureTask.getHttpCameraId();
                            snapshot.imageQuantity = captureTask.getImageQuantity();
                            snapshot.captureInterval = captureTask.getCaptureInterval();
                        }
                    }

                    if (snapshot.datasetId != null) {
                        snapshot.datasetName = loadDatasetName(snapshot.datasetId);
                        snapshot.datasetImageCount = loadDatasetImageCount(snapshot.datasetId);
                        snapshot.datasetGroupName = loadDatasetGroupName(snapshot.datasetId);
                    }
                }
            } catch (Exception e) {
                log.warn("加载采集执行详情失败：executionId={}", executionId, e);
            }
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private String loadDatasetName(Long datasetId) {
        try {
            DataResponseBody response = dubheDataFeign.get(dubheUtils.getAuthorization(), datasetId);
            if (response != null && response.succeed() && response.getData() instanceof Map) {
                Object name = ((Map<?, ?>) response.getData()).get("name");
                if (name != null) {
                    return String.valueOf(name);
                }
            }
        } catch (Exception e) {
            log.warn("查询数据集名称失败：datasetId={}", datasetId, e);
        }
        return null;
    }

    private Integer loadDatasetImageCount(Long datasetId) {
        try {
            DataResponseBody<Integer> resp = dubheDataFeign.getDatasetImageCount(dubheUtils.getAuthorization(), datasetId);
            if (resp != null && resp.succeed()) {
                return resp.getData();
            }
        } catch (Exception e) {
            log.warn("查询数据集图片数量失败：datasetId={}", datasetId, e);
        }
        return 0;
    }

    private String loadDatasetGroupName(Long datasetId) {
        try {
            DataResponseBody<String> resp = dubheDataFeign.getDatasetGroupNameByDatasetId(dubheUtils.getAuthorization(), datasetId);
            if (resp != null && resp.succeed()) {
                return resp.getData();
            }
        } catch (Exception e) {
            log.warn("查询数据集组名称失败：datasetId={}", datasetId, e);
        }
        return null;
    }

    private static class CaptureRuntimeSnapshot {
        private Long executionId;
        private Long captureTaskId;
        private Long datasetId;
        private Integer executionStatus;
        private Integer capturedCount;
        private String message;
        private Long httpCameraId;
        private Integer imageQuantity;
        private Integer captureInterval;
        private String captureTaskName;
        private String datasetName;
        private Integer datasetImageCount;
        private String datasetGroupName;
    }

    /**
     * 获取父任务的跨轮次指标对比
     * TODO: 实现完整的指标对比逻辑，当前返回基础数据
     */
    public Map<String, Object> getTaskMetrics(Long taskId) {
        requireVisibleTask(taskId);
        List<SelfIterationJob> jobs = jobRepo.findBySelfIterationTask_IdOrderByRoundAsc(taskId);

        Map<String, Object> metrics = new java.util.HashMap<>();
        List<Map<String, Object>> roundMetrics = new ArrayList<>();

        for (SelfIterationJob job : jobs) {
            Map<String, Object> roundData = new java.util.HashMap<>();
            roundData.put("round", job.getRound());
            roundData.put("trainAccuracy", job.getTrainAccuracy());
            roundData.put("trainRecall", job.getTrainRecall());
            roundData.put("status", job.getStatus());
            roundData.put("statusLabel", getJobStatusLabel(job.getStatus()));
            roundMetrics.add(roundData);
        }

        metrics.put("rounds", roundMetrics);
        // TODO: 添加趋势分析、对比建议等
        return metrics;
    }

    private List<SelfIterationTask> listVisibleTasks() {
        if (isManagerRole()) {
            return taskRepo.findAll();
        }
        return taskRepo.findByUserId(currentUserId());
    }

    private List<SelfIterationTask> pageSlice(List<SelfIterationTask> tasks, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= tasks.size()) {
            return Collections.emptyList();
        }
        int end = Math.min(start + pageable.getPageSize(), tasks.size());
        return tasks.subList(start, end);
    }

    private SelfIterationTask requireVisibleTask(Long taskId) {
        SelfIterationTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在：id=" + taskId));
        assertVisibleTask(task);
        return task;
    }

    private void assertVisibleTask(SelfIterationTask task) {
        if (task == null) {
            throw new IllegalArgumentException("任务不存在或无权限访问");
        }
        if (isManagerRole()) {
            return;
        }
        if (!Objects.equals(task.getUserId(), currentUserId())) {
            throw new IllegalArgumentException("任务不存在或无权限访问");
        }
    }

    private Long currentUserId() {
        return Long.valueOf(UserContextHolder.getUserContext().getId());
    }

    private boolean isManagerRole() {
        UserContext userContext = UserContextHolder.getUserContext();
        if (userContext == null || userContext.getRoles() == null) {
            return false;
        }
        return userContext.getRoles().stream()
                .filter(Objects::nonNull)
                .anyMatch(role -> "管理员".equals(role.getName()) || "管理人员".equals(role.getName()));
    }

    // ===================== 实体 → VO 转换 =====================

    private SelfIterationTaskVO toTaskVO(SelfIterationTask task) {
        return toTaskVO(task, null, true);
    }

    private SelfIterationTaskVO toTaskVO(
            SelfIterationTask task,
            List<SelfIterationJob> prefetchedJobs,
            boolean includeDataSourceDetails
    ) {
        SelfIterationTaskVO vo = new SelfIterationTaskVO();
        vo.setId(task.getId());
        vo.setName(task.getName());
        vo.setDescription(task.getDescription());

        if (task.getModelApplication() != null) {
            vo.setModelApplicationId(task.getModelApplication().getId());
            vo.setModelApplicationName(task.getModelApplication().getApplicationTaskName());
            if (task.getModelApplication().getApplicationName() != null) {
                vo.setApplicationName(task.getModelApplication().getApplicationName().getApplicationName());
            }
            if (task.getModelApplication().getDevice() != null) {
                vo.setDeviceFirmware(
                        task.getModelApplication().getDevice().getDeviceName()
                                + "-"
                                + task.getModelApplication().getDevice().getFirmwareVersion());
            }
        }

        vo.setAutoLabelImageUrl(task.getAutoLabelImageUrl());
        vo.setInitialAutoLabelSource(task.getInitialAutoLabelSource());
        vo.setInitialAutoLabelTrainSource(task.getInitialAutoLabelTrainSource());
        vo.setInitialAutoLabelModelGenerationId(task.getInitialAutoLabelModelGenerationId());
        vo.setInitialAutoLabelStandardJobName(task.getInitialAutoLabelStandardJobName());
        vo.setDataSourceIds(task.getDataSourceIds());
        vo.setDeleteRawAfterCollect(task.isDeleteRawAfterCollect());
        vo.setAllDatasetVersionIds(task.getAllDatasetVersionIds());
        vo.setRequireManualReview(task.isRequireManualReview());
        vo.setAutoDeployEnabled(task.isAutoDeployEnabled());
        vo.setMaxRounds(task.getMaxRounds());
        vo.setIterationStartImageQuantity(task.getIterationStartImageQuantity());
        vo.setTargetAccuracy(task.getTargetAccuracy());
        vo.setTargetRecall(task.getTargetRecall());
        vo.setConfidenceThreshold(resolveConfidenceThreshold(task));
        vo.setCurrentRound(task.getCurrentRound());
        vo.setReuseAnnotationModel(task.getReuseAnnotationModel());
        vo.setReuseTrainModel(task.getReuseTrainModel());

        if (task.getHardwareParams() != null) {
            vo.setHardwareParamsId(task.getHardwareParams().getId());
            vo.setHardwareParamsTitle(task.getHardwareParams().getTitle());
        }

        vo.setGpuMode(task.getGpuMode());
        vo.setGpuCount(task.getGpuCount());
        vo.setSplitSize(task.getSplitSize());
        vo.setModelType(task.getModelType());
        vo.setHyperParams(task.getHyperParams());
        vo.setConvertParams(task.getConvertParams());
        vo.setAuthCode(task.getAuthCode());
        vo.setInferenceDeviceIds(task.getInferenceDeviceIds());
        vo.setGpuUrlTargetIds(task.getGpuUrlTargetIds());
        vo.setStatus(task.getStatus());
        vo.setStatusLabel(getTaskStatusLabel(task.getStatus()));
        vo.setUserId(task.getUserId());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        vo.setSelectedLabels(task.getSelectedLabels());
        if (includeDataSourceDetails) {
            List<SelfIterationTaskVO.DataSourceConfig> dataSourceConfigs = loadDataSourceConfigs(task.getDataSourceIds());
            vo.setDataSources(dataSourceConfigs);
            if (vo.getIterationStartImageQuantity() == null) {
                vo.setIterationStartImageQuantity(sumImageQuantity(dataSourceConfigs));
            }
            if (!dataSourceConfigs.isEmpty()) {
                SelfIterationTaskVO.DataSourceConfig first = dataSourceConfigs.get(0);
                if (first != null) {
                    // datasetGroupId 来自同一组回流任务，默认取第一个
                    Long datasetGroupId = null;
                    for (Long sourceId : task.getDataSourceIds()) {
                        try {
                            DataResponseBody<DubheDataFeign.RtspCaptureTaskVO> detail = dubheDataFeign.getRtspCaptureTaskDetail(
                                    dubheUtils.getCurrentAuthorization(), sourceId);
                            if (detail != null && detail.succeed() && detail.getData() != null && detail.getData().getDatasetGroupId() != null) {
                                datasetGroupId = detail.getData().getDatasetGroupId();
                                break;
                            }
                        } catch (Exception e) {
                            log.warn("查询回流任务详情失败：taskId={}, sourceId={}", task.getId(), sourceId);
                        }
                    }
                    vo.setDatasetGroupId(datasetGroupId);
                }
            }
        } else {
            vo.setDataSources(Collections.emptyList());
            vo.setDatasetGroupId(null);
        }

        List<SelfIterationJob> jobs = prefetchedJobs != null
                ? prefetchedJobs
                : jobRepo.findBySelfIterationTask_IdOrderByRoundAsc(task.getId());
        vo.setJobList(jobs.stream().map(this::toJobVO).collect(Collectors.toList()));

        return vo;
    }

    private List<SelfIterationTaskVO.DataSourceConfig> loadDataSourceConfigs(List<Long> dataSourceIds) {
        if (dataSourceIds == null || dataSourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SelfIterationTaskVO.DataSourceConfig> configs = new ArrayList<>();
        for (Long dataSourceId : dataSourceIds) {
            try {
                DataResponseBody<DubheDataFeign.RtspCaptureTaskVO> detail = dubheDataFeign.getRtspCaptureTaskDetail(
                        dubheUtils.getCurrentAuthorization(), dataSourceId);
                if (detail == null || !detail.succeed() || detail.getData() == null) {
                    continue;
                }
                DubheDataFeign.RtspCaptureTaskVO taskVO = detail.getData();
                SelfIterationTaskVO.DataSourceConfig config = new SelfIterationTaskVO.DataSourceConfig();
                config.setHttpCameraId(taskVO.getHttpCameraId());
                config.setCaptureInterval(taskVO.getCaptureInterval());
                config.setImageQuantity(taskVO.getImageQuantity());
                configs.add(config);
            } catch (Exception e) {
                log.warn("加载回流任务配置失败：dataSourceId={}", dataSourceId);
            }
        }
        return configs;
    }

    private Integer resolveIterationStartImageQuantity(SelfIterationTaskDTO dto, SelfIterationTask existingTask) {
        Integer configured = dto.getIterationStartImageQuantity();
        if (configured != null) {
            validateIterationStartImageQuantity(configured);
            return configured;
        }

        if (existingTask != null && existingTask.getIterationStartImageQuantity() != null) {
            validateIterationStartImageQuantity(existingTask.getIterationStartImageQuantity());
            return existingTask.getIterationStartImageQuantity();
        }

        Integer fallback = sumDtoImageQuantity(dto.getDataSources());
        if ((fallback == null || fallback <= 0) && existingTask != null) {
            fallback = sumImageQuantity(loadDataSourceConfigs(existingTask.getDataSourceIds()));
        }
        validateIterationStartImageQuantity(fallback);
        return fallback;
    }

    private void validateIterationStartImageQuantity(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("迭代开始图片数量必须填写且大于 0");
        }
    }

    private Integer sumDtoImageQuantity(List<SelfIterationTaskDTO.DataSourceConfig> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            return null;
        }
        int total = 0;
        for (SelfIterationTaskDTO.DataSourceConfig dataSource : dataSources) {
            if (dataSource != null && dataSource.getImageQuantity() != null) {
                total += Math.max(dataSource.getImageQuantity(), 0);
            }
        }
        return total > 0 ? total : null;
    }

    private Integer sumImageQuantity(List<SelfIterationTaskVO.DataSourceConfig> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            return null;
        }
        int total = 0;
        for (SelfIterationTaskVO.DataSourceConfig dataSource : dataSources) {
            if (dataSource != null && dataSource.getImageQuantity() != null) {
                total += Math.max(dataSource.getImageQuantity(), 0);
            }
        }
        return total > 0 ? total : null;
    }

    private void validateIterationStopConfig(SelfIterationTaskDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("自迭代任务参数不能为空");
        }
        if (dto.getMaxRounds() == null || dto.getMaxRounds() <= 0) {
            throw new IllegalArgumentException("最大轮次必须填写且大于 0");
        }
        validateMetricTarget(dto.getTargetAccuracy(), "最低准确率");
        validateMetricTarget(dto.getTargetRecall(), "最低召回率");
    }

    private void validateMetricTarget(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "必须填写");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(fieldName + "必须在 0 到 1 之间");
        }
    }

    private void validateDataSources(List<SelfIterationTaskDTO.DataSourceConfig> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalArgumentException("至少需要配置一个数据源");
        }

        Set<Long> selectedCameraIds = new HashSet<>();
        final String sql = "SELECT id, status, is_delete FROM http_camera WHERE id = ? LIMIT 1";

        for (int i = 0; i < dataSources.size(); i++) {
            SelfIterationTaskDTO.DataSourceConfig dataSource = dataSources.get(i);
            if (dataSource == null || dataSource.getHttpCameraId() == null || dataSource.getHttpCameraId() <= 0) {
                throw new IllegalArgumentException("第" + (i + 1) + "路摄像机未配置");
            }

            Long cameraId = dataSource.getHttpCameraId();
            if (!selectedCameraIds.add(cameraId)) {
                throw new IllegalArgumentException("同一摄像机不能重复选择：cameraId=" + cameraId);
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, cameraId);
            if (rows == null || rows.isEmpty()) {
                throw new IllegalArgumentException("HTTP摄像机不存在：cameraId=" + cameraId);
            }

            Map<String, Object> row = rows.get(0);
            int isDelete = 0;
            Object isDeleteObj = row.get("is_delete");
            if (isDeleteObj instanceof Number) {
                isDelete = ((Number) isDeleteObj).intValue();
            }
            if (isDelete != 0) {
                throw new IllegalArgumentException("HTTP摄像机已删除：cameraId=" + cameraId);
            }

            String status = row.get("status") == null ? "" : String.valueOf(row.get("status")).toUpperCase(Locale.ROOT);
            if (!"ONLINE".equals(status)) {
                throw new IllegalArgumentException("HTTP摄像机当前不可用（仅支持 ONLINE）：cameraId=" + cameraId + ", status=" + status);
            }
            if (dataSource.getCaptureInterval() == null || dataSource.getCaptureInterval() <= 0) {
                throw new IllegalArgumentException("第" + (i + 1) + "路摄像机采样间隔必须大于 0");
            }
            if (dataSource.getImageQuantity() == null || dataSource.getImageQuantity() <= 0) {
                throw new IllegalArgumentException("第" + (i + 1) + "路摄像机采集数量必须大于 0");
            }
        }
    }

    private String resolveAuthCode(SelfIterationTaskDTO dto) {
        return resolveAuthCodeFromGpuUrlTargets(dto != null ? dto.getGpuUrlTargetIds() : null);
    }

    private String resolveAuthCodeFromGpuUrlTargets(List<Long> gpuUrlTargetIds) {
        if (gpuUrlTargetIds == null || gpuUrlTargetIds.isEmpty()) {
            return null;
        }

        Long firstTargetId = gpuUrlTargetIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .findFirst()
                .orElse(null);
        if (firstTargetId == null) {
            return null;
        }

        for (Map<String, Object> target : listGpuUrlTargetOptions()) {
            if (target == null) {
                continue;
            }
            Object idObj = target.get("id");
            Long id = null;
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            } else if (idObj != null) {
                try {
                    id = Long.parseLong(String.valueOf(idObj));
                } catch (NumberFormatException ignored) {
                }
            }
            if (!Objects.equals(firstTargetId, id)) {
                continue;
            }
            Object authCodeObj = target.get("authCode");
            if (authCodeObj == null) {
                return null;
            }
            String authCode = String.valueOf(authCodeObj).trim();
            return authCode.isEmpty() ? null : authCode;
        }

        return null;
    }

    private List<Long> normalizeInferenceDeviceIds(List<Long> inferenceDeviceIds) {
        if (inferenceDeviceIds == null || inferenceDeviceIds.isEmpty()) {
            return new ArrayList<>();
        }
        return inferenceDeviceIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> listModelApplicationOptions() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<ModelApplication> apps = modelApplicationRepo.findAll();
        for (ModelApplication app : apps) {
            if (app == null || !Objects.equals(app.getIsDelete(), 0) || !"已发布".equals(app.getIsReleased())) {
                continue;
            }
            if (app.getApplicationName() == null || app.getDevice() == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            String applicationName = app.getApplicationName().getApplicationName();
            String deviceFirmware = app.getDevice().getDeviceName() + "-" + app.getDevice().getFirmwareVersion();
            item.put("id", app.getId());
            item.put("applicationName", applicationName);
            item.put("deviceFirmware", deviceFirmware);
            item.put("label", applicationName + " / " + deviceFirmware);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> listHardwareParamsOptions(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        List<HardwareParams> hardwareParams = hardwareParamsService.findAllHardwareParams(userId);
        for (HardwareParams hp : hardwareParams) {
            if (hp == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", hp.getId());
            item.put("title", hp.getTitle());
            item.put("cpuNum", hp.getCpus());
            item.put("memoryMax", hp.getMemory());
            item.put("gpuMax", hp.getGpus());
            item.put("gpuMemory", hp.getGpuMemory());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> listGpuUrlTargetOptions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (GpuUrlTarget target : gpuUrlTargetRepo.searchAvailable(null)) {
            if (target == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", target.getId());
            item.put("name", target.getName());
            item.put("ip", target.getIp());
            item.put("port", target.getPort());
            item.put("gpuUrl", target.getGpuUrl());
            item.put("platformIp", target.getPlatformIp());
            item.put("platformPort", target.getPlatformPort());
            item.put("authCode", target.getAuthCode());
            item.put("label", target.getName() + " (GPU " + target.getIp() + ":" + target.getPort()
                    + " -> 平台 " + target.getPlatformIp() + ":" + target.getPlatformPort() + ")");
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> listDataSourceOptions() {
        final String sql = "SELECT id, name, camera_id, http_camera_server_id, status FROM http_camera WHERE is_delete = 0 ORDER BY id DESC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String status = row.get("status") == null ? "" : String.valueOf(row.get("status")).toUpperCase(Locale.ROOT);
            if (!"ONLINE".equals(status)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("name", row.get("name"));
            item.put("cameraId", row.get("camera_id"));
            item.put("serverId", row.get("http_camera_server_id"));
            item.put("status", status);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> listInferenceDeviceOptions() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            DataResponseBody<com.baomidou.mybatisplus.extension.plugins.pagination.Page<DubheDataFeign.InferenceDeviceVO>> resp =
                    dubheDataFeign.pageInferenceDevices(dubheUtils.getCurrentAuthorization(), 1L, 1000L, null);
            if (resp == null || !resp.succeed() || resp.getData() == null || resp.getData().getRecords() == null) {
                return result;
            }
            for (DubheDataFeign.InferenceDeviceVO device : resp.getData().getRecords()) {
                if (device == null || device.getId() == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", device.getId());
                item.put("deviceName", device.getDeviceName());
                item.put("inferenceIp", device.getInferenceIp());
                item.put("inferencePort", device.getInferencePort());
                item.put("label", device.getDeviceName() + " (" + device.getInferenceIp() + ":" + device.getInferencePort() + ")");
                result.add(item);
            }
        } catch (Exception e) {
            log.warn("查询模型接收地址选项失败", e);
        }
        return result;
    }

    private Long resolveCaptureTaskIdByTaskName(String taskName) {
        String authorization = dubheUtils.getCurrentAuthorization();
        for (int i = 0; i < 20; i++) {
            try {
                DataResponseBody<com.baomidou.mybatisplus.extension.plugins.pagination.Page<DubheDataFeign.RtspCaptureTaskVO>> pageResp =
                        dubheDataFeign.pageRtspCaptureTasks(authorization, 1L, 1L, taskName);
                if (pageResp != null && pageResp.succeed() && pageResp.getData() != null
                        && pageResp.getData().getRecords() != null && !pageResp.getData().getRecords().isEmpty()) {
                    DubheDataFeign.RtspCaptureTaskVO latest = pageResp.getData().getRecords().get(0);
                    if (latest != null && latest.getId() != null) {
                        return latest.getId();
                    }
                }
            } catch (Exception e) {
                log.warn("根据 taskName 回查 rtsp_capture_task 失败（API）：taskName={}, attempt={}", taskName, i + 1, e);
            }

            try {
                List<Long> ids = jdbcTemplate.query(
                        "SELECT id FROM rtsp_capture_task WHERE task_name = ? AND is_deleted = 0 ORDER BY id DESC LIMIT 1",
                        (rs, rowNum) -> rs.getLong("id"),
                        taskName
                );
                if (ids != null && !ids.isEmpty()) {
                    return ids.get(0);
                }
            } catch (Exception e) {
                log.warn("根据 taskName 回查 rtsp_capture_task 失败（SQL）：taskName={}, attempt={}", taskName, i + 1, e);
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private SelfIterationJobVO toJobVO(SelfIterationJob job) {
        SelfIterationJobVO vo = new SelfIterationJobVO();
        vo.setId(job.getId());
        vo.setRound(job.getRound());
        vo.setPhase(job.getPhase());
        vo.setPhaseLabel(getJobPhaseLabel(job.getPhase()));
        vo.setStatus(job.getStatus());
        vo.setStatusLabel(getJobStatusLabel(job.getStatus()));
        vo.setCaptureExecutionIds(job.getCaptureExecutionIds());
        vo.setDatasetVersionIds(job.getDatasetVersionIds());
        vo.setTrainJobId(job.getTrainJobId());
        vo.setConvertJobId(job.getConvertJobId());
        vo.setPackageTaskId(job.getPackageTaskId());
        vo.setTrainAccuracy(job.getTrainAccuracy());
        vo.setTrainRecall(job.getTrainRecall());
        vo.setEpochDetail(job.getEpochDetail());
        vo.setCreateTime(job.getCreateTime());
        vo.setUpdateTime(job.getUpdateTime());
        return vo;
    }

    private String getTaskStatusLabel(int status) {
        switch (status) {
            case SelfIterationTask.STATUS_IDLE: return "待启动";
            case SelfIterationTask.STATUS_RUNNING: return "运行中";
            case SelfIterationTask.STATUS_PAUSED: return "等待审核";
            case SelfIterationTask.STATUS_COMPLETED: return "已完成";
            case SelfIterationTask.STATUS_FAILED: return "已失败";
            case SelfIterationTask.STATUS_CANCELLED: return "已取消";
            default: return "未知状态";
        }
    }

    private String getJobStatusLabel(int status) {
        switch (status) {
            case SelfIterationJob.STATUS_CREATED: return "已创建";
            case SelfIterationJob.STATUS_COLLECTING: return "数据采集中";
            case SelfIterationJob.STATUS_COLLECT_FAILED: return "采集失败";
            case SelfIterationJob.STATUS_AUTO_LABELING: return "自动标注中";
            case SelfIterationJob.STATUS_WAITING_REVIEW: return "等待人工审核";
            case SelfIterationJob.STATUS_ANNOTATE_FAILED: return "标注失败";
            case SelfIterationJob.STATUS_TRAINING: return "训练中";
            case SelfIterationJob.STATUS_CONVERTING: return "转换中";
            case SelfIterationJob.STATUS_TRAIN_FAILED: return "训练失败";
            case SelfIterationJob.STATUS_PACKAGING: return "打包中";
            case SelfIterationJob.STATUS_PACKAGE_FAILED: return "打包失败";
            case SelfIterationJob.STATUS_PENDING_DEPLOY: return "待下发";
            case SelfIterationJob.STATUS_COMPLETED: return "已完成";
            case SelfIterationJob.STATUS_CANCELLED: return "已取消";
            default: return "未知状态";
        }
    }

    private String getJobPhaseLabel(int phase) {
        switch (phase) {
            case SelfIterationJob.PHASE_DATA_COLLECT: return "数据采集";
            case SelfIterationJob.PHASE_DATA_ANNOTATE: return "数据标注";
            case SelfIterationJob.PHASE_MODEL_TRAIN: return "模型训练";
            case SelfIterationJob.PHASE_MODEL_DEPLOY: return "模型下发";
            default: return "未知阶段";
        }
    }
}
