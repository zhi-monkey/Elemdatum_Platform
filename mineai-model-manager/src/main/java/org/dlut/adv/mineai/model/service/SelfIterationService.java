package org.dlut.adv.mineai.model.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.core.vo.DatasetVO;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.dto.DatasetDeleteDTO;
import org.dlut.adv.mineai.model.domain.dto.GuidedAndLabelsVO;
import org.dlut.adv.mineai.model.domain.dto.LabelDTO;
import org.dlut.adv.mineai.model.domain.dto.ModelConvertDTO;
import org.dlut.adv.mineai.model.dto.AutoLabelJobRequest;
import org.dlut.adv.mineai.model.dto.DatasetVersionCreateDTO;
import org.dlut.adv.mineai.model.dto.LabelMappingDTO;
import org.dlut.adv.mineai.model.openfeign.PackageFeign;
import org.dlut.adv.mineai.model.repository.ModelApplicationRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.repository.SelfIterationJobRepo;
import org.dlut.adv.mineai.model.repository.SelfIterationTaskRepo;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.dlut.adv.mineai.model.statusMachine.statemachine.SelfIterationJobStateMachine;
import org.dlut.adv.mineai.model.statusMachine.statemachine.SelfIterationTaskStateMachine;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.MinioUtils;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.dlut.adv.mineai.model.vo.DatasetVersionDetailVO;
import org.dlut.adv.mineai.model.vo.RtspCaptureExecutionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 自迭代训练 Service
 *
 * <p>驱动自迭代训练的完整流程，协调状态机、回流任务、自动标注、训练等各子系统。</p>
 *
 * <pre>
 * 数据流（每轮）：
 * 1. [DATA_COLLECT]  为每个 dataSourceId 触发 startCapture → 得到 RtspCaptureExecution
 * 2. [DATA_ANNOTATE] 采集完成后对每个 execution.datasetId 触发自动标注
 *                    - 第1轮：useDefault=true（镜像自带默认模型）
 *                    - 第N轮：useDefault=false + 上一轮 trainJobId.weightPath
 *                    标注完成后发版，版本 ID 追加到 job.datasetVersionIds 及 task.allDatasetVersionIds
 * 3. [MODEL_TRAIN]   trainDatasetVersions = task.allDatasetVersionIds（全量历史版本，数据持续累积）
 * 4. [CONVERTING]    转换训练产物
 * 5. [PACKAGING]     打包（预留，暂不实现）
 * </pre>
 */
@Service
@Slf4j
public class SelfIterationService {

    private static final int CAPTURE_STATUS_SUCCESS = 2;
    private static final int CAPTURE_STATUS_FAILED = 3;
    private static final int CAPTURE_STATUS_CANCELLED = 4;

    /**
     * 审核确认防重：同一父任务在一次异步审核处理完成前不允许重复触发。
     */
    private final Set<Long> reviewProcessingTaskIds = ConcurrentHashMap.newKeySet();

    public boolean isReviewProcessing(Long taskId) {
        return taskId != null && reviewProcessingTaskIds.contains(taskId);
    }

    @Resource
    private SelfIterationTaskRepo taskRepo;

    @Resource
    private SelfIterationJobRepo jobRepo;

    @Resource
    private ModelJobRepo modelJobRepo;

    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private DubheUtils dubheUtils;

    @Autowired
    private ModelApplicationZipLabelService zipLabelService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Resource
    private MinioUtils minioUtils;

    @Resource
    private PackageFeign packageFeign;

    @Resource
    private GpuUrlTargetService gpuUrlTargetService;

    @Resource
    private SelfIterationGpuDispatchService gpuDispatchService;

    @Resource
    private org.dlut.adv.mineai.model.repository.ModelJobLogDataRepo modelJobLogDataRepo;

    @Value("${datasetRootPath}")
    private String datasetRootPath;

    @Value("${container.weightPath}")
    private String weightPath;

    // ===================== 外部触发（Controller 调用） =====================

    /**
     * 启动自迭代任务：IDLE → RUNNING，创建第 1 轮子任务并开始数据采集。
     */
    @Transactional
    public void startTask(Long taskId) {
        startTask(taskId, null);
    }

    @Transactional
    public void startTask(Long taskId, Boolean delRawOverride) {
        SelfIterationTask task = requireTask(taskId);
        if (task.getStatus() != SelfIterationTask.STATUS_IDLE) {
            throw new IllegalStateException("任务当前状态不允许启动：status=" + task.getStatus());
        }
        SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
        taskSm.taskStartEvent(taskId);
        task.setStatus(SelfIterationTask.STATUS_RUNNING);
        taskRepo.save(task);

        SelfIterationJob job = createNewJob(task, 1);
        startCollectPhase(task, job, delRawOverride);
    }

    /**
     * 取消自迭代任务：任意活跃状态 → CANCELLED。
     */
    @Transactional
    public void cancelTask(Long taskId) {
        SelfIterationTask task = requireTask(taskId);
        // 取消当前进行中的子任务
        List<SelfIterationJob> activeJobs = jobRepo.findBySelfIterationTask_IdOrderByRoundAsc(taskId)
                .stream()
                .filter(j -> j.getStatus() != SelfIterationJob.STATUS_COMPLETED
                        && j.getStatus() != SelfIterationJob.STATUS_CANCELLED
                        && j.getStatus() != SelfIterationJob.STATUS_COLLECT_FAILED
                        && j.getStatus() != SelfIterationJob.STATUS_ANNOTATE_FAILED
                        && j.getStatus() != SelfIterationJob.STATUS_TRAIN_FAILED
                        && j.getStatus() != SelfIterationJob.STATUS_PACKAGE_FAILED)
                .collect(Collectors.toList());

        for (SelfIterationJob job : activeJobs) {
            SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
            jobSm.jobCancelEvent(job.getId());
            job.setStatus(SelfIterationJob.STATUS_CANCELLED);
            jobRepo.save(job);
        }

        SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
        taskSm.taskCancelEvent(taskId);
        task.setStatus(SelfIterationTask.STATUS_CANCELLED);
        taskRepo.save(task);
    }

    /**
     * 确认人工审核完成：父任务 PAUSED → RUNNING，子任务 WAITING_REVIEW → TRAINING。
     */
    @Transactional
    public void confirmReview(Long taskId) {
        SelfIterationTask task = requireTask(taskId);
        if (task.getStatus() != SelfIterationTask.STATUS_PAUSED) {
            throw new IllegalStateException("任务当前不在审核等待状态：status=" + task.getStatus());
        }

        if (!reviewProcessingTaskIds.add(taskId)) {
            throw new IllegalStateException("审核确认处理中，请勿重复提交");
        }

        try {
            // 找到当前 WAITING_REVIEW 的子任务
            List<SelfIterationJob> waitingJobs = jobRepo.findBySelfIterationTask_IdOrderByRoundAsc(taskId)
                    .stream()
                    .filter(j -> j.getStatus() == SelfIterationJob.STATUS_WAITING_REVIEW)
                    .collect(Collectors.toList());

            if (waitingJobs.isEmpty()) {
                throw new IllegalStateException("未找到等待审核的子任务");
            }

            SelfIterationJob job = waitingJobs.get(0);
            // 审核确认后先发版并做标签校验，校验通过后再进入训练
            log.info("任务[{}]第{}轮：审核确认，开始异步发布版本并校验标签", task.getId(), job.getRound());
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("任务[{}]第{}轮：进入版本发布流程", task.getId(), job.getRound());
                    List<Long> newVersionIds = publishDatasetVersions(job, task);
                    if (newVersionIds == null) {
                        log.error("任务[{}]第{}轮：版本发布失败", task.getId(), job.getRound());
                        return;
                    }

                    if (!versionsHaveAnyLabels(newVersionIds, task, job)) {
                        // 审核确认后仍无标签：继续停留在 WAITING_REVIEW/PAUSED，等待用户补标后再次确认
                        transitionToManualReview(task.getId(), job.getId(), "发版后标签数为0");
                        log.warn("任务[{}]第{}轮：发版后标签总数为0，维持人工审核等待状态", task.getId(), job.getRound());
                        return;
                    }

                    bindPublishedVersionsToTaskAndJob(task.getId(), job.getId(), newVersionIds);
                    if (!promoteToTraining(task.getId(), job.getId())) {
                        log.warn("任务[{}]第{}轮：推进训练状态失败，当前状态可能已变化", task.getId(), job.getRound());
                        return;
                    }

                    SelfIterationTask trainTaskSnapshot = transactionTemplate.execute(status -> {
                        SelfIterationTask updatedTask = taskRepo.findById(task.getId()).orElse(null);
                        if (updatedTask != null) {
                            return buildTrainTaskSnapshot(updatedTask);
                        }
                        return null;
                    });
                    if (trainTaskSnapshot == null) {
                        log.error("任务[{}]第{}轮：发版后重新读取 task 失败", task.getId(), job.getRound());
                        return;
                    }

                    SelfIterationJob latestJob = requireJob(job.getId());
                    log.info("任务[{}]第{}轮：标签校验通过，开始启动训练", task.getId(), latestJob.getRound());
                    startTrainPhase(trainTaskSnapshot, latestJob);
                } catch (Exception e) {
                    log.error("任务[{}]第{}轮：审核后启动训练异常", task.getId(), job.getRound(), e);
                } finally {
                    reviewProcessingTaskIds.remove(taskId);
                }
            });
        } catch (RuntimeException ex) {
            reviewProcessingTaskIds.remove(taskId);
            throw ex;
        }
    }

    // ===================== 采集阶段回调 =====================

    /**
     * 单个 RtspCaptureExecution 采集成功回调。
     * 检查该子任务所有 execution 是否全部完成，全部完成则推进到标注阶段。
     */
    @Transactional
    public void onCaptureComplete(Long executionId) {
        SelfIterationJob job = findJobByExecutionId(executionId);
        if (job == null) {
            log.warn("onCaptureComplete: 未找到包含 executionId={} 的子任务", executionId);
            return;
        }
        refreshCollectProgress(job.getId());
    }

    /**
     * 单个 RtspCaptureExecution 采集失败回调 → 子任务进入 COLLECT_FAILED，父任务进入 FAILED。
     */
    @Transactional
    public void onCaptureFailed(Long executionId) {
        SelfIterationJob job = findJobByExecutionId(executionId);
        if (job == null) {
            log.warn("onCaptureFailed: 未找到包含 executionId={} 的子任务", executionId);
            return;
        }
        refreshCollectProgress(job.getId());
    }

    /**
     * 刷新采集阶段进度：根据 execution 实时状态推进子任务。
     * 规则：
     * - 所有 execution 的 capturedCount 累计达到“迭代开始图片数量”后进入标注阶段
     * - 未达到阈值且所有 execution 均结束时，判定采集失败
     */
    @Transactional
    public void refreshCollectProgress(Long jobId) {
        // 同一子任务可能被多个采集回调同时推进，这里加行锁避免重复触发后续阶段
        SelfIterationJob job = requireJobForUpdate(jobId);
        refreshCollectProgress(job);
    }

    private void refreshCollectProgress(SelfIterationJob job) {
        if (job.getStatus() != SelfIterationJob.STATUS_COLLECTING) {
            return;
        }

        List<Long> executionIds = job.getCaptureExecutionIds() == null
                ? Collections.emptyList()
                : job.getCaptureExecutionIds();
        if (executionIds.isEmpty()) {
            onCollectFailed(job);
            return;
        }

        SelfIterationTask task = job.getSelfIterationTask();
        CaptureProgressSummary summary = summarizeCaptureProgress(executionIds);
        int startImageQuantity = resolveIterationStartImageQuantity(task);

        if (summary.settledCapturedCount >= startImageQuantity) {
            if (summary.runningCount > 0) {
                log.info("任务[{}]第{}轮：已结束采集执行累计图片 {}/{} 达到迭代开始阈值，停止剩余 {} 个采集执行",
                        task.getId(), job.getRound(), summary.settledCapturedCount, startImageQuantity, summary.runningCount);
                cancelRunningCaptureExecutions(summary.runningExecutionIds);
            }
            log.info("任务[{}]第{}轮：已结束采集执行累计图片 {}/{} 达到迭代开始阈值，进入标注阶段",
                    task.getId(), job.getRound(), summary.settledCapturedCount, startImageQuantity);
            onCollectDone(job);
            return;
        }

        if (summary.totalCapturedCount >= startImageQuantity) {
            log.info("任务[{}]第{}轮：实时累计采集图片 {}/{} 已达到迭代开始阈值，停止剩余 {} 个采集执行后等待确认",
                    task.getId(), job.getRound(), summary.totalCapturedCount, startImageQuantity, summary.runningCount);
            cancelRunningCaptureExecutions(summary.runningExecutionIds);
            return;
        }

        if (summary.runningCount == 0) {
            log.warn("任务[{}]第{}轮：采集执行均已结束，但累计图片数 {}/{} 未达到迭代开始阈值，判定采集失败",
                    task.getId(), job.getRound(), summary.totalCapturedCount, startImageQuantity);
            onCollectFailed(job);
        }
    }

    /**
     * 刷新模型迭代阶段进度：用于兜底推进训练/转换状态。
     *
     * <p>场景：当异步监听线程中断、回调偶发丢失时，前端轮询详情页仍可触发状态自愈，
     * 保证「训练完成 -> 自动发起转换 -> 转换完成」链路不会卡死。</p>
     */
    @Transactional
    public void refreshModelIterationProgress(Long jobId) {
        SelfIterationJob job = requireJobForUpdate(jobId);

        // 兜底：检查采集是否完成
        if (job.getStatus() == SelfIterationJob.STATUS_COLLECTING) {
            refreshCollectProgress(job);
            return;
        }

        // 兜底：检查自动标注是否完成
        if (job.getStatus() == SelfIterationJob.STATUS_AUTO_LABELING) {
            List<Long> executionIds = job.getCaptureExecutionIds();
            if (executionIds != null && !executionIds.isEmpty()) {
                int finishedCount = 0;
                for (Long executionId : executionIds) {
                    RtspCaptureExecutionVO execution = getExecutionById(executionId);
                    if (execution != null && execution.getDatasetId() != null) {
                        try {
                            DataResponseBody resp = dubheDataFeign.get(dubheUtils.getAuthorization(), execution.getDatasetId());
                            if (resp != null && resp.succeed() && resp.getData() != null) {
                                Object data = resp.getData();
                                Integer status = null;
                                if (data instanceof java.util.Map) {
                                    status = (Integer) ((java.util.Map) data).get("status");
                                } else if (data instanceof DatasetVO) {
                                    status = ((DatasetVO) data).getStatus();
                                }
                                if (status != null && status == 3) { // AUTO_FINISHED
                                    finishedCount++;
                                }
                            }
                        } catch (Exception e) {
                            log.warn("查询 dataset 标注状态失败：datasetId={}", execution.getDatasetId(), e);
                        }
                    }
                }
                if (finishedCount == executionIds.size()) {
                    log.info("任务[{}]第{}轮：兜底检测到所有 dataset 标注完成，触发推进", job.getSelfIterationTask().getId(), job.getRound());
                    for (Long executionId : executionIds) {
                        RtspCaptureExecutionVO execution = getExecutionById(executionId);
                        if (execution != null && execution.getDatasetId() != null) {
                            onAutoLabelComplete(job.getId(), execution.getDatasetId());
                        }
                    }
                }
            }
            return;
        }

        ModelJobService modelJobService = SpringContextHolder.getBean(ModelJobService.class);

        if (job.getStatus() == SelfIterationJob.STATUS_TRAINING && job.getTrainJobId() != null) {
            ModelJob trainJob = modelJobService.getModelJobById(job.getTrainJobId());
            if (trainJob == null || trainJob.getStatus() == null) {
                return;
            }
            Integer trainStatus = trainJob.getStatus();
            if (ModelJobStateCodeConstant.TRAIN_SUCCEEDED.equals(trainStatus)) {
                onTrainComplete(job.getId(), job.getTrainJobId());
            } else if (ModelJobStateCodeConstant.TRAIN_FAILED.equals(trainStatus)
                    || ModelJobStateCodeConstant.CANCELED.equals(trainStatus)) {
                onTrainFailed(job.getId());
            }
            return;
        }

        if (job.getStatus() == SelfIterationJob.STATUS_CONVERTING && job.getConvertJobId() != null) {
            ModelJob convertJob = modelJobService.getModelJobById(job.getConvertJobId());
            if (!isValidConvertJob(job, convertJob)) {
                Long resolvedConvertJobId = resolveConvertJobId(job, null);
                if (resolvedConvertJobId != null && !Objects.equals(resolvedConvertJobId, job.getConvertJobId())) {
                    Long oldConvertJobId = job.getConvertJobId();
                    job.setConvertJobId(resolvedConvertJobId);
                    jobRepo.save(job);
                    convertJob = modelJobService.getModelJobById(resolvedConvertJobId);
                    log.info("Task[{}] round[{}]: corrected convertJobId {} -> {}",
                            job.getSelfIterationTask() != null ? job.getSelfIterationTask().getId() : null,
                            job.getRound(),
                            oldConvertJobId,
                            resolvedConvertJobId);
                }
            }
            if (convertJob == null || convertJob.getStatus() == null) {
                return;
            }
            Integer convertStatus = convertJob.getStatus();
            if (ModelJobStateCodeConstant.CONVERT_SUCCEEDED.equals(convertStatus)) {
                onConvertComplete(job.getId(), job.getConvertJobId());
            } else if (ModelJobStateCodeConstant.CONVERT_FAILED.equals(convertStatus)
                    || ModelJobStateCodeConstant.CANCELED.equals(convertStatus)) {
                onTrainFailed(job.getId());
            }
            return;
        }

        if (job.getStatus() == SelfIterationJob.STATUS_PACKAGING && job.getPackageTaskId() != null) {
            refreshPackageProgress(job);
            return;
        }

        // 兜底：只要转换任务已存在且尚未进入打包任务，直接以转换任务状态驱动流程推进
        if (job.getConvertJobId() != null
                && (job.getPackageTaskId() == null || job.getPackageTaskId().trim().isEmpty())
                && job.getStatus() != SelfIterationJob.STATUS_PENDING_DEPLOY
                && job.getStatus() != SelfIterationJob.STATUS_COMPLETED
                && job.getStatus() != SelfIterationJob.STATUS_PACKAGE_FAILED) {
            ModelJob convertJob = modelJobService.getModelJobById(job.getConvertJobId());
            if (convertJob == null || convertJob.getStatus() == null) {
                return;
            }
            Integer convertStatus = convertJob.getStatus();
            if (ModelJobStateCodeConstant.CONVERT_SUCCEEDED.equals(convertStatus)) {
                log.info("Task[{}] round[{}]: refresh fallback detected convert success, promote to packaging",
                        job.getSelfIterationTask() != null ? job.getSelfIterationTask().getId() : null,
                        job.getRound());
                onConvertComplete(job.getId(), job.getConvertJobId());
            } else if (ModelJobStateCodeConstant.CONVERT_FAILED.equals(convertStatus)
                    || ModelJobStateCodeConstant.CANCELED.equals(convertStatus)) {
                onTrainFailed(job.getId());
            }
        }
    }

    /**
     * 采集失败后重采：清理旧数据集与关联，重新触发采集。
     */
    @Transactional
    public void retryCollect(Long jobId) {
        SelfIterationJob job = requireJob(jobId);
        SelfIterationTask task = job.getSelfIterationTask();

        if (job.getStatus() != SelfIterationJob.STATUS_COLLECT_FAILED) {
            throw new IllegalStateException("当前子任务状态不允许重采：status=" + job.getStatus());
        }

        // 清理旧采集产生的数据集
        List<Long> oldExecutionIds = job.getCaptureExecutionIds() == null
                ? Collections.emptyList()
                : new ArrayList<>(job.getCaptureExecutionIds());
        for (Long executionId : oldExecutionIds) {
            RtspCaptureExecutionVO execution = getExecutionById(executionId);
            if (execution == null || execution.getDatasetId() == null) {
                continue;
            }
            try {
                dubheDataFeign.deleteDataset(
                        dubheUtils.getAuthorization(),
                        new DatasetDeleteDTO(execution.getDatasetId())
                );
            } catch (Exception e) {
                log.warn("重采前删除旧数据集失败：jobId={}, executionId={}, datasetId={}",
                        jobId, executionId, execution.getDatasetId(), e);
            }
        }

        job.setCaptureExecutionIds(new ArrayList<>());
        job.setDatasetVersionIds(new ArrayList<>());
        job.setPhase(SelfIterationJob.PHASE_DATA_COLLECT);
        job.setStatus(SelfIterationJob.STATUS_CREATED);
        jobRepo.save(job);

        // 父任务从 FAILED 恢复到 RUNNING，允许继续流程
        if (task.getStatus() == SelfIterationTask.STATUS_FAILED) {
            task.setStatus(SelfIterationTask.STATUS_RUNNING);
            taskRepo.save(task);
        }

        startCollectPhase(task, job);
    }

    // ===================== 标注阶段回调 =====================

    /**
     * 单个 Dataset 自动标注完成回调。
     * 发版后将 versionId 追加到子任务，检查是否所有 dataset 都标注完毕。
     */
    @Transactional
    public void onAutoLabelComplete(Long jobId, Long datasetId) {
        log.info("收到自动标注完成回调：jobId={}, datasetId={}", jobId, datasetId);

        // 回调可能并发到达；先加锁再处理，避免重复发布版本与进度丢失
        SelfIterationJob job = requireJobForUpdate(jobId);
        SelfIterationTask task = job.getSelfIterationTask();
        if (job.getStatus() != SelfIterationJob.STATUS_AUTO_LABELING) {
            log.warn(
                    "任务[{}]第{}轮：忽略自动标注完成回调（状态已推进），jobId={}, datasetId={}, status={}",
                    task != null ? task.getId() : null,
                    job.getRound(),
                    jobId,
                    datasetId,
                    job.getStatus()
            );
            return;
        }

        // 记录该 dataset 标注完成（不发布版本，版本发布推迟到审核完成后）
        int doneCount = jobRepo.countAnnotatedDatasetsByJobId(jobId);
        // 用 datasetId 记录已完成的标注数据集（通过 annotated_dataset_ids 字段）
        jobRepo.insertAnnotatedDatasetIdIfAbsent(jobId, datasetId);
        doneCount = jobRepo.countAnnotatedDatasetsByJobId(jobId);

        int expectedCount = job.getAnnotateJobCount() > 0
                ? job.getAnnotateJobCount()
                : (job.getCaptureExecutionIds() == null ? 0 : job.getCaptureExecutionIds().size());
        log.info(
                "任务[{}]第{}轮：datasetId={} 标注完成，进度 {}/{}",
                task.getId(), job.getRound(), datasetId, doneCount, expectedCount
        );
        if (doneCount < expectedCount) {
            return;
        }

        log.info("任务[{}]第{}轮：所有数据集标注完成", task.getId(), job.getRound());

        if (task.isRequireManualReview()) {
            // 需要人工审核：子任务 → WAITING_REVIEW，父任务 → PAUSED（版本发布在审核完成后）
            SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
            jobSm.jobLabelNeedReviewEvent(job.getId());
            job.setStatus(SelfIterationJob.STATUS_WAITING_REVIEW);
            jobRepo.save(job);

            SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
            taskSm.taskPauseForReviewEvent(task.getId());
            task.setStatus(SelfIterationTask.STATUS_PAUSED);
            taskRepo.save(task);
        } else {
            // 无需审核：先发版并校验标签，若标签总数为0则自动切回人工审核
            log.info("任务[{}]第{}轮：无需审核，准备异步发布版本并校验标签", task.getId(), job.getRound());

            CompletableFuture.runAsync(() -> {
                try {
                    log.info("任务[{}]第{}轮：进入版本发布流程（无需审核）", task.getId(), job.getRound());
                    List<Long> newVersionIds = publishDatasetVersions(job, task);
                    if (newVersionIds == null) {
                        log.error("任务[{}]第{}轮：版本发布失败", task.getId(), job.getRound());
                        return;
                    }

                    if (!versionsHaveAnyLabels(newVersionIds, task, job)) {
                        transitionToManualReview(task.getId(), job.getId(), "发版后标签数为0");
                        log.warn("任务[{}]第{}轮：发版后标签总数为0，已切换到人工审核", task.getId(), job.getRound());
                        return;
                    }

                    bindPublishedVersionsToTaskAndJob(task.getId(), job.getId(), newVersionIds);
                    if (!promoteToTraining(task.getId(), job.getId())) {
                        log.warn("任务[{}]第{}轮：推进训练状态失败，当前状态可能已变化", task.getId(), job.getRound());
                        return;
                    }

                    SelfIterationTask trainTaskSnapshot = transactionTemplate.execute(status -> {
                        SelfIterationTask updatedTask = taskRepo.findById(task.getId()).orElse(null);
                        if (updatedTask != null) {
                            return buildTrainTaskSnapshot(updatedTask);
                        }
                        return null;
                    });
                    if (trainTaskSnapshot == null) {
                        log.error("任务[{}]第{}轮：发版后重新读取 task 失败", task.getId(), job.getRound());
                        return;
                    }

                    SelfIterationJob latestJob = requireJob(job.getId());
                    log.info("任务[{}]第{}轮：标签校验通过，开始启动训练", task.getId(), latestJob.getRound());
                    startTrainPhase(trainTaskSnapshot, latestJob);
                } catch (Exception e) {
                    log.error("任务[{}]第{}轮：无需审核启动训练异常", task.getId(), job.getRound(), e);
                }
            });
        }
    }

    /**
     * 单个 Dataset 自动标注失败回调 → 子任务进入 ANNOTATE_FAILED，父任务进入 FAILED。
     */
    @Transactional
    public void onAutoLabelFailed(Long jobId, Long datasetId) {
        SelfIterationJob job = requireJob(jobId);
        SelfIterationTask task = job.getSelfIterationTask();
        log.error("任务[{}]第{}轮：datasetId={} 自动标注失败", task.getId(), job.getRound(), datasetId);

        SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
        jobSm.jobLabelFailedEvent(job.getId());
        job.setStatus(SelfIterationJob.STATUS_ANNOTATE_FAILED);
        jobRepo.save(job);

        SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
        taskSm.taskFailEvent(task.getId());
        task.setStatus(SelfIterationTask.STATUS_FAILED);
        taskRepo.save(task);
    }

    // ===================== 训练/转换/打包回调 =====================

    /**
     * 训练完成回调：TRAINING → CONVERTING
     */
    @Transactional
    public void onTrainComplete(Long jobId, Long modelJobId) {
        SelfIterationJob job = requireJob(jobId);
        SelfIterationTask task = job.getSelfIterationTask();
        Long taskId = task != null ? task.getId() : null;

        if (job.getStatus() != SelfIterationJob.STATUS_TRAINING
                && job.getStatus() != SelfIterationJob.STATUS_CONVERTING) {
            log.warn("任务[{}]第{}轮：忽略训练完成回调，当前状态不是训练/转换中，status={}",
                    task.getId(), job.getRound(), job.getStatus());
            return;
        }

        if (job.getStatus() == SelfIterationJob.STATUS_CONVERTING && job.getConvertJobId() != null) {
            log.info("任务[{}]第{}轮：训练完成回调重复，转换任务已存在，convertJobId={}",
                    task.getId(), job.getRound(), job.getConvertJobId());
            return;
        }

        job.setTrainJobId(modelJobId);
        SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
        jobSm.jobTrainDoneEvent(job.getId());
        job.setStatus(SelfIterationJob.STATUS_CONVERTING);
        jobRepo.save(job);

        // 构建快照，避免异步线程/非代理调用下访问懒加载字段触发 LazyInitializationException
        SelfIterationTask convertTaskSnapshot = transactionTemplate.execute(status -> {
            if (taskId == null) {
                return null;
            }
            SelfIterationTask freshTask = taskRepo.findById(taskId).orElse(null);
            if (freshTask == null) {
                return null;
            }
            return buildTrainTaskSnapshot(freshTask);
        });

        if (convertTaskSnapshot == null) {
            log.error("任务[{}]第{}轮：训练完成后构建转换快照失败", taskId, job.getRound());
            onTrainFailed(job.getId());
            return;
        }

        log.info("任务[{}]第{}轮：训练完成，modelJobId={}，开始转换", taskId, job.getRound(), modelJobId);
        CompletableFuture.runAsync(() -> startConvertPhaseWithRetry(convertTaskSnapshot, job.getId()));
    }

    /**
     * 训练或转换失败回调：→ TRAIN_FAILED，父任务 → FAILED
     */
    @Transactional
    public void onTrainFailed(Long jobId) {
        SelfIterationJob job = requireJob(jobId);
        SelfIterationTask task = job.getSelfIterationTask();
        log.error("任务[{}]第{}轮：训练/转换失败", task.getId(), job.getRound());

        SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
        jobSm.jobTrainFailedEvent(job.getId());
        job.setStatus(SelfIterationJob.STATUS_TRAIN_FAILED);
        jobRepo.save(job);

        SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
        taskSm.taskFailEvent(task.getId());
        task.setStatus(SelfIterationTask.STATUS_FAILED);
        taskRepo.save(task);
    }

    /**
     * 转换完成回调：CONVERTING → PACKAGING（暂时直接跳过打包到 PENDING_DEPLOY）
     */
    public void onConvertComplete(Long jobId, Long convertModelJobId) {
        PackagePhaseTrigger trigger = transactionTemplate.execute(status -> {
            SelfIterationJob job = requireJobForUpdate(jobId);
            SelfIterationTask task = job.getSelfIterationTask();

            if (job.getPackageTaskId() != null && !job.getPackageTaskId().trim().isEmpty()) {
                log.info("任务[{}]第{}轮：忽略重复转换完成回调，已存在打包任务 packageTaskId={}",
                        task.getId(), job.getRound(), job.getPackageTaskId());
                return null;
            }
            if (job.getStatus() == SelfIterationJob.STATUS_PACKAGING
                    || job.getStatus() == SelfIterationJob.STATUS_PENDING_DEPLOY
                    || job.getStatus() == SelfIterationJob.STATUS_COMPLETED) {
                log.info("任务[{}]第{}轮：忽略重复转换完成回调，当前状态={}",
                        task.getId(), job.getRound(), job.getStatus());
                return null;
            }

            job.setConvertJobId(convertModelJobId);
            SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
            jobSm.jobConvertDoneEvent(job.getId());
            job.setStatus(SelfIterationJob.STATUS_PACKAGING);
            job.setPhase(SelfIterationJob.PHASE_MODEL_TRAIN);
            jobRepo.save(job);

            log.info("任务[{}]第{}轮：转换完成，convertModelJobId={}，开始打包", task.getId(), job.getRound(), convertModelJobId);
            return new PackagePhaseTrigger(task.getId(), job.getId());
        });

        if (trigger == null) {
            return;
        }
        startPackagePhase(trigger.taskId, trigger.jobId);
    }

    /**
     * 打包完成回调（预留）
     */
    @Transactional
    public void onPackageComplete(Long jobId) {
        Boolean shouldComplete = transactionTemplate.execute(status -> {
            SelfIterationJob job = requireJobForUpdate(jobId);
            if (job.getStatus() == SelfIterationJob.STATUS_PENDING_DEPLOY
                    || job.getStatus() == SelfIterationJob.STATUS_COMPLETED) {
                log.info("任务[{}]第{}轮：忽略重复打包完成回调，status={}", job.getSelfIterationTask().getId(), job.getRound(), job.getStatus());
                return false;
            }
            if (job.getStatus() != SelfIterationJob.STATUS_PACKAGING) {
                log.warn("任务[{}]第{}轮：忽略打包完成回调，当前状态非打包中，status={}", job.getSelfIterationTask().getId(), job.getRound(), job.getStatus());
                return false;
            }
            SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
            jobSm.jobPackageDoneEvent(job.getId());
            job.setStatus(SelfIterationJob.STATUS_PENDING_DEPLOY);
            job.setPhase(SelfIterationJob.PHASE_MODEL_DEPLOY);
            jobRepo.save(job);
            return true;
        });

        if (!Boolean.TRUE.equals(shouldComplete)) {
            return;
        }

        SelfIterationJob job = requireJob(jobId);
        SelfIterationTask task = job.getSelfIterationTask();
        if (task != null && task.isAutoDeployEnabled() && shouldAutoDeployCurrentRound(task, job)) {
            SelfIterationGpuDispatchService.DispatchResult dispatchResult = dispatchPackageForJob(job);
            if (!dispatchResult.isSuccess()) {
                onPackageFailed(jobId);
                return;
            }
        }
        completeRound(task, job);
    }

    private boolean shouldAutoDeployCurrentRound(SelfIterationTask task, SelfIterationJob job) {
        if (task == null || job == null) {
            return false;
        }
        if (isMetricStopReached(task, job)) {
            return true;
        }
        Integer maxRounds = task.getMaxRounds();
        return maxRounds != null && task.getCurrentRound() + 1 >= maxRounds;
    }

    /**
     * 打包失败回调（预留）
     */
    @Transactional
    public void onPackageFailed(Long jobId) {
        SelfIterationJob job = requireJob(jobId);
        SelfIterationTask task = job.getSelfIterationTask();

        if (job.getStatus() == SelfIterationJob.STATUS_PACKAGE_FAILED) {
            return;
        }
        log.error("任务[{}]第{}轮：打包失败", task.getId(), job.getRound());

        SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
        jobSm.jobPackageFailedEvent(job.getId());
        job.setStatus(SelfIterationJob.STATUS_PACKAGE_FAILED);
        jobRepo.save(job);

        SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
        taskSm.taskFailEvent(task.getId());
        task.setStatus(SelfIterationTask.STATUS_FAILED);
        taskRepo.save(task);
    }

    public Map<String, Object> testGpuDispatchPayload(String packageTaskId, List<Long> gpuUrlTargetIds, boolean execute) {
        if (packageTaskId == null || packageTaskId.trim().isEmpty()) {
            throw new IllegalArgumentException("packageTaskId 不能为空");
        }
        List<GpuUrlTarget> targets = gpuUrlTargetService.findAvailableByIds(gpuUrlTargetIds);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("packageTaskId", packageTaskId);
        result.put("targets", targets.stream().map(this::buildDispatchPreviewTarget).collect(Collectors.toList()));
        result.put("targetCount", targets.size());
        result.put("multipartFileField", "appZip");
        result.put("multipartStringField", "gpuUrl");
        if (execute) {
            byte[] packedBytes = packageFeign.getPackedFile(packageTaskId);
            result.put("binaryBytes", packedBytes == null ? 0 : packedBytes.length);
            result.put("dispatchResult", gpuDispatchService.dispatchToTargets(packedBytes, targets));
        } else {
            result.put("binaryBytes", "execute=false 时不读取一次性打包文件");
        }
        return result;
    }

    @Transactional
    public SelfIterationGpuDispatchService.DispatchResult manualDispatchPackage(Long jobId) {
        SelfIterationJob job = requireJobForUpdate(jobId);
        if (job.getStatus() != SelfIterationJob.STATUS_PENDING_DEPLOY
                && job.getStatus() != SelfIterationJob.STATUS_COMPLETED) {
            throw new IllegalStateException("当前子任务状态无法手动下发");
        }
        SelfIterationGpuDispatchService.DispatchResult result = dispatchPackageForJob(job);
        if (!result.isSuccess()) {
            onPackageFailed(jobId);
            return result;
        }
        if (job.getStatus() == SelfIterationJob.STATUS_PENDING_DEPLOY) {
            completeRound(job.getSelfIterationTask(), job);
        }
        return result;
    }

    // ===================== 内部辅助 =====================

    /**
     * 创建新一轮子任务
     */
    private SelfIterationJob createNewJob(SelfIterationTask task, int round) {
        SelfIterationJob job = new SelfIterationJob();
        job.setSelfIterationTask(task);
        job.setRound(round);
        job.setPhase(SelfIterationJob.PHASE_DATA_COLLECT);
        job.setStatus(SelfIterationJob.STATUS_CREATED);
        return jobRepo.save(job);
    }

    /**
     * 启动数据采集阶段：对每个 dataSourceId 触发 startCapture，记录 executionId。
     */
    private void startCollectPhase(SelfIterationTask task, SelfIterationJob job) {
        startCollectPhase(task, job, null);
    }

    private void startCollectPhase(SelfIterationTask task, SelfIterationJob job, Boolean delRawOverride) {
        // 在事务内读取 task 和 dataSourceIds，避免懒加载异常
        CollectConfig collectConfig = transactionTemplate.execute(ts -> {
            SelfIterationTask freshTask = taskRepo.findById(task.getId())
                    .orElseThrow(() -> new IllegalArgumentException("任务不存在：id=" + task.getId()));
            boolean delRaw = delRawOverride != null ? delRawOverride : freshTask.isDeleteRawAfterCollect();
            return new CollectConfig(normalizeCaptureTaskIds(freshTask), delRaw);
        });
        List<Long> captureTaskIds = collectConfig == null ? Collections.emptyList() : collectConfig.getCaptureTaskIds();
        boolean delRaw = collectConfig != null && collectConfig.isDelRaw();
        if (captureTaskIds == null) captureTaskIds = Collections.emptyList();

        SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
        jobSm.jobStartCollectEvent(job.getId());
        job.setStatus(SelfIterationJob.STATUS_COLLECTING);
        jobRepo.save(job);

        List<Long> executionIds = new ArrayList<>();
        for (Long captureTaskId : captureTaskIds) {
            try {
                DataResponseBody<Boolean> resp = dubheDataFeign.startCapture(
                        dubheUtils.getAuthorization(), captureTaskId, Collections.singletonMap("delRaw", delRaw));
                if (resp != null && resp.succeed() && Boolean.TRUE.equals(resp.getData())) {
                    Long executionId = resolveLatestExecutionId(captureTaskId);
                    if (executionId != null) {
                        executionIds.add(executionId);
                    } else {
                        log.error("任务[{}]第{}轮：captureTaskId={} 已触发采集，但未获取到 executionId", task.getId(), job.getRound(), captureTaskId);
                    }
                } else {
                    log.error("任务[{}]第{}轮：captureTaskId={} 启动采集失败", task.getId(), job.getRound(), captureTaskId);
                }
            } catch (Exception e) {
                log.error("任务[{}]第{}轮：captureTaskId={} 启动采集异常", task.getId(), job.getRound(), captureTaskId, e);
            }
        }

        job.setCaptureExecutionIds(executionIds);
        jobRepo.save(job);

        if (executionIds.isEmpty()) {
            log.error("任务[{}]第{}轮：所有采集任务启动失败", task.getId(), job.getRound());
            onCollectFailed(job);
        }
    }

    private List<Long> normalizeCaptureTaskIds(SelfIterationTask task) {
        List<Long> raw = task.getDataSourceIds();
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> normalized = new ArrayList<>(raw.size());
        boolean changed = false;
        for (int i = 0; i < raw.size(); i++) {
            Long captureTaskId = raw.get(i);
            if (captureTaskId != null && captureTaskId > 0) {
                normalized.add(captureTaskId);
                continue;
            }
            Long fixedId = resolveCaptureTaskIdBySlotName(task.getName(), i + 1);
            if (fixedId != null && fixedId > 0) {
                log.warn("任务[{}] 自动修复无效采集任务ID：slot={}, {} -> {}", task.getId(), i + 1, captureTaskId, fixedId);
                normalized.add(fixedId);
                changed = true;
            } else {
                normalized.add(captureTaskId);
            }
        }
        if (changed) {
            task.setDataSourceIds(normalized);
            taskRepo.save(task);
        }
        return normalized;
    }

    private Long resolveCaptureTaskIdBySlotName(String taskName, int slotIndex) {
        String captureTaskName = "capture-" + taskName + "-" + slotIndex;
        String authorization = dubheUtils.getAuthorization();
        for (int i = 0; i < 10; i++) {
            try {
                DataResponseBody<Page<DubheDataFeign.RtspCaptureTaskVO>> pageResp =
                        dubheDataFeign.pageRtspCaptureTasks(authorization, 1L, 1L, captureTaskName);
                if (pageResp != null && pageResp.succeed() && pageResp.getData() != null
                        && pageResp.getData().getRecords() != null && !pageResp.getData().getRecords().isEmpty()) {
                    DubheDataFeign.RtspCaptureTaskVO taskVO = pageResp.getData().getRecords().get(0);
                    if (taskVO != null && taskVO.getId() != null) {
                        return taskVO.getId();
                    }
                }
            } catch (Exception e) {
                log.warn("按槽位名回查采集任务失败：taskName={}, slot={}, attempt={}", taskName, slotIndex, i + 1, e);
            }
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private void onCollectDone(SelfIterationJob job) {
        SelfIterationTask task = job.getSelfIterationTask();
        if (job.getStatus() != SelfIterationJob.STATUS_COLLECTING) {
            return;
        }
        log.info("任务[{}]第{}轮：采集判定成功，进入标注阶段", task.getId(), job.getRound());
        SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
        jobSm.jobCollectDoneEvent(job.getId());
        job.setStatus(SelfIterationJob.STATUS_AUTO_LABELING);
        job.setPhase(SelfIterationJob.PHASE_DATA_ANNOTATE);
        jobRepo.save(job);

        // 提前加载懒加载字段，避免事务外访问失败
        List<Long> executionIds = job.getCaptureExecutionIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(job.getCaptureExecutionIds());
        Long taskId = task.getId();
        Long jobId = job.getId();
        Integer round = job.getRound();

        // 事务外异步启动标注，避免阻塞事务
        CompletableFuture.runAsync(() -> {
            try {
                SelfIterationTask freshTask = taskRepo.findById(taskId).orElse(null);
                SelfIterationJob freshJob = jobRepo.findById(jobId).orElse(null);
                if (freshTask != null && freshJob != null) {
                    // 恢复懒加载字段
                    freshJob.setCaptureExecutionIds(executionIds);
                    startAnnotatePhase(freshTask, freshJob);
                }
            } catch (Exception e) {
                log.error("任务[{}]第{}轮：启动标注阶段失败", taskId, round, e);
                onAutoLabelFailed(jobId, -1L);
            }
        });
    }

    private void onCollectFailed(SelfIterationJob job) {
        SelfIterationTask task = job.getSelfIterationTask();
        if (job.getStatus() != SelfIterationJob.STATUS_COLLECTING
                && job.getStatus() != SelfIterationJob.STATUS_CREATED) {
            return;
        }
        log.error("任务[{}]第{}轮：采集判定失败", task.getId(), job.getRound());
        SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
        if (job.getStatus() == SelfIterationJob.STATUS_COLLECTING) {
            jobSm.jobCollectFailedEvent(job.getId());
        }
        job.setStatus(SelfIterationJob.STATUS_COLLECT_FAILED);
        job.setPhase(SelfIterationJob.PHASE_DATA_COLLECT);
        jobRepo.save(job);

        SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
        taskSm.taskFailEvent(task.getId());
        task.setStatus(SelfIterationTask.STATUS_FAILED);
        taskRepo.save(task);
    }

    /**
     * 启动自动标注阶段：对本轮所有 execution 产出的 datasetId 触发标注。
     * 第1轮 useDefault=true；第N轮 useDefault=false + 上一轮训练权重。
     */
    private void startAnnotatePhase(SelfIterationTask task, SelfIterationJob job) {
        log.info("任务[{}]第{}轮：进入 startAnnotatePhase，jobId={}, jobStatus={}", task.getId(), job.getRound(), job.getId(), job.getStatus());
        List<Long> labelIds = zipLabelService.getBoundLabelIds(task.getModelApplication().getId());
        List<String> labelNames = getLabelNames(labelIds);
        log.info("Task[{}] round[{}]: start auto-label submit, labelCount={}, labels={}",
                task.getId(),
                job.getRound(),
                labelNames == null ? 0 : labelNames.size(),
                labelNames);
        if (labelNames == null || labelNames.isEmpty()) {
            log.warn("Task[{}] round[{}]: executionId={} has no datasetId, skip", task.getId(), job.getRound());
        }

        boolean isFirstRound = (job.getRound() == 1);
        String strategy = task.getReuseAnnotationModel();
        String prevWeightPath = null;
        boolean useDefault = true;
        boolean useTrainedInitialAutoLabel = isFirstRound
                && "TRAINED_MODEL".equals(task.getInitialAutoLabelSource());

        if (!isFirstRound) {
            log.info("任务[{}]第{}轮：标注模型复用策略={}", task.getId(), job.getRound(), strategy);
            if ("ALWAYS".equals(strategy)) {
                prevWeightPath = getPrevWeightPath(task, job.getRound());
                if (prevWeightPath != null) {
                    useDefault = false;
                    log.info("任务[{}]第{}轮：使用上一轮权重路径={}", task.getId(), job.getRound(), prevWeightPath);
                } else {
                    log.warn("任务[{}]第{}轮：ALWAYS 策略但上一轮权重路径为空，回退到默认模型", task.getId(), job.getRound());
                    useDefault = true;
                }
            } else if ("NEVER".equals(strategy)) {
                useDefault = true;
            } else if ("METRIC_COMPARE".equals(strategy)) {
                if (shouldReuseBasedOnMetrics(task, job.getRound())) {
                    prevWeightPath = getPrevWeightPath(task, job.getRound());
                    if (prevWeightPath != null) {
                        useDefault = false;
                    } else {
                        log.warn("任务[{}]第{}轮：METRIC_COMPARE 策略但上一轮权重路径为空，回退到默认模型", task.getId(), job.getRound());
                        useDefault = true;
                    }
                }
            }
        } else if (useTrainedInitialAutoLabel) {
            useDefault = false;
        }

        String imageUrl = resolveAutoLabelImageUrlFromGeneration(task, job.getRound(), isFirstRound, strategy);
        log.info("任务[{}]第{}轮：autoLabelImageUrl={}, useDefault={}", task.getId(), job.getRound(), imageUrl, useDefault);
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            log.error("任务[{}]：autoLabelImageUrl 未配置，无法启动标注", task.getId());
            onAutoLabelFailed(job.getId(), -1L);
            return;
        }

        int submittedCount = 0;
        List<Long> executionIds = job.getCaptureExecutionIds();
        log.info("任务[{}]第{}轮：captureExecutionIds={}", task.getId(), job.getRound(), executionIds);
        for (Long executionId : executionIds) {
            RtspCaptureExecutionVO execution = getExecutionById(executionId);
            if (execution == null || execution.getDatasetId() == null) {
                log.warn("任务[{}]第{}轮：executionId={} 未找到对应 dataset，跳过", task.getId(), job.getRound(), executionId);
                continue;
            }
            Long datasetId = execution.getDatasetId();
            String datasetPath = "dataset/" + datasetId;
            String jobName = "autolabel-iter" + task.getId() + "-r" + job.getRound() + "-job" + job.getId() + "-ds" + datasetId;

            // 构建并提交 K8s 自动标注 Job。createAutoLabelJob 内部会统一负责
            // 创建 K8s Job 并通知 dubhe 进入自动标注状态，避免自迭代链路重复启动。
            AutoLabelJobRequest request = new AutoLabelJobRequest();
            request.setJobName(jobName);
            request.setDatasetPath(datasetPath);
            request.setUseDefault(useDefault);
            request.setImageUrl(imageUrl);
            if (useTrainedInitialAutoLabel) {
                request.setModelGenerationId(task.getInitialAutoLabelModelGenerationId());
                if ("STANDARD".equals(task.getInitialAutoLabelTrainSource())) {
                    request.setStandardJobName(task.getInitialAutoLabelStandardJobName());
                }
            } else {
                request.setDirectWeightPath(prevWeightPath);
            }
            request.setLabelNames(labelNames);
            request.setClearExistingLabels(true);
            request.setRequireManualConfirmation(false);

            Map<String, String> params = new java.util.HashMap<>();
            params.put("MODE_WORKING_MODE", "5");
            Double confidenceThreshold = getConfidenceThreshold(task);
            params.put("HP_CONFIDENCE", String.valueOf(confidenceThreshold != null ? confidenceThreshold : 0.25));
            request.setParams(params);

            log.info("任务[{}]第{}轮：提交 K8s 自动标注 Job，jobName={}, datasetPath={}, useDefault={}, imageUrl={}",
                    task.getId(), job.getRound(), jobName, datasetPath, useDefault, imageUrl);
            try {
                SpringContextHolder.getBean(
                        org.dlut.adv.mineai.model.kubernetes.controller.JobController.class
                ).createAutoLabelJob(request);
                submittedCount++;
                log.info("Task[{}] round[{}]: datasetId={} auto-label job submitted", task.getId(), job.getRound(), datasetId);
            } catch (Exception e) {
                log.error("Task[{}] round[{}]: create autolabel k8s job failed for datasetId={}", task.getId(), job.getRound(), datasetId, e);
                onAutoLabelFailed(job.getId(), datasetId);
                return;
            }
        }

        if (submittedCount == 0) {
            log.error("Task[{}] round[{}]: no valid dataset submitted for autolabel", task.getId(), job.getRound());
            onAutoLabelFailed(job.getId(), -1L);
            return;
        }

        // 记录实际提交数，供 onAutoLabelComplete 判断全部完成
        job.setAnnotateJobCount(submittedCount);
        jobRepo.save(job);
        log.info("任务[{}]第{}轮：共提交 {} 个自动标注 Job", task.getId(), job.getRound(), submittedCount);
    }

    /**
     * 启动训练阶段：使用全量历史版本创建 ModelJob。
     */
    private SelfIterationTask buildTrainTaskSnapshot(SelfIterationTask task) {
        SelfIterationTask snapshot = new SelfIterationTask();
        snapshot.setId(task.getId());
        snapshot.setUserId(task.getUserId());
        snapshot.setModelApplication(task.getModelApplication());
        snapshot.setModelType(task.getModelType());
        snapshot.setHardwareParams(task.getHardwareParams());
        snapshot.setGpuMode(task.getGpuMode());
        snapshot.setGpuCount(task.getGpuCount());
        snapshot.setSplitSize(task.getSplitSize());
        snapshot.setAllDatasetVersionIds(task.getAllDatasetVersionIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(task.getAllDatasetVersionIds()));
        snapshot.setHyperParams(task.getHyperParams() == null
                ? new HashMap<>()
                : new HashMap<>(task.getHyperParams()));
        snapshot.setSelectedLabels(task.getSelectedLabels() == null
                ? new HashMap<>()
                : new HashMap<>(task.getSelectedLabels()));
        snapshot.setConvertParams(task.getConvertParams() == null
                ? new HashMap<>()
                : new HashMap<>(task.getConvertParams()));
        return snapshot;
    }

    private void startTrainPhase(SelfIterationTask task, SelfIterationJob job) {
        int versionCount = task.getAllDatasetVersionIds() == null ? 0 : task.getAllDatasetVersionIds().size();
        log.info("Task[{}] round[{}]: start train, datasetVersionCount={}", task.getId(), job.getRound(), versionCount);

        try {
            ModelJobService modelJobService = SpringContextHolder.getBean(ModelJobService.class);

            ModelGeneration modelGeneration = getOrCreateModelGeneration(task);
            modelGeneration.setTrainDatasetVersions(new ArrayList<>(task.getAllDatasetVersionIds()));
            modelGeneration.setHyperParams(new HashMap<>(task.getHyperParams()));
            modelGeneration.setSelectedLabels(new HashMap<>(task.getSelectedLabels()));
            modelGeneration.setHardwareParams(task.getHardwareParams());
            modelGeneration.setGpuMode(task.getGpuMode());
            modelGeneration.setGpuCount(task.getGpuCount());
            modelGeneration.setSplitSize(task.getSplitSize());

            ModelGenerationService modelGenerationService = SpringContextHolder.getBean(ModelGenerationService.class);
            modelGenerationService.saveModelGeneration(modelGeneration);

            Long preJobId = null;
            if (job.getRound() > 1) {
                String strategy = task.getReuseTrainModel();
                if ("ALWAYS".equals(strategy)) {
                    Optional<SelfIterationJob> prevJobOpt = jobRepo.findBySelfIterationTask_IdAndRound(task.getId(), job.getRound() - 1);
                    if (prevJobOpt.isPresent() && prevJobOpt.get().getTrainJobId() != null) {
                        preJobId = prevJobOpt.get().getTrainJobId();
                    }
                } else if ("NEVER".equals(strategy)) {
                    preJobId = null;
                } else if ("METRIC_COMPARE".equals(strategy)) {
                    if (shouldReuseBasedOnMetrics(task, job.getRound())) {
                        Optional<SelfIterationJob> prevJobOpt = jobRepo.findBySelfIterationTask_IdAndRound(task.getId(), job.getRound() - 1);
                        if (prevJobOpt.isPresent() && prevJobOpt.get().getTrainJobId() != null) {
                            preJobId = prevJobOpt.get().getTrainJobId();
                        }
                    }
                }
            }

            Msg<String> result = modelJobService.createJob(
                    modelGeneration.getId(),
                    preJobId,
                    1,
                    task.getModelType()
            );

            if (result == null || !MsgCode.SUCCEED.getCode().equals(result.getCode())) {
                log.error("Task[{}] round[{}]: create train job failed", task.getId(), job.getRound());
                onTrainFailed(job.getId());
                return;
            }

            // 检查是否需要数据集切分（异步任务）
            if (result.getPayload() != null && result.getPayload().contains("数据集切分任务已提交")) {
                log.info("Task[{}] round[{}]: dataset split task submitted, waiting for completion", task.getId(), job.getRound());
                // 数据集切分是异步的，训练Job会在切分完成后自动创建
                // 通过轮询 modelGeneration.latestJobId 来等待训练Job创建
                CompletableFuture.runAsync(() -> waitForTrainJobCreation(task, job, modelGeneration.getId()));
                return;
            }

            Long trainJobId = modelGeneration.getLatestJobId();
            job.setTrainJobId(trainJobId);
            jobRepo.save(job);

            log.info("Task[{}] round[{}]: train job created, jobId={}", task.getId(), job.getRound(), trainJobId);
            CompletableFuture.runAsync(() -> trackTrainJobStatus(task, job));
        } catch (Exception e) {
            log.error("Task[{}] round[{}]: start train failed", task.getId(), job.getRound(), e);
            onTrainFailed(job.getId());
        }
    }

    /**
     * 启动转换阶段（扩展点）
     */
    private boolean startConvertPhase(SelfIterationTask task, SelfIterationJob job) {
        log.info("Task[{}] round[{}]: start convert phase", task.getId(), job.getRound());

        try {
            ModelJobService modelJobService = SpringContextHolder.getBean(ModelJobService.class);
            ModelJob trainJob = modelJobService.getModelJobById(job.getTrainJobId());

            if (trainJob == null) {
                log.error("Task[{}] round[{}]: train job not found before convert", task.getId(), job.getRound());
                return false;
            }

            ModelGeneration modelGeneration = getOrCreateModelGeneration(task);

            ModelConvertDTO convertDTO = new ModelConvertDTO();
            String convertJobName = trainJob.getWeightPath() + "-convert-" + System.currentTimeMillis();
            convertDTO.setJobName(convertJobName);
            convertDTO.setOutputWeightPath(convertJobName);
            convertDTO.setWeightPath(trainJob.getWeightPath());
            convertDTO.setGpuNum(trainJob.getGpus());
            convertDTO.setCpuNum(trainJob.getCpus());
            convertDTO.setParams(new HashMap<>(task.getConvertParams()));

            ModelVersion convertModelVersion = modelGeneration.getModelExplore().getConvertModelVersion();
            if (convertModelVersion == null) {
                log.error("Task[{}] round[{}]: convert image not found", task.getId(), job.getRound());
                return false;
            }
            convertDTO.setImage(convertModelVersion.getUrl());

            Msg<String> result = modelJobService.createConvertJob(
                    modelGeneration.getId(),
                    convertDTO,
                    null
            );

            if (result == null || !MsgCode.SUCCEED.getCode().equals(result.getCode())) {
                log.error("Task[{}] round[{}]: create convert job failed, code={}, text={}",
                        task.getId(),
                        job.getRound(),
                        result == null ? null : result.getCode(),
                        result == null ? null : result.getText());
                return false;
            }

            Long convertJobId = resolveConvertJobId(job, convertJobName);

            if (convertJobId == null || convertJobId <= 0) {
                log.error("Task[{}] round[{}]: convert job created but id not found", task.getId(), job.getRound());
                return false;
            }

            job.setConvertJobId(convertJobId);
            jobRepo.save(job);

            log.info("Task[{}] round[{}]: convert job created, jobId={}", task.getId(), job.getRound(), convertJobId);
            CompletableFuture.runAsync(() -> trackConvertJobStatus(task, job));
            return true;
        } catch (Exception e) {
            log.error("Task[{}] round[{}]: start convert failed", task.getId(), job.getRound(), e);
            return false;
        }
    }

    private void startConvertPhaseWithRetry(SelfIterationTask task, Long jobId) {
        final int maxAttempts = 5;
        final long retryDelayMs = 5000L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Thread.sleep(retryDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Task[{}]: interrupted while waiting to start convert phase", task.getId(), e);
                onTrainFailed(jobId);
                return;
            }

            try {
                SelfIterationJob latestJob = requireJob(jobId);
                if (latestJob.getStatus() != SelfIterationJob.STATUS_CONVERTING) {
                    log.warn("Task[{}] round[{}]: abort convert retry because job status changed to {}",
                            task.getId(), latestJob.getRound(), latestJob.getStatus());
                    return;
                }
                if (latestJob.getConvertJobId() != null && latestJob.getConvertJobId() > 0) {
                    log.info("Task[{}] round[{}]: convert job already created before retry completes, convertJobId={}",
                            task.getId(), latestJob.getRound(), latestJob.getConvertJobId());
                    return;
                }
                if (attempt > 1) {
                    log.warn("Task[{}] round[{}]: retry starting convert phase, attempt {}/{}",
                            task.getId(), latestJob.getRound(), attempt, maxAttempts);
                }
                if (startConvertPhase(task, latestJob)) {
                    return;
                }
            } catch (Exception e) {
                log.error("Task[{}]: unexpected error during convert retry flow, attempt {}/{}",
                        task.getId(), attempt, maxAttempts, e);
            }
        }

        SelfIterationJob failedJob = requireJob(jobId);
        log.error("Task[{}] round[{}]: convert start failed after {} attempts",
                task.getId(), failedJob.getRound(), maxAttempts);
        onTrainFailed(jobId);
    }

    private Long resolveConvertJobId(SelfIterationJob job, String expectedConvertJobName) {
        ModelGeneration modelGeneration = null;
        if (job.getSelfIterationTask() != null) {
            modelGeneration = getOrCreateModelGeneration(job.getSelfIterationTask());
        }
        Long candidateId = modelGeneration != null ? modelGeneration.getLatestJobId() : null;
        if (candidateId != null && candidateId > 0) {
            ModelJob candidate = modelJobRepo.findModelJobById(candidateId);
            if (isValidConvertJob(job, candidate)
                    && (expectedConvertJobName == null || expectedConvertJobName.equals(candidate.getName()))) {
                return candidateId;
            }
        }

        if (expectedConvertJobName != null && !expectedConvertJobName.trim().isEmpty()) {
            ModelJob byName = modelJobRepo.findModelJobByName(expectedConvertJobName);
            if (isValidConvertJob(job, byName)) {
                return byName.getId();
            }
        }

        if (job.getTrainJobId() != null) {
            ModelJobService modelJobService = SpringContextHolder.getBean(ModelJobService.class);
            List<ModelJob> convertJobs = modelJobService.getConvertJobsByTrainJobID(job.getTrainJobId());
            if (convertJobs != null && !convertJobs.isEmpty()) {
                return convertJobs.stream()
                        .filter(candidate -> isValidConvertJob(job, candidate))
                        .max(Comparator.comparingLong(ModelJob::getId))
                        .map(ModelJob::getId)
                        .orElse(null);
            }
        }

        return null;
    }

    private boolean isValidConvertJob(SelfIterationJob job, ModelJob candidate) {
        if (candidate == null) {
            return false;
        }
        if (candidate.getJobType() != ModelJob.CONVERT) {
            return false;
        }
        if (job.getTrainJobId() != null && Objects.equals(candidate.getId(), job.getTrainJobId())) {
            return false;
        }
        return true;
    }

    /**
     * 启动打包阶段：调用 package-manager 生成算法包，并在完成后自动流转到模型下发阶段。
     */
    private void startPackagePhase(Long taskId, Long jobId) {
        log.info("Task[{}] job[{}]: start package phase", taskId, jobId);

        try {
            PackageStartSnapshot snapshot = transactionTemplate.execute(status -> buildPackageStartSnapshot(taskId, jobId));
            if (snapshot == null) {
                log.error("Task[{}] job[{}]: package snapshot is null", taskId, jobId);
                onPackageFailed(jobId);
                return;
            }

            // 1) 上传待替换模型到 MinIO 临时路径
            String objectName = "tmp/" + snapshot.localFileName;
            if (!minioUtils.uploadLocalFile(snapshot.localModelPath, objectName)) {
                log.error("Task[{}] round[{}]: upload model for package failed, path={}",
                        snapshot.taskId, snapshot.round, snapshot.localModelPath);
                onPackageFailed(jobId);
                return;
            }

            // 2) 写入 classes.txt（用于 package-manager 替换标签文件）
            String classesUrl = "package/" + snapshot.convertJobName + "/classes.txt";
            minioUtils.writeString(minioUtils.getBucketName(), classesUrl, generateClassesTxtFromMap(snapshot.selectedLabels));

            // 3) 发起打包任务
            String packageTaskId;
            try {
                packageTaskId = packageFeign.startAlterAndPack(
                        snapshot.appZipPath,
                        objectName,
                        snapshot.localFileName,
                        classesUrl,
                        snapshot.authCode == null ? "" : snapshot.authCode
                );
            } catch (Exception e) {
                // 本地开发环境可能没有注册 package-manager 服务，降级为“本地直通”以便流程联调
                if (hasCause(e, UnknownHostException.class)) {
                    File localFile = new File(snapshot.localModelPath);
                    long binarySize = localFile.exists() ? localFile.length() : 0L;
                    log.warn("Task[{}] round[{}]: package-manager unavailable, fallback local passthrough, model={}, bytes={}",
                            snapshot.taskId, snapshot.round, snapshot.localModelPath, binarySize);
                    logBoundInferenceDevices(snapshot.taskId, snapshot.round, snapshot.inferenceDeviceIds, "fallback");
                    log.info("Task[{}] round[{}]: package completed by fallback (待接入甲方下发)",
                            snapshot.taskId, snapshot.round);
                    onPackageComplete(jobId);
                    return;
                }
                throw e;
            }

            if (packageTaskId == null || packageTaskId.trim().isEmpty()) {
                log.error("Task[{}] round[{}]: package manager returned empty taskId", snapshot.taskId, snapshot.round);
                onPackageFailed(jobId);
                return;
            }

            transactionTemplate.executeWithoutResult(status -> {
                SelfIterationJob latestJob = requireJobForUpdate(jobId);
                latestJob.setPackageTaskId(packageTaskId);
                latestJob.setStatus(SelfIterationJob.STATUS_PACKAGING);
                latestJob.setPhase(SelfIterationJob.PHASE_MODEL_TRAIN);
                jobRepo.save(latestJob);
            });

            log.info("Task[{}] round[{}]: package task created, packageTaskId={}",
                    snapshot.taskId, snapshot.round, packageTaskId);

            CompletableFuture.runAsync(() -> trackPackageTaskStatus(snapshot.taskId, jobId, packageTaskId, snapshot.round, snapshot.inferenceDeviceIds));
        } catch (Exception e) {
            log.error("Task[{}] job[{}]: start package failed", taskId, jobId, e);
            onPackageFailed(jobId);
        }
    }

    /**
     * 监听 package-manager 任务状态。
     */
    private void trackPackageTaskStatus(
            Long taskId,
            Long jobId,
            String packageTaskId,
            Integer round,
            List<Long> inferenceDeviceIds
    ) {
        int maxRetries = 360; // 30分钟
        for (int i = 0; i < maxRetries; i++) {
            try {
                Msg<String> statusMsg = packageFeign.checkTaskStatus(packageTaskId);
                String status = statusMsg != null ? statusMsg.getPayload() : null;

                if ("COMPLETED".equalsIgnoreCase(status)) {
                    log.info("Task[{}] round[{}]: package completed, packageTaskId={}",
                            taskId, round, packageTaskId);
                    logBoundInferenceDevices(taskId, round, inferenceDeviceIds, "package-completed");
                    try {
                        onPackageComplete(jobId);
                    } catch (Exception e) {
                        log.error("Task[{}] round[{}]: onPackageComplete failed, but package is done, stop polling", taskId, round, e);
                    }
                    return;
                }

                if ("FAILED".equalsIgnoreCase(status)) {
                    Msg<Object> detailMsg = packageFeign.getTaskDetail(packageTaskId);
                    Object detail = detailMsg != null ? detailMsg.getPayload() : null;
                    log.error("Task[{}] round[{}]: package failed, packageTaskId={}, detail={}",
                            taskId, round, packageTaskId, detail);
                    // 自动重试，最多3次
                    transactionTemplate.executeWithoutResult(ts -> {
                        SelfIterationJob latestJob = requireJobForUpdate(jobId);
                        int retryCount = latestJob.getPackageRetryCount();
                        if (retryCount < 3) {
                            log.warn("Task[{}] round[{}]: package failed, retry {}/3", taskId, round, retryCount + 1);
                            latestJob.setPackageRetryCount(retryCount + 1);
                            latestJob.setPackageTaskId(null);
                            latestJob.setStatus(SelfIterationJob.STATUS_CONVERTING);
                            jobRepo.save(latestJob);
                        } else {
                            log.error("Task[{}] round[{}]: package failed after 3 retries", taskId, round);
                        }
                    });
                    SelfIterationJob checkJob = jobRepo.findById(jobId).orElse(null);
                    if (checkJob != null && checkJob.getStatus() == SelfIterationJob.STATUS_CONVERTING) {
                        // 重置为 CONVERTING 状态，重新触发打包
                        SelfIterationTask task = checkJob.getSelfIterationTask();
                        if (task != null) {
                            onConvertComplete(jobId, checkJob.getConvertJobId());
                        }
                    } else {
                        onPackageFailed(jobId);
                    }
                    return;
                }

                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                if (isPackageTaskStateUnavailable(e)) {
                    log.warn("Task[{}] round[{}]: package status unavailable after cleanup, complete via fallback without consuming packed file, packageTaskId={}",
                            taskId, round, packageTaskId);
                    logBoundInferenceDevices(taskId, round, inferenceDeviceIds, "cleanup-fallback");
                    try {
                        onPackageComplete(jobId);
                    } catch (Exception ex) {
                        log.error("Task[{}] round[{}]: onPackageComplete failed in fallback, but package is done, stop polling", taskId, round, ex);
                    }
                    return;
                }
                log.warn("Task[{}] round[{}]: check package status failed, packageTaskId={}, retry={}",
                        taskId, round, packageTaskId, i + 1, e);
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        log.error("Task[{}] round[{}]: package timeout, packageTaskId={}", taskId, round, packageTaskId);
        onPackageFailed(jobId);
    }

    /**
     * 详情轮询触发的兜底刷新：防止异步监听线程中断导致卡在打包中。
     */
    private void refreshPackageProgress(SelfIterationJob job) {
        if (job.getPackageTaskId() == null || job.getPackageTaskId().trim().isEmpty()) {
            return;
        }
        try {
            Msg<String> statusMsg = packageFeign.checkTaskStatus(job.getPackageTaskId());
            String status = statusMsg != null ? statusMsg.getPayload() : null;
            if ("COMPLETED".equalsIgnoreCase(status)) {
                log.info("Task[{}] round[{}]: package completed by refresh, packageTaskId={}",
                        job.getSelfIterationTask() != null ? job.getSelfIterationTask().getId() : null,
                        job.getRound(),
                        job.getPackageTaskId());
                logBoundInferenceDevices(
                        job.getSelfIterationTask() != null ? job.getSelfIterationTask().getId() : null,
                        job.getRound(),
                        job.getSelfIterationTask() != null ? job.getSelfIterationTask().getInferenceDeviceIds() : null,
                        "refresh"
                );
                onPackageComplete(job.getId());
            } else if ("FAILED".equalsIgnoreCase(status)) {
                onPackageFailed(job.getId());
            }
        } catch (Exception e) {
            if (isPackageTaskStateUnavailable(e)) {
                log.warn("Task[{}] round[{}]: package status unavailable in refresh after cleanup, complete via fallback without consuming packed file, packageTaskId={}",
                        job.getSelfIterationTask() != null ? job.getSelfIterationTask().getId() : null,
                        job.getRound(),
                        job.getPackageTaskId());
                logBoundInferenceDevices(
                        job.getSelfIterationTask() != null ? job.getSelfIterationTask().getId() : null,
                        job.getRound(),
                        job.getSelfIterationTask() != null ? job.getSelfIterationTask().getInferenceDeviceIds() : null,
                        "refresh-cleanup-fallback"
                );
                onPackageComplete(job.getId());
                return;
            }
            log.warn("Task[{}] round[{}]: refresh package progress failed, packageTaskId={}",
                    job.getSelfIterationTask() != null ? job.getSelfIterationTask().getId() : null,
                    job.getRound(),
                    job.getPackageTaskId(),
                    e);
        }
    }

    private PackageStartSnapshot buildPackageStartSnapshot(Long taskId, Long jobId) {
        SelfIterationJob job = requireJobForUpdate(jobId);
        SelfIterationTask task = requireTask(taskId);

        if (job.getConvertJobId() == null) {
            return null;
        }
        ModelJob convertJob = modelJobRepo.findModelJobById(job.getConvertJobId());
        if (convertJob == null) {
            return null;
        }

        String appZipPath = task.getModelApplication() != null ? task.getModelApplication().getAppZipPath() : null;
        if (appZipPath == null || appZipPath.trim().isEmpty()) {
            log.error("Task[{}] round[{}]: appZipPath is empty, cannot package", taskId, job.getRound());
            return null;
        }

        String modelRelativePath = resolvePackModelRelativePath(convertJob);
        if (modelRelativePath == null) {
            log.error("Task[{}] round[{}]: no converted model file found for convert job {}",
                    taskId, job.getRound(), convertJob.getId());
            return null;
        }

        String localModelPath = datasetRootPath + "/" + weightPath + "/" + modelRelativePath;
        File modelFile = new File(localModelPath);
        if (!modelFile.exists() || !modelFile.isFile()) {
            log.error("Task[{}] round[{}]: converted model file not exists, path={}",
                    taskId, job.getRound(), localModelPath);
            return null;
        }

        Map<Integer, String> selectedLabels = convertJob.getSelectedLabels() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(convertJob.getSelectedLabels());
        if (selectedLabels.isEmpty() && task.getSelectedLabels() != null) {
            selectedLabels.putAll(task.getSelectedLabels());
        }

        PackageStartSnapshot snapshot = new PackageStartSnapshot();
        snapshot.taskId = taskId;
        snapshot.round = job.getRound();
        snapshot.appZipPath = appZipPath;
        snapshot.convertJobName = convertJob.getName();
        snapshot.localModelPath = localModelPath;
        snapshot.localFileName = modelFile.getName();
        snapshot.selectedLabels = selectedLabels;
        snapshot.authCode = task.getAuthCode();
        snapshot.inferenceDeviceIds = task.getInferenceDeviceIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(task.getInferenceDeviceIds());
        return snapshot;
    }

    private SelfIterationGpuDispatchService.DispatchResult dispatchPackageForJob(SelfIterationJob job) {
        if (job == null) {
            throw new IllegalArgumentException("自迭代子任务不能为空");
        }
        SelfIterationTask task = job.getSelfIterationTask();
        if (task == null) {
            throw new IllegalArgumentException("自迭代父任务不能为空");
        }
        if (job.getPackageTaskId() == null || job.getPackageTaskId().trim().isEmpty()) {
            throw new IllegalArgumentException("packageTaskId 不能为空");
        }
        byte[] packedBytes;
        try {
            packedBytes = packageFeign.getPackedFile(job.getPackageTaskId());
        } catch (Exception e) {
            log.warn("Task[{}] round[{}]: packed file unavailable, packageTaskId={}",
                    task.getId(), job.getRound(), job.getPackageTaskId(), e);
            throw new IllegalStateException("打包文件已不存在或已被读取，请重新打包后再下发");
        }
        if (packedBytes == null || packedBytes.length == 0) {
            log.warn("Task[{}] round[{}]: packed file is empty, packageTaskId={}",
                    task.getId(), job.getRound(), job.getPackageTaskId());
            throw new IllegalStateException("打包文件为空或已被删除，请重新打包后再下发");
        }
        List<GpuUrlTarget> targets = gpuUrlTargetService.findAvailableByIds(task.getGpuUrlTargetIds());
        SelfIterationGpuDispatchService.DispatchResult result = gpuDispatchService.dispatchToTargets(packedBytes, targets);
        log.info("Task[{}] round[{}]: manual/auto GPU dispatch result success={}, targetCount={}, bytes={}",
                task.getId(), job.getRound(), result.isSuccess(), result.getTargetCount(), result.getBinaryBytes());
        return result;
    }

    private Map<String, Object> buildDispatchPreviewTarget(GpuUrlTarget target) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", target.getId());
        item.put("name", target.getName());
        item.put("gpuUrl", target.getGpuUrl());
        item.put("platform", target.getPlatformIp() + ":" + target.getPlatformPort());
        item.put("uploadUrl", "http://" + target.getPlatformIp() + ":" + target.getPlatformPort() + "/api/sys-ai-dev/uploadAppZip");
        return item;
    }

    private void logBoundInferenceDevices(Long taskId, Integer round, List<Long> inferenceDeviceIds, String source) {
        try {
            List<Long> safeIds = inferenceDeviceIds == null
                    ? new ArrayList<>()
                    : inferenceDeviceIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (safeIds.isEmpty()) {
                log.info("Task[{}] round[{}]: no bound inference devices ({})", taskId, round, source);
                return;
            }

            DataResponseBody<Page<DubheDataFeign.InferenceDeviceVO>> resp = dubheDataFeign.pageInferenceDevices(
                    dubheUtils.getAuthorization(),
                    1L,
                    1000L,
                    null
            );
            if (resp == null || !resp.succeed() || resp.getData() == null || resp.getData().getRecords() == null) {
                log.warn("Task[{}] round[{}]: query inference devices failed ({})", taskId, round, source);
                return;
            }
            Set<Long> idSet = new LinkedHashSet<>(safeIds);
            List<DubheDataFeign.InferenceDeviceVO> bound = resp.getData().getRecords().stream()
                    .filter(Objects::nonNull)
                    .filter(d -> d.getId() != null && idSet.contains(d.getId()))
                    .collect(Collectors.toList());
            log.info("Task[{}] round[{}]: bound inference devices ({}) -> {}", taskId, round, source, bound);
        } catch (Exception e) {
            log.warn("Task[{}] round[{}]: log bound inference devices failed ({})", taskId, round, source, e);
        }
    }

    private String resolvePackModelRelativePath(ModelJob convertJob) {
        if (convertJob == null || convertJob.getWeightPath() == null || convertJob.getWeightPath().trim().isEmpty()) {
            return null;
        }

        String trainJobName = extractTrainJobNameFromConvertName(convertJob.getName());
        log.info("resolvePackModelRelativePath: convertJobId={}, convertJobName={}, weightPath={}, trainJobName={}",
                convertJob.getId(), convertJob.getName(), convertJob.getWeightPath(), trainJobName);
        List<String> candidateRelativeDirs = new ArrayList<>();
        // 场景1：输出直接在 weight/<convertJobName>/
        candidateRelativeDirs.add(convertJob.getWeightPath());

        // 场景2：输出在 weight/<trainJobName>/<convertJobName>/（当前 convert/create 挂载模式）
        if (trainJobName != null && !trainJobName.trim().isEmpty()) {
            candidateRelativeDirs.add(trainJobName + "/" + convertJob.getWeightPath());

            // 场景3：目录名称包含动态时间戳，按前缀匹配 trainJobName-convert-
            File trainDir = new File(datasetRootPath + "/" + weightPath + "/" + trainJobName);
            File[] convertDirs = trainDir.listFiles(file -> file.isDirectory() && file.getName().startsWith(trainJobName + "-convert-"));
            if (convertDirs != null && convertDirs.length > 0) {
                Arrays.sort(convertDirs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                for (File dir : convertDirs) {
                    candidateRelativeDirs.add(trainJobName + "/" + dir.getName());
                }
            }

            // 场景4：本地调试兜底，转换目录名不稳定时，允许在 train 根目录下递归匹配可用转换产物
            candidateRelativeDirs.add(trainJobName);
        }

        List<String> triedDirs = new ArrayList<>();
        for (String relativeDir : candidateRelativeDirs) {
            File dir = new File(datasetRootPath + "/" + weightPath + "/" + relativeDir);
            triedDirs.add(dir.getAbsolutePath());
            if (!dir.isDirectory()) {
                continue;
            }

            String matchedPath = findModelArtifactRecursively(dir, relativeDir);
            if (matchedPath != null) {
                return matchedPath;
            }
        }

        log.warn("No converted model artifact found for convertJob={}, triedDirs={}",
                convertJob.getId(), triedDirs);
        return null;
    }

    private String findModelArtifactRecursively(File dir, String relativeDir) {
        try {
            List<File> files = java.nio.file.Files.walk(dir.toPath())
                    .filter(java.nio.file.Files::isRegularFile)
                    .map(java.nio.file.Path::toFile)
                    .sorted(Comparator.comparing(File::lastModified).reversed())
                    .collect(Collectors.toList());

            for (File file : files) {
                String name = file.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".rknn") || name.endsWith(".engine") || name.endsWith(".bmodel") || name.endsWith(".onnx")) {
                    String tail = dir.toPath().relativize(file.toPath()).toString().replace("\\", "/");
                    return relativeDir + "/" + tail;
                }
            }

            // 兜底：任意普通文件
            if (!files.isEmpty()) {
                String tail = dir.toPath().relativize(files.get(0).toPath()).toString().replace("\\", "/");
                return relativeDir + "/" + tail;
            }
        } catch (Exception ignored) {
        }
        return null;
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

    private String generateClassesTxtFromMap(Map<Integer, String> selectedLabels) {
        if (selectedLabels == null || selectedLabels.isEmpty()) {
            return "";
        }
        StringBuilder txtBuilder = new StringBuilder();
        selectedLabels.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> txtBuilder.append(entry.getValue()).append("\n"));
        return txtBuilder.toString();
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> targetType) {
        Throwable current = throwable;
        while (current != null) {
            if (targetType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * package-manager 任务已完成并被清理后，checkTaskStatus 可能返回 500/404。
     * 这类错误在当前场景视为“状态不可再查询”，上层走完成兜底，避免无限轮询刷错。
     */
    private boolean isPackageTaskStateUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof FeignException) {
                FeignException fe = (FeignException) current;
                if (fe.status() == 404 || fe.status() == 500) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 暂停自迭代任务：RUNNING → PAUSED。
     * 轻量级暂停，不停止后台任务，定时任务和 completeRound 会跳过已暂停的任务。
     */
    @Transactional
    public void pauseTask(Long taskId) {
        SelfIterationTask task = requireTask(taskId);
        if (task.getStatus() != SelfIterationTask.STATUS_RUNNING) {
            throw new IllegalStateException("只能暂停运行中的任务，当前状态：" + task.getStatus());
        }
        SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
        taskSm.taskPauseForReviewEvent(taskId);
        task.setStatus(SelfIterationTask.STATUS_PAUSED);
        taskRepo.save(task);
        log.info("任务[{}]已暂停", taskId);
    }

    /**
     * 恢复自迭代任务：PAUSED → RUNNING，并触发一次状态推进。
     */
    @Transactional
    public void resumeTask(Long taskId) {
        SelfIterationTask task = requireTask(taskId);
        if (task.getStatus() != SelfIterationTask.STATUS_PAUSED) {
            throw new IllegalStateException("只能恢复已暂停的任务，当前状态：" + task.getStatus());
        }
        SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
        taskSm.taskResumeEvent(taskId);
        task.setStatus(SelfIterationTask.STATUS_RUNNING);
        taskRepo.save(task);
        log.info("任务[{}]已恢复", taskId);

        // 找到当前活跃子任务，触发一次推进（兜底暂停期间已完成的阶段）
        List<SelfIterationJob> activeJobs = jobRepo.findBySelfIterationTask_IdOrderByRoundAsc(taskId)
                .stream()
                .filter(j -> j.getStatus() == SelfIterationJob.STATUS_COLLECTING
                        || j.getStatus() == SelfIterationJob.STATUS_AUTO_LABELING
                        || j.getStatus() == SelfIterationJob.STATUS_TRAINING
                        || j.getStatus() == SelfIterationJob.STATUS_CONVERTING
                        || j.getStatus() == SelfIterationJob.STATUS_PACKAGING)
                .collect(Collectors.toList());

        if (!activeJobs.isEmpty()) {
            refreshModelIterationProgress(activeJobs.get(0).getId());
        }
    }

    private static class PackageStartSnapshot {
        Long taskId;
        Integer round;
        String appZipPath;
        String convertJobName;
        String localModelPath;
        String localFileName;
        Map<Integer, String> selectedLabels;
        String authCode;
        List<Long> inferenceDeviceIds;
    }

    private static class PackagePhaseTrigger {
        final Long taskId;
        final Long jobId;

        PackagePhaseTrigger(Long taskId, Long jobId) {
            this.taskId = taskId;
            this.jobId = jobId;
        }
    }

    /**
     * 本轮完成：更新轮次计数，决定是否进入下一轮或结束父任务。
     */
    public void completeRound(SelfIterationTask task, SelfIterationJob job) {
        // 检查父任务是否已暂停/取消，若是则不开启下一轮
        SelfIterationTask currentTask = taskRepo.findById(task.getId()).orElse(null);
        if (currentTask == null) {
            log.warn("任务[{}]不存在，忽略 completeRound", task.getId());
            return;
        }
        if (currentTask.getStatus() == SelfIterationTask.STATUS_PAUSED) {
            log.info("任务[{}]已暂停，第{}轮完成后不启动下一轮", task.getId(), job.getRound());
            transactionTemplate.executeWithoutResult(s -> {
                SelfIterationJob freshJob = requireJobForUpdate(job.getId());
                if (freshJob.getStatus() != SelfIterationJob.STATUS_COMPLETED) {
                    SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
                    jobSm.jobCompleteEvent(freshJob.getId());
                    freshJob.setStatus(SelfIterationJob.STATUS_COMPLETED);
                    jobRepo.save(freshJob);
                }
            });
            return;
        }
        if (currentTask.getStatus() == SelfIterationTask.STATUS_CANCELLED) {
            log.info("任务[{}]已取消，忽略 completeRound", task.getId());
            return;
        }

        SelfIterationJob[] nextJobHolder = new SelfIterationJob[1];
        SelfIterationTask[] nextTaskHolder = new SelfIterationTask[1];
        boolean[] shouldStartNext = {false};
        boolean[] taskCompleted = {false};

        transactionTemplate.execute(status -> {
            SelfIterationJob freshJob = requireJobForUpdate(job.getId());
            if (freshJob.getStatus() == SelfIterationJob.STATUS_COMPLETED) {
                log.info("任务[{}]第{}轮：completeRound 已执行，忽略重复调用", task.getId(), job.getRound());
                return null;
            }

            SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
            jobSm.jobCompleteEvent(freshJob.getId());
            freshJob.setStatus(SelfIterationJob.STATUS_COMPLETED);
            jobRepo.save(freshJob);

            SelfIterationTask freshTask = taskRepo.findById(task.getId())
                    .orElseThrow(() -> new IllegalArgumentException("任务不存在：id=" + task.getId()));

            if (freshTask.getCurrentRound() > job.getRound()) {
                log.info("任务[{}]：currentRound={} 已超过本轮 {}，忽略重复 completeRound",
                        freshTask.getId(), freshTask.getCurrentRound(), job.getRound());
                return null;
            }

            int nextRound = job.getRound() + 1;
            boolean nextJobExists = jobRepo.findBySelfIterationTask_IdAndRound(freshTask.getId(), nextRound).isPresent();
            if (nextJobExists) {
                log.info("任务[{}]：第{}轮已存在，忽略重复 completeRound", freshTask.getId(), nextRound);
                return null;
            }

            freshTask.setCurrentRound(freshTask.getCurrentRound() + 1);

            if (isMetricStopReached(freshTask, freshJob)) {
                freshTask.setMaxRounds(freshJob.getRound());
                log.info("任务[{}]第{}轮：达到指标截止条件，accuracy={}, recall={}, targetAccuracy={}, targetRecall={}",
                        freshTask.getId(), freshJob.getRound(),
                        freshJob.getTrainAccuracy(), freshJob.getTrainRecall(),
                        freshTask.getTargetAccuracy(), freshTask.getTargetRecall());
            }

            if (freshTask.getMaxRounds() != null && freshTask.getCurrentRound() >= freshTask.getMaxRounds()) {
                SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
                taskSm.taskCompleteEvent(freshTask.getId());
                freshTask.setStatus(SelfIterationTask.STATUS_COMPLETED);
                taskRepo.save(freshTask);
                log.info("任务[{}]：达到最大轮次 {}，任务完成", freshTask.getId(), freshTask.getMaxRounds());
                taskCompleted[0] = true;
            } else {
                taskRepo.save(freshTask);
                SelfIterationJob nextJob = createNewJob(freshTask, nextRound);
                log.info("任务[{}]：第{}轮完成，创建第{}轮", freshTask.getId(), job.getRound(), nextRound);
                nextJobHolder[0] = nextJob;
                nextTaskHolder[0] = taskRepo.findById(freshTask.getId()).orElse(freshTask);
                shouldStartNext[0] = true;
            }
            return null;
        });

        if (shouldStartNext[0]) {
            startCollectPhase(nextTaskHolder[0], nextJobHolder[0]);
        }
    }

    private boolean isMetricStopReached(SelfIterationTask task, SelfIterationJob job) {
        return task.getTargetAccuracy() != null
                && task.getTargetRecall() != null
                && job.getTrainAccuracy() != null
                && job.getTrainRecall() != null
                && job.getTrainAccuracy().compareTo(task.getTargetAccuracy()) >= 0
                && job.getTrainRecall().compareTo(task.getTargetRecall()) >= 0;
    }

    /**
     * 获取置信度阈值，兼容不同版本的 SelfIterationTask 实体。
     */
    private Double getConfidenceThreshold(SelfIterationTask task) {
        try {
            Object value = task.getClass().getMethod("getConfidenceThreshold").invoke(task);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Compatible with old mineai-core entities
        }
        return 0.25;
    }


    /**
     * 为 job 中所有已完成标注的 dataset 发布版本。
     * 返回 null 表示有发版失败，已触发失败流程。
     */
    private List<Long> publishDatasetVersions(SelfIterationJob job, SelfIterationTask task) {
        // 在事务内加载 job 并提前读取懒加载字段，避免事务外访问失败
        List<Long> executionIds = transactionTemplate.execute(status -> {
            SelfIterationJob freshJob = jobRepo.findById(job.getId()).orElse(null);
            if (freshJob == null) {
                return null;
            }
            List<Long> ids = freshJob.getCaptureExecutionIds();
            return ids == null ? new ArrayList<>() : new ArrayList<>(ids);
        });
        if (executionIds == null) {
            log.error("任务[{}]第{}轮：job 不存在，无法发布版本", task.getId(), job.getRound());
            return null;
        }

        // 从 captureExecutionIds 出发获取 datasetId，避免并发回调时 annotated_datasets 表未完全写入
        if (executionIds.isEmpty()) {
            log.error("任务[{}]第{}轮：captureExecutionIds 为空，无法发布版本", task.getId(), job.getRound());
            onAutoLabelFailed(job.getId(), -1L);
            return null;
        }

        List<Long> datasetIds = new ArrayList<>();
        Set<Long> datasetIdSet = new LinkedHashSet<>();
        for (Long executionId : executionIds) {
            RtspCaptureExecutionVO execution = getExecutionById(executionId);
            if (execution != null && execution.getDatasetId() != null && datasetIdSet.add(execution.getDatasetId())) {
                datasetIds.add(execution.getDatasetId());
            }
        }

        if (datasetIds.isEmpty()) {
            log.error("任务[{}]第{}轮：所有 execution 均无 datasetId，无法发布版本", task.getId(), job.getRound());
            onAutoLabelFailed(job.getId(), -1L);
            return null;
        }

        log.info("任务[{}]第{}轮：开始发布版本，共{}个数据集需要发布，数据集id: {}", task.getId(), job.getRound(), datasetIds.size(), datasetIds);
        List<Long> newVersionIds = new ArrayList<>();
        int index = 0;
        for (Long datasetId : datasetIds) {
            index++;
            log.info("任务[{}]第{}轮：发布数据集版本 [{}/{}]，datasetId={}",
                    task.getId(), job.getRound(), index, datasetIds.size(), datasetId);
            Long versionId = publishDatasetVersion(datasetId, task);
            if (versionId == null) {
                log.error("任务[{}]第{}轮：datasetId={} 发版失败，触发标注失败流程", task.getId(), job.getRound(), datasetId);
                onAutoLabelFailed(job.getId(), datasetId);
                return null;
            }
            log.info("任务[{}]第{}轮：数据集版本 [{}/{}] 发布成功，versionId={}",
                    task.getId(), job.getRound(), index, datasetIds.size(), versionId);
            newVersionIds.add(versionId);
        }
        return newVersionIds;
    }

    /**
     * 将本轮发版结果写入 job.datasetVersionIds 与 task.allDatasetVersionIds。
     */
    private void bindPublishedVersionsToTaskAndJob(Long taskId, Long jobId, List<Long> newVersionIds) {
        if (newVersionIds == null || newVersionIds.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            SelfIterationJob jobToUpdate = requireJobForUpdate(jobId);
            List<Long> existedJobVersionIds = jobToUpdate.getDatasetVersionIds() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(jobToUpdate.getDatasetVersionIds());
            java.util.LinkedHashSet<Long> jobVersionIdSet = new java.util.LinkedHashSet<>(existedJobVersionIds);
            jobVersionIdSet.addAll(newVersionIds);
            jobToUpdate.setDatasetVersionIds(new ArrayList<>(jobVersionIdSet));
            jobRepo.save(jobToUpdate);

            SelfIterationTask taskToUpdate = taskRepo.findById(taskId).orElse(null);
            if (taskToUpdate != null) {
                List<Long> allVersionIds = taskToUpdate.getAllDatasetVersionIds() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(taskToUpdate.getAllDatasetVersionIds());
                java.util.LinkedHashSet<Long> allVersionIdSet = new java.util.LinkedHashSet<>(allVersionIds);
                allVersionIdSet.addAll(newVersionIds);
                taskToUpdate.setAllDatasetVersionIds(new ArrayList<>(allVersionIdSet));
                taskRepo.save(taskToUpdate);
            }
        });
    }

    /**
     * 校验发布版本是否包含可训练标签（宽松模式）。
     * 规则：只要本轮任意一个版本 labelsCount 总和 > 0 即可继续训练。
     */
    private boolean versionsHaveAnyLabels(List<Long> versionIds, SelfIterationTask task, SelfIterationJob job) {
        if (versionIds == null || versionIds.isEmpty()) {
            log.warn("任务[{}]第{}轮：本轮无可校验版本，判定为标签不足", task.getId(), job.getRound());
            return false;
        }

        String authorization = dubheUtils.getAuthorization();
        Map<Long, Map<String, Integer>> versionLabelCountMap = new HashMap<>();
        try {
            DataResponseBody<List<DatasetVersionDetailVO>> batchResp = dubheDataFeign.batchGetVersionsLabelCountMap(
                    authorization,
                    versionIds,
                    Collections.emptyList()
            );
            if (batchResp != null && batchResp.succeed() && batchResp.getData() != null) {
                for (DatasetVersionDetailVO detailVO : batchResp.getData()) {
                    if (detailVO == null || detailVO.getId() == null) {
                        continue;
                    }
                    versionLabelCountMap.put(detailVO.getId(), detailVO.getLabelCountMap());
                }
            } else {
                log.warn("任务[{}]第{}轮：批量查询版本标签统计失败，按无标签处理，versionIds={}",
                        task.getId(), job.getRound(), versionIds);
            }
        } catch (Exception ex) {
            log.warn("任务[{}]第{}轮：批量查询版本标签统计异常，按无标签处理，versionIds={}",
                    task.getId(), job.getRound(), versionIds, ex);
        }

        boolean hasTrainableLabels = false;
        for (Long versionId : versionIds) {
            try {
                Map<String, Integer> labelCountMap = versionLabelCountMap.get(versionId);
                long totalLabels = 0L;
                if (labelCountMap != null && !labelCountMap.isEmpty()) {
                    for (Integer count : labelCountMap.values()) {
                        if (count != null && count > 0) {
                            totalLabels += count;
                        }
                    }
                }

                if (totalLabels > 0) {
                    hasTrainableLabels = true;
                    log.info("任务[{}]第{}轮：版本{}标签校验通过，总标签数={}",
                            task.getId(), job.getRound(), versionId, totalLabels);
                } else {
                    log.warn("任务[{}]第{}轮：版本{}标签总数为0，labelCountMap={}",
                            task.getId(), job.getRound(), versionId, labelCountMap);
                }
            } catch (Exception ex) {
                log.warn("任务[{}]第{}轮：校验版本{}标签统计异常，按无标签处理",
                        task.getId(), job.getRound(), versionId, ex);
            }
        }

        if (!hasTrainableLabels) {
            log.warn("任务[{}]第{}轮：本轮所有版本标签总数均为0（或无法读取），需人工审核", task.getId(), job.getRound());
        }
        return hasTrainableLabels;
    }

    /**
     * 将任务切换到人工审核等待态（job=WAITING_REVIEW，task=PAUSED）。
     */
    private void transitionToManualReview(Long taskId, Long jobId, String reason) {
        transactionTemplate.executeWithoutResult(status -> {
            SelfIterationJob freshJob = requireJobForUpdate(jobId);
            SelfIterationTask freshTask = taskRepo.findById(taskId).orElse(null);
            if (freshTask == null) {
                return;
            }

            if (freshJob.getStatus() == SelfIterationJob.STATUS_AUTO_LABELING) {
                SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
                jobSm.jobLabelNeedReviewEvent(freshJob.getId());
                freshJob.setStatus(SelfIterationJob.STATUS_WAITING_REVIEW);
                jobRepo.save(freshJob);
            }

            if (freshTask.getStatus() == SelfIterationTask.STATUS_RUNNING) {
                SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
                taskSm.taskPauseForReviewEvent(freshTask.getId());
                freshTask.setStatus(SelfIterationTask.STATUS_PAUSED);
                taskRepo.save(freshTask);
            }

            log.info("任务[{}]第{}轮：切换到人工审核等待态，reason={}", freshTask.getId(), freshJob.getRound(), reason);
        });
    }

    /**
     * 将 job 从 AUTO_LABELING/WAITING_REVIEW 推进到 TRAINING；若 task 当前 PAUSED 同步恢复 RUNNING。
     */
    private boolean promoteToTraining(Long taskId, Long jobId) {
        Boolean promoted = transactionTemplate.execute(status -> {
            SelfIterationJob freshJob = requireJobForUpdate(jobId);
            SelfIterationTask freshTask = taskRepo.findById(taskId).orElse(null);
            if (freshTask == null) {
                return false;
            }

            if (freshJob.getStatus() == SelfIterationJob.STATUS_TRAINING
                    || freshJob.getStatus() == SelfIterationJob.STATUS_CONVERTING
                    || freshJob.getStatus() == SelfIterationJob.STATUS_PACKAGING
                    || freshJob.getStatus() == SelfIterationJob.STATUS_PENDING_DEPLOY
                    || freshJob.getStatus() == SelfIterationJob.STATUS_COMPLETED) {
                return false;
            }

            if (freshJob.getStatus() != SelfIterationJob.STATUS_AUTO_LABELING
                    && freshJob.getStatus() != SelfIterationJob.STATUS_WAITING_REVIEW) {
                log.warn("任务[{}]第{}轮：当前状态{}不可推进到训练", taskId, freshJob.getRound(), freshJob.getStatus());
                return false;
            }

            SelfIterationJobStateMachine jobSm = SpringContextHolder.getBean("selfIterationJobStateMachine");
            if (freshJob.getStatus() == SelfIterationJob.STATUS_WAITING_REVIEW) {
                jobSm.setMemoryState(SpringContextHolder.getBean("jobWaitingReviewState"));
            } else {
                jobSm.setMemoryState(SpringContextHolder.getBean("jobAutoLabelingState"));
            }
            jobSm.jobLabelDoneEvent(freshJob.getId());
            freshJob.setStatus(SelfIterationJob.STATUS_TRAINING);
            freshJob.setPhase(SelfIterationJob.PHASE_MODEL_TRAIN);
            jobRepo.save(freshJob);

            if (freshTask.getStatus() == SelfIterationTask.STATUS_PAUSED) {
                SelfIterationTaskStateMachine taskSm = SpringContextHolder.getBean("selfIterationTaskStateMachine");
                taskSm.taskResumeEvent(freshTask.getId());
                freshTask.setStatus(SelfIterationTask.STATUS_RUNNING);
                taskRepo.save(freshTask);
            }

            return true;
        });
        return Boolean.TRUE.equals(promoted);
    }

    private Long publishDatasetVersion(Long datasetId, SelfIterationTask task) {
        try {
            DatasetVersionCreateDTO dto = new DatasetVersionCreateDTO();
            dto.setDatasetId(datasetId);
            dto.setFormat("YOLO");
            dto.setOfRecord(0);

            // 查询数据集的标签信息，构建 labelMappings
            List<LabelMappingDTO> labelMappings = new ArrayList<>();
            try {
                DataResponseBody<GuidedAndLabelsVO> labelsResp = dubheDataFeign.guidedAndLabels(
                        dubheUtils.getAuthorization(), datasetId);
                if (labelsResp != null && labelsResp.succeed() && labelsResp.getData() != null) {
                    List<LabelDTO> labels = labelsResp.getData().getLabels();
                    if (labels != null && !labels.isEmpty()) {
                        for (LabelDTO label : labels) {
                            LabelMappingDTO mapping = new LabelMappingDTO();
                            mapping.setSourceLabelId(label.getId());
                            mapping.setTargetLabelId(label.getId());
                            labelMappings.add(mapping);
                        }
                        log.info("任务[{}]：datasetId={} 发布版本时设置标签映射，共{}个标签",
                                task.getId(), datasetId, labelMappings.size());
                    } else {
                        log.warn("任务[{}]：datasetId={} 查询到的标签列表为空", task.getId(), datasetId);
                    }
                } else {
                    log.warn("任务[{}]：datasetId={} 查询标签信息失败", task.getId(), datasetId);
                }
            } catch (Exception e) {
                log.error("任务[{}]：datasetId={} 查询标签信息异常", task.getId(), datasetId, e);
            }
            dto.setLabelMappings(labelMappings);

            DataResponseBody<Long> resp = dubheDataFeign.publish(dubheUtils.getAuthorization(), dto);
            if (resp == null || !resp.succeed()) {
                log.error("发版失败：datasetId={}，response={}", datasetId, resp);
                return null;
            }
            Long versionId = resp.getData();
            // 轮询等待发布完成（dataConversion=1 表示发布完成）
            // 每5秒查一次，最多等1.5小时 = 5400秒 / 5 = 1080次
            int maxRetries = 1080;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                try {
                    DataResponseBody<DatasetVersionVO> versionResp =
                            dubheDataFeign.selectDatasetVersionById(dubheUtils.getAuthorization(), versionId);
                    if (versionResp != null && versionResp.succeed() && versionResp.getData() != null) {
                        Integer dataConversion = versionResp.getData().getDataConversion();
                        if (Integer.valueOf(1).equals(dataConversion)) {
                            log.info("数据集版本{}发布完成，dataConversion={}", versionId, dataConversion);
                            return versionId;
                        }
                        log.info("等待数据集版本{}发布完成，当前dataConversion={}，已等待{}次", versionId, dataConversion, i + 1);
                    }
                } catch (Exception e) {
                    log.warn("查询数据集版本{}状态异常，继续等待", versionId, e);
                }
            }
            log.error("数据集版本{}发布超时（{}次轮询），datasetId={}", versionId, maxRetries, datasetId);
            return null;
        } catch (Exception e) {
            log.error("发版异常：datasetId={}", datasetId, e);
        }
        return null;
    }

    private Long resolveLatestExecutionId(Long captureTaskId) {
        String authorization = dubheUtils.getAuthorization();
        for (int i = 0; i < 10; i++) {
            try {
                DataResponseBody<Page<RtspCaptureExecutionVO>> pageResp = dubheDataFeign.pageCaptureExecutions(
                        authorization, 1L, 1L, captureTaskId, null);
                if (pageResp != null && pageResp.succeed() && pageResp.getData() != null
                        && pageResp.getData().getRecords() != null && !pageResp.getData().getRecords().isEmpty()) {
                    RtspCaptureExecutionVO latest = pageResp.getData().getRecords().get(0);
                    if (latest != null && latest.getId() != null) {
                        return latest.getId();
                    }
                }
            } catch (Exception e) {
                log.warn("查询最新采集执行记录失败：captureTaskId={}, retry={}", captureTaskId, i + 1, e);
            }

            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return null;
    }

    /**
     * 获取上一轮训练 Job 的权重路径，用于第 N 轮（N>1）的 finetune 训练和自动标注。
     */
    private String getPrevWeightPath(SelfIterationTask task, int currentRound) {
        Optional<SelfIterationJob> prevJobOpt = jobRepo.findBySelfIterationTask_IdAndRound(task.getId(), currentRound - 1);
        if (!prevJobOpt.isPresent() || prevJobOpt.get().getTrainJobId() == null) {
            return null;
        }
        ModelJob prevTrainJob = modelJobRepo.findModelJobById(prevJobOpt.get().getTrainJobId());
        return prevTrainJob != null ? prevTrainJob.getWeightPath() : null;
    }

    /**
     * 第二轮及以后，在复用上一轮模型时，必须使用设备固件绑定的训练镜像，
     * 并要求该训练镜像本身支持自动标注。这与现有自动标注按钮逻辑保持一致。
     */
    private String resolveAutoLabelImageUrl(SelfIterationTask task, int currentRound, boolean isFirstRound, String strategy) {
        String fallback = task.getAutoLabelImageUrl();
        if (isFirstRound) {
            return fallback;
        }
        if (!"ALWAYS".equals(strategy) && !"METRIC_COMPARE".equals(strategy)) {
            return fallback;
        }

        try {
            ModelApplication modelApplication = task.getModelApplication();
            if (modelApplication == null || modelApplication.getId() == null) {
                log.error("任务[{}]第{}轮：复用上一轮模型时未关联 ModelApplication，无法解析绑定自动标注镜像",
                        task.getId(), currentRound);
                return null;
            }
            ModelApplicationRepo modelApplicationRepo = SpringContextHolder.getBean(ModelApplicationRepo.class);
            ModelApplication freshApp = modelApplicationRepo.findById(modelApplication.getId()).orElse(null);
            if (freshApp == null || freshApp.getModel() == null
                    || freshApp.getModel().getTrainModelVersion() == null) {
                log.error("Task[{}] round[{}]: no bound train model version found for reused auto-label image selection",
                        task.getId(), currentRound);
                return null;
            }
            ModelVersion boundTrainModelVersion = freshApp.getModel().getTrainModelVersion();
            if (!boundTrainModelVersion.isAutoLabel()) {
                log.error("Task[{}] round[{}]: bound train model version does not support auto-label, trainModelVersionId={}, url={}",
                        task.getId(), currentRound, boundTrainModelVersion.getId(), boundTrainModelVersion.getUrl());
                return null;
            }
            String boundImageUrl = boundTrainModelVersion.getUrl();
            if (boundImageUrl == null || boundImageUrl.trim().isEmpty()) {
                log.error("Task[{}] round[{}]: bound train model version url is empty, trainModelVersionId={}",
                        task.getId(), currentRound, boundTrainModelVersion.getId());
                return null;
            }
            log.info("Task[{}] round[{}]: using bound train model image for auto-label, trainModelVersionId={}, imageUrl={}",
                    task.getId(), currentRound, boundTrainModelVersion.getId(), boundImageUrl);
            log.info("Task[{}] round[{}]: task cached autoLabelImageUrl='{}' is ignored for reused auto-label",
                    task.getId(), currentRound, fallback);
            return boundImageUrl;
        } catch (Exception e) {
            log.error("Task[{}] round[{}]: failed to resolve bound train model image for reused auto-label",
                    task.getId(), currentRound, e);
            return null;
        }
    }

    /**
     * 基于指标对比决定是否复用上一轮模型
     */
    private boolean shouldReuseBasedOnMetrics(SelfIterationTask task, int currentRound) {
        Optional<SelfIterationJob> prevJobOpt = jobRepo.findBySelfIterationTask_IdAndRound(task.getId(), currentRound - 1);
        if (!prevJobOpt.isPresent()) {
            return false;
        }
        SelfIterationJob prevJob = prevJobOpt.get();
        if (prevJob.getTrainAccuracy() == null || prevJob.getTrainRecall() == null) {
            return false;
        }
        return true;
    }

    /**
     * 第二轮及以后，在复用上一轮模型时，沿用“自动标注按钮”的镜像选择逻辑：
     * ModelGeneration -> modelExplore -> trainModelVersion。
     */
    private String resolveAutoLabelImageUrlFromGeneration(SelfIterationTask task, int currentRound, boolean isFirstRound, String strategy) {
        String fallback = task.getAutoLabelImageUrl();
        if (isFirstRound) {
            return fallback;
        }
        if (!"ALWAYS".equals(strategy) && !"METRIC_COMPARE".equals(strategy)) {
            return fallback;
        }

        try {
            ModelGeneration modelGeneration = getOrCreateModelGeneration(task);
            if (modelGeneration == null || modelGeneration.getModelExplore() == null
                    || modelGeneration.getModelExplore().getTrainModelVersion() == null) {
                log.error("Task[{}] round[{}]: no bound train model version found for reused auto-label image selection",
                        task.getId(), currentRound);
                return null;
            }

            ModelVersion boundTrainModelVersion = modelGeneration.getModelExplore().getTrainModelVersion();
            if (!boundTrainModelVersion.isAutoLabel()) {
                log.error("Task[{}] round[{}]: bound train model version does not support auto-label, trainModelVersionId={}, url={}",
                        task.getId(), currentRound, boundTrainModelVersion.getId(), boundTrainModelVersion.getUrl());
                return null;
            }

            String boundImageUrl = boundTrainModelVersion.getUrl();
            if (boundImageUrl == null || boundImageUrl.trim().isEmpty()) {
                log.error("Task[{}] round[{}]: bound train model version url is empty, trainModelVersionId={}",
                        task.getId(), currentRound, boundTrainModelVersion.getId());
                return null;
            }

            log.info("Task[{}] round[{}]: using bound train model image for auto-label, trainModelVersionId={}, imageUrl={}",
                    task.getId(), currentRound, boundTrainModelVersion.getId(), boundImageUrl);
            log.info("Task[{}] round[{}]: task cached autoLabelImageUrl='{}' is ignored for reused auto-label",
                    task.getId(), currentRound, fallback);
            return boundImageUrl;
        } catch (Exception e) {
            log.error("Task[{}] round[{}]: failed to resolve bound train model image for reused auto-label",
                    task.getId(), currentRound, e);
            return null;
        }
    }

    /**
     * 训练完成后保存指标到 SelfIterationJob
     */
    private void saveTrainMetrics(SelfIterationJob job, Long modelJobId) {
        try {
            ModelJob modelJob = modelJobRepo.findModelJobById(modelJobId);
            if (modelJob == null) return;
            org.dlut.adv.mineai.core.entity.ModelJobLogData logData = modelJobLogDataRepo.findByModelJob(modelJob);
            if (logData != null) {
                job.setTrainAccuracy(logData.getAccuracy());
                job.setTrainRecall(logData.getRecall());
                jobRepo.save(job);
                log.info("Task[{}] round[{}]: saved train metrics, accuracy={}, recall={}",
                        job.getSelfIterationTask().getId(), job.getRound(), logData.getAccuracy(), logData.getRecall());
            }
        } catch (Exception e) {
            log.warn("Task[{}] round[{}]: failed to save train metrics",
                    job.getSelfIterationTask().getId(), job.getRound(), e);
        }
    }

    /**
     * 通过 labelId 列表获取标签名称列表。
     */
    private List<String> getLabelNames(List<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<LabelDTO> labels = dubheDataFeign.findLabelByIds(dubheUtils.getAuthorization(), labelIds).getData();
            if (labels == null) return new ArrayList<>();
            return labels.stream().map(LabelDTO::getName).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取标签名称失败：labelIds={}", labelIds, e);
            return new ArrayList<>();
        }
    }

    private CaptureProgressSummary summarizeCaptureProgress(List<Long> executionIds) {
        CaptureProgressSummary summary = new CaptureProgressSummary();
        if (executionIds == null || executionIds.isEmpty()) {
            return summary;
        }

        for (Long executionId : executionIds) {
            RtspCaptureExecutionVO execution = getExecutionById(executionId);
            int capturedCount = 0;
            if (execution != null && execution.getCapturedCount() != null) {
                capturedCount = Math.max(execution.getCapturedCount(), 0);
                summary.totalCapturedCount += capturedCount;
            }

            Integer status = execution != null ? execution.getStatus() : null;
            if (status == null) {
                summary.runningCount++;
                summary.runningExecutionIds.add(executionId);
                continue;
            }
            if (status == CAPTURE_STATUS_SUCCESS
                    || status == CAPTURE_STATUS_FAILED
                    || status == CAPTURE_STATUS_CANCELLED) {
                summary.settledCapturedCount += capturedCount;
                continue;
            }

            summary.runningCount++;
            summary.runningExecutionIds.add(executionId);
        }
        return summary;
    }

    private int resolveIterationStartImageQuantity(SelfIterationTask task) {
        if (task != null && task.getIterationStartImageQuantity() != null
                && task.getIterationStartImageQuantity() > 0) {
            return task.getIterationStartImageQuantity();
        }

        int fallback = sumConfiguredDataSourceImageQuantity(task != null ? task.getDataSourceIds() : null);
        if (fallback > 0) {
            return fallback;
        }

        log.warn("任务[{}]：未配置迭代开始图片数量且无法从数据源计算，使用 1 作为兜底阈值",
                task != null ? task.getId() : null);
        return 1;
    }

    private int sumConfiguredDataSourceImageQuantity(List<Long> dataSourceIds) {
        if (dataSourceIds == null || dataSourceIds.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Long dataSourceId : dataSourceIds) {
            if (dataSourceId == null || dataSourceId <= 0) {
                continue;
            }
            try {
                DataResponseBody<DubheDataFeign.RtspCaptureTaskVO> resp = dubheDataFeign.getRtspCaptureTaskDetail(
                        dubheUtils.getAuthorization(), dataSourceId);
                if (resp != null && resp.succeed() && resp.getData() != null
                        && resp.getData().getImageQuantity() != null) {
                    total += Math.max(resp.getData().getImageQuantity(), 0);
                }
            } catch (Exception e) {
                log.warn("查询采集任务图片数量失败：captureTaskId={}", dataSourceId, e);
            }
        }
        return total;
    }

    private void cancelRunningCaptureExecutions(List<Long> executionIds) {
        if (executionIds == null || executionIds.isEmpty()) {
            return;
        }
        for (Long executionId : new LinkedHashSet<>(executionIds)) {
            if (executionId == null || executionId <= 0) {
                continue;
            }
            try {
                DataResponseBody<Boolean> resp = dubheDataFeign.cancelCaptureExecution(
                        dubheUtils.getAuthorization(), executionId);
                if (resp == null || !resp.succeed() || !Boolean.TRUE.equals(resp.getData())) {
                    log.warn("停止采集执行未成功：executionId={}, msg={}",
                            executionId, resp != null ? resp.getMsg() : "response 为空");
                }
            } catch (Exception e) {
                log.warn("停止采集执行失败：executionId={}", executionId, e);
            }
        }
    }

    private static class CaptureProgressSummary {
        private long totalCapturedCount;
        private long settledCapturedCount;
        private int runningCount;
        private final List<Long> runningExecutionIds = new ArrayList<>();
    }

    /**
     * 检查子任务所有 execution 是否都已成功完成。
     */
    private boolean checkAllExecutionsDone(SelfIterationJob job) {
        // 由 RtspCaptureService/回调机制驱动，此处通过查询 execution 状态验证
        // 当前由 onCaptureComplete 逐个触发，收到通知数 = captureExecutionIds.size() 时全部完成
        // 简单判断：已发版的 datasetVersionIds 数量等于 captureExecutionIds 数量
        // 采集阶段无 versionIds，改为判断"完成的 execution 数"——由调用方计数或查库
        // TODO: 接入 RtspCaptureExecution 状态查询
        return true; // 占位，具体实现依赖采集完成通知机制
    }

    /**
     * 根据 executionId 查找对应的子任务（遍历 captureExecutionIds）。
     */
    private SelfIterationJob findJobByExecutionId(Long executionId) {
        return jobRepo.findByStatus(SelfIterationJob.STATUS_COLLECTING)
                .stream()
                .filter(j -> j.getCaptureExecutionIds() != null && j.getCaptureExecutionIds().contains(executionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 executionId 获取对应 Dataset 信息（datasetId 等）。
     */
    private RtspCaptureExecutionVO getExecutionById(Long executionId) {
        try {
            DataResponseBody<RtspCaptureExecutionVO> resp = dubheDataFeign.getCaptureExecution(
                    dubheUtils.getAuthorization(), executionId);
            if (resp != null && resp.succeed()) {
                return resp.getData();
            }
        } catch (Exception e) {
            log.error("查询 execution 失败：executionId={}", executionId, e);
        }
        return null;
    }

    private SelfIterationTask requireTask(Long taskId) {
        SelfIterationTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("自迭代任务不存在：id=" + taskId));
        assertAccessibleTask(task);
        return task;
    }

    private SelfIterationJob requireJob(Long jobId) {
        SelfIterationJob job = jobRepo.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("自迭代子任务不存在：id=" + jobId));
        assertAccessibleTask(job.getSelfIterationTask());
        return job;
    }

    private SelfIterationJob requireJobForUpdate(Long jobId) {
        SelfIterationJob job = jobRepo.findByIdForUpdate(jobId)
                .orElseThrow(() -> new IllegalArgumentException("鑷凯浠ｅ瓙浠诲姟涓嶅瓨鍦細id=" + jobId));
        assertAccessibleTask(job.getSelfIterationTask());
        return job;
    }

    private void assertAccessibleTask(SelfIterationTask task) {
        UserContext userContext = UserContextHolder.getUserContext();
        if (userContext == null) {
            return;
        }
        if (task == null) {
            throw new IllegalArgumentException("自迭代任务不存在或无权限访问");
        }
        if (isManagerRole(userContext)) {
            return;
        }
        if (!Objects.equals(task.getUserId(), Long.valueOf(userContext.getId()))) {
            throw new IllegalArgumentException("自迭代任务不存在或无权限访问");
        }
    }

    private boolean isManagerRole(UserContext userContext) {
        if (userContext.getRoles() == null) {
            return false;
        }
        return userContext.getRoles().stream()
                .filter(Objects::nonNull)
                .anyMatch(role -> "管理员".equals(role.getName()) || "管理人员".equals(role.getName()));
    }

    /**
     * 获取或创建关联的 ModelGeneration
     */
    private ModelGeneration getOrCreateModelGeneration(SelfIterationTask task) {
        ModelGenerationService modelGenerationService = SpringContextHolder.getBean(ModelGenerationService.class);

        String generationName = "self-iter-" + task.getId();
        ModelGeneration existing = modelGenerationService.findModelGenerationByName(generationName);

        if (existing != null) {
            // 补填旧数据可能缺失的 model/modelExplore
            if (existing.getModel() == null && task.getModelApplication() != null && task.getModelApplication().getId() != null) {
                ModelApplicationRepo modelApplicationRepo = SpringContextHolder.getBean(ModelApplicationRepo.class);
                ModelApplication freshApp = modelApplicationRepo.findById(task.getModelApplication().getId()).orElse(null);
                if (freshApp != null && freshApp.getModel() != null) {
                    existing.setModel(freshApp.getModel());
                    if (freshApp.getModel().getGenerationId() > 0) {
                        ModelGeneration sourceGeneration = modelGenerationService.findModelGenerationById(freshApp.getModel().getGenerationId());
                        if (sourceGeneration != null && sourceGeneration.getModelExplore() != null) {
                            existing.setModelExplore(sourceGeneration.getModelExplore());
                        }
                    }
                    modelGenerationService.saveModelGeneration(existing);
                }
            }
            return existing;
        }

        ModelGeneration modelGeneration = new ModelGeneration();
        modelGeneration.setName(generationName);
        modelGeneration.setUserId(task.getUserId());
        modelGeneration.setModelApplication(task.getModelApplication());
        modelGeneration.setAnnotationType("YOLO");

        // 从 modelApplication.model 推导关联的 model 和 modelExplore，供 getTrainModelVersion 查找训练镜像
        if (task.getModelApplication() != null && task.getModelApplication().getId() != null) {
            ModelApplicationRepo modelApplicationRepo = SpringContextHolder.getBean(ModelApplicationRepo.class);
            ModelApplication freshApp = modelApplicationRepo.findById(task.getModelApplication().getId()).orElse(null);
            if (freshApp != null && freshApp.getModel() != null) {
                modelGeneration.setModel(freshApp.getModel());
                if (freshApp.getModel().getGenerationId() > 0) {
                    ModelGeneration sourceGeneration = modelGenerationService.findModelGenerationById(freshApp.getModel().getGenerationId());
                    if (sourceGeneration != null && sourceGeneration.getModelExplore() != null) {
                        modelGeneration.setModelExplore(sourceGeneration.getModelExplore());
                    }
                }
            }
        }

        modelGenerationService.saveModelGeneration(modelGeneration);
        return modelGeneration;
    }

    /**
     * 等待训练Job创建完成（用于数据集切分异步场景）
     */
    private void waitForTrainJobCreation(SelfIterationTask task, SelfIterationJob job, Long modelGenerationId) {
        ModelGenerationService modelGenerationService = SpringContextHolder.getBean(ModelGenerationService.class);
        ModelJobService modelJobService = SpringContextHolder.getBean(ModelJobService.class);
        int maxRetries = 360; // 最多等待30分钟 (360 * 5秒)
        int retryCount = 0;

        log.info("Task[{}] round[{}]: waiting for train job creation after dataset split", task.getId(), job.getRound());

        while (retryCount < maxRetries) {
            try {
                Thread.sleep(5000); // 每5秒检查一次

                ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(modelGenerationId);
                if (modelGeneration == null) {
                    log.error("Task[{}] round[{}]: modelGeneration not found", task.getId(), job.getRound());
                    onTrainFailed(job.getId());
                    return;
                }

                Long latestJobId = modelGeneration.getLatestJobId();
                if (latestJobId != null && latestJobId > 0) {
                    // 检查训练Job的状态
                    ModelJob modelJob = modelJobService.getModelJobById(latestJobId);
                    if (modelJob != null) {
                        Integer status = modelJob.getStatus();
                        // 状态码：12=SPLITTING(切分中), 11=QUEUING(排队中), 1=TRAINING(训练中)
                        if (status != null && (status == 11 || status == 1)) {
                            // 训练Job已创建并开始排队或训练
                            job.setTrainJobId(latestJobId);
                            jobRepo.save(job);
                            log.info("Task[{}] round[{}]: train job created after dataset split, jobId={}, status={}",
                                    task.getId(), job.getRound(), latestJobId, status);

                            // 开始监听训练状态
                            trackTrainJobStatus(task, job);
                            return;
                        } else if (status != null && status < 0) {
                            // 训练Job失败
                            log.error("Task[{}] round[{}]: train job failed, status={}", task.getId(), job.getRound(), status);
                            onTrainFailed(job.getId());
                            return;
                        }
                        // 否则继续等待（状态可能是12=SPLITTING）
                    }
                }

                retryCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Task[{}] round[{}]: interrupted while waiting for train job creation", task.getId(), job.getRound());
                onTrainFailed(job.getId());
                return;
            } catch (Exception e) {
                log.error("Task[{}] round[{}]: error while waiting for train job creation", task.getId(), job.getRound(), e);
                retryCount++;
            }
        }

        log.error("Task[{}] round[{}]: timeout waiting for train job creation", task.getId(), job.getRound());
        onTrainFailed(job.getId());
    }

    /**
     * 监听训练Job状态
     */
    private void trackTrainJobStatus(SelfIterationTask task, SelfIterationJob job) {
        ModelJobService modelJobService = SpringContextHolder.getBean(ModelJobService.class);

        while (true) {
            try {
                ModelJob modelJob = modelJobService.getModelJobById(job.getTrainJobId());
                if (modelJob == null) {
                    log.error("Task[{}] round[{}]: train job not found", task.getId(), job.getRound());
                    onTrainFailed(job.getId());
                    break;
                }

                int status = modelJob.getStatus();

                if (ModelJobStateCodeConstant.TRAIN_SUCCEEDED.equals(status)) {
                    log.info("Task[{}] round[{}]: train succeeded", task.getId(), job.getRound());
                    try {
                        saveTrainMetrics(job, modelJob.getId());
                        onTrainComplete(job.getId(), job.getTrainJobId());
                    } catch (Exception e) {
                        log.error("Task[{}] round[{}]: train succeeded but complete callback failed", task.getId(), job.getRound(), e);
                        onTrainFailed(job.getId());
                    }
                    break;
                } else if (ModelJobStateCodeConstant.TRAIN_FAILED.equals(status) ||
                        ModelJobStateCodeConstant.CANCELED.equals(status)) {
                    log.error("Task[{}] round[{}]: train failed or canceled, status={}", task.getId(), job.getRound(), status);
                    onTrainFailed(job.getId());
                    break;
                }

                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Task[{}] round[{}]: error while tracking train status", task.getId(), job.getRound(), e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * 监听转换Job状态
     */
    private void trackConvertJobStatus(SelfIterationTask task, SelfIterationJob job) {
        ModelJobService modelJobService = SpringContextHolder.getBean(ModelJobService.class);

        while (true) {
            try {
                ModelJob modelJob = modelJobService.getModelJobById(job.getConvertJobId());
                if (modelJob == null) {
                    log.error("Task[{}] round[{}]: convert job not found, jobId={}",
                            task.getId(), job.getRound(), job.getConvertJobId());
                    onTrainFailed(job.getId());
                    return;
                }

                int status = modelJob.getStatus();

                if (ModelJobStateCodeConstant.CONVERT_SUCCEEDED.equals(status)) {
                    log.info("Task[{}] round[{}]: convert succeeded", task.getId(), job.getRound());
                    onConvertComplete(job.getId(), job.getConvertJobId());
                    return;
                } else if (ModelJobStateCodeConstant.CONVERT_FAILED.equals(status) ||
                        ModelJobStateCodeConstant.CANCELED.equals(status)) {
                    log.error("Task[{}] round[{}]: convert failed or canceled, status={}", task.getId(), job.getRound(), status);
                    onTrainFailed(job.getId());
                    return;
                }

                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Task[{}] round[{}]: error while tracking convert status", task.getId(), job.getRound(), e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static class CollectConfig {
        private final List<Long> captureTaskIds;
        private final boolean delRaw;

        private CollectConfig(List<Long> captureTaskIds, boolean delRaw) {
            this.captureTaskIds = captureTaskIds;
            this.delRaw = delRaw;
        }

        private List<Long> getCaptureTaskIds() {
            return captureTaskIds;
        }

        private boolean isDelRaw() {
            return delRaw;
        }
    }

}
