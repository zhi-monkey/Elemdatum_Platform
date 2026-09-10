package org.dlut.adv.mineai.model.service;

import io.minio.Result;
import io.minio.messages.DeleteError;
import org.dlut.adv.mineai.core.entity.ExternalModelConverter;
import org.dlut.adv.mineai.core.entity.HardwareParams;
import org.dlut.adv.mineai.core.entity.ModelGeneration;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.core.utils.RedisUtil;
import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.dto.ModelConvertDTO;
import org.dlut.adv.mineai.model.domain.vo.TaskStatusVO;
import org.dlut.adv.mineai.model.dto.DatasetMergeRequest;
import org.dlut.adv.mineai.model.dto.QuantizedMergeRequest;
import org.dlut.adv.mineai.model.kubernetes.controller.JobController;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.MinioUtils;
import org.dlut.adv.mineai.model.utils.ModelConversionQueueManager;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class AsyncTaskService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    ModelDatasetService modelDatasetService;

    @Autowired
    JobController jobController;
    @Resource
    ModelJobRepo modelJobRepo;
    @Lazy
    @Autowired
    ModelGenerationService modelGenerationService;

    @Lazy
    @Autowired
    ModelJobService modelJobService;

    @Autowired
    DubheDataFeign dubheDataFeign;

    @Autowired
    DubheUtils dubheUtils;

    @Autowired
    RedisUtil redisUtil;

    @Resource
    private ExternalModelConverterService externalModelConverterService;

    private ModelConversionQueueManager modelConversionQueueManager = null;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${datasetTaskPool.datasetSplit.maxRetries:120}")
    private int datasetSplitMaxRetries;

    // Redis key前缀
    private static final String DATASET_CUT_TASK_KEY_PREFIX = "dataset_cut_task:";
    private static final long TASK_EXPIRE_TIME = 24 * 60 * 60; // 24小时过期
    @Autowired
    private MinioUtils minioUtils;

    /**
     * 提交数据集切分任务
     */
    public String submitDatasetCutAndCreateJob(
            String namespace,
            ModelJob modelJobSaved,
            ModelGeneration modelGeneration,
            String url,
            HardwareParams hardwareParams,
            String preWeightPath,
            String modelType
    ) {
        AtomicReference<String> datasetTaskId = new AtomicReference<>();
        // 异步任务
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 构建数据集合并请求
                DatasetMergeRequest request = buildDatasetMergeRequest(modelGeneration);

                // 2. 调用数据集切分API
                System.out.println("开始提交数据集切分任务...");
                System.out.println("modelJobSaved.getId() = " + modelJobSaved.getId());

                DataResponseBody<String> response = dubheDataFeign.datasetAssembleFromMultipleVersionAsync(
                        dubheUtils.getAuthorization(),
                        request
                );

                if (response == null || !response.succeed()) {
                    throw new RuntimeException("数据集切分任务提交失败: " +
                            (response != null ? response.getMsg() : "response为空"));
                }

                datasetTaskId.set(response.getData());
                System.out.println("数据集切分任务已提交，taskId: " + datasetTaskId);

                // 3. 存储taskId到Redis
                storeTaskIdToRedis(modelJobSaved.getId(), datasetTaskId.get());

                // 4. 等待数据集切分完成
                boolean splitSuccess = waitForDatasetSplitCompletion(datasetTaskId.get());


                if (!splitSuccess) {
                    ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
                    modelJobStateMachine.modelJobSplitFailEvent(Integer.parseInt(String.valueOf(modelJobSaved.getId())));
                    throw new RuntimeException("数据集切分失败或取消");
                }

                System.out.println("数据集切分完成，开始创建训练任务...");
                removeTaskIdFromRedis(modelJobSaved.getId());
                DataResponseBody<TaskStatusVO> statusResponse = dubheDataFeign.getTaskStatus(
                        dubheUtils.getAuthorization(),
                        datasetTaskId.get()
                );
                if(Objects.equals(statusResponse.getData().getStatus(), "CANCELLED")){
                    System.out.println("数据集切分取消");
                    return ;
                }
                if(Objects.equals(statusResponse.getData().getStatus(), "FAILED")){
                    System.out.println("数据集切分失败");
                    return ;
                }

                System.out.println("数据集切分完成，开始创建训练任务...");
                Map<String, String> resultMap = (Map<String, String>) statusResponse.getData().getResult();

                String vGpuCores = null;
                switch (modelJobSaved.getGpuMode()) {
                    case ModelJob.GPU_MODE_SINGLE_EXCLUSIVE:
                        vGpuCores = "100"; // 独享
                        break;

                    case ModelJob.GPU_MODE_SINGLE_SHARED:
                    case ModelJob.GPU_MODE_MULTI_SHARED:
                        vGpuCores = "50";  // 共享
                        break;

                    default:
                        vGpuCores = "100";
                }

                // 6. 创建训练任务
                Map<String, String> hyperParams = modelJobSaved.getParams();

                // 如果是多卡独占模式，将batchsize乘以显卡数量
                if (modelJobSaved.getGpuMode().equals(ModelJob.GPU_MODE_MULTI_EXCLUSIVE) && hyperParams != null) {
                        if (hyperParams.containsKey("HP_BATCH_SIZE")) {
                            try {
                                int originalBatchSize = Integer.parseInt(hyperParams.get("HP_BATCH_SIZE"));
                                int adjustedBatchSize = originalBatchSize * modelJobSaved.getGpuCount();
                                hyperParams.put("HP_BATCH_SIZE", String.valueOf(adjustedBatchSize));
                                System.out.println("多卡独占模式：将" + "HP_BATCH_SIZE" + "从" + originalBatchSize + "调整为" + adjustedBatchSize);
                            } catch (NumberFormatException e) {
                                System.out.println("WARNING: " + "HP_BATCH_SIZE" + "参数值不是有效的数字: " + hyperParams.get("HP_BATCH_SIZE"));
                            }
                        }
                }

                jobController.createTrainJobUseCutDataset(
                        namespace,
                        modelJobSaved.getName(),
                        modelJobSaved.getWeightPath(),
                        url,
                        resultMap.get("trainImagesPath"),
                        resultMap.get("testImagesPath"),
                        resultMap.get("valImagesPath"),
                        resultMap.get("trainAnnotationsPath"),
                        resultMap.get("testAnnotationsPath"),
                        resultMap.get("valAnnotationsPath"),
                        request.getTargetDir()+ "data.yaml",
                        modelJobSaved.getGpuCount(),
                        hardwareParams == null ? null : hardwareParams.getCpus(),
                        hardwareParams == null ? null : hardwareParams.getMemory(),
                        hardwareParams == null ? null : hardwareParams.getGpuMemory(),
                        vGpuCores,
                        hyperParams, // 使用调整后的hyperParams
                        preWeightPath,
                        modelType
                );

                System.out.println("训练任务创建完成");

                System.out.println("训练任务创建完成");

            } catch (Exception e) {
                // 状态机 - 切分失败
                ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
                modelJobStateMachine.modelJobSplitFailEvent(Integer.parseInt(String.valueOf(modelJobSaved.getId())));
                e.printStackTrace();
                System.err.println("数据集切分或训练任务创建失败：" + datasetTaskId.get());

                // 清理Redis中的taskId
                removeTaskIdFromRedis(modelJobSaved.getId());

                // 清理train_temp文件夹
                String folderPath = "train_temp/" + modelJobSaved.getId();
                try {
                    minioUtils.delFolder(minioUtils.getBucketName(), folderPath);
                    System.out.println("已删除切分失败的临时文件夹: " + folderPath);
                } catch (Exception delEx) {
                    System.err.println("删除临时文件夹失败: " + folderPath + ", 错误: " + delEx.getMessage());
                }

                ModelJob failedJob = modelJobService.getModelJobById(modelJobSaved.getId());
                modelJobService.saveModelJob(failedJob);
            }
        }, executorService);

        return datasetTaskId.get();
    }

    /**
     * 构建数据集合并请求
     */
    private DatasetMergeRequest buildDatasetMergeRequest(ModelGeneration modelGeneration) {
        DatasetMergeRequest request = new DatasetMergeRequest();

        // 设置版本信息 (根据您的业务逻辑调整)
        request.setVersions(modelGeneration.getTrainDatasetVersions());

        // 设置目标目录
        request.setTargetDir("train_temp/" + modelGeneration.getLatestJobId() +"/");

        // 设置是否需要切分
        request.setIsSplit(modelGeneration.getSplitSize() != null && !modelGeneration.getSplitSize().isEmpty());

        // 设置切分比例
        if (request.getIsSplit()) {
            request.setSplitRatio(normalizeSplitRatio(modelGeneration.getSplitSize()));
        }

        // 设置是否保留标签
        // 设置保留的标签列表 - 从Map中提取values，正确处理Hibernate集合代理
        if (modelGeneration.getSelectedLabels() != null && !modelGeneration.getSelectedLabels().isEmpty()) {
            // 安全地创建新的List实例，避免Hibernate集合代理问题
            List<String> labelsList = new ArrayList<>();
            Collection<String> values = modelGeneration.getSelectedLabels().values();
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    labelsList.add(value);
                }
            }
            request.setKeepLabels(labelsList);
            System.out.println("labelsList = " + labelsList);
        } else {
            request.setKeepLabels(new ArrayList<>()); // 设置为空列表而不是null
        }

        return request;
    }

    private String normalizeSplitRatio(String splitRatio) {
        String defaultSplitRatio = "80-10-10";
        String rawSplitRatio = splitRatio == null || splitRatio.trim().isEmpty() ? "8-1-1" : splitRatio;
        String[] split = rawSplitRatio.split("-");
        if (split.length != 3) {
            return defaultSplitRatio;
        }

        try {
            List<Double> parts = Arrays.stream(split)
                    .map(String::trim)
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
            double total = parts.stream().reduce(0.0, Double::sum);
            if (total <= 0 || parts.stream().anyMatch(item -> item <= 0)) {
                return defaultSplitRatio;
            }
            if (Math.abs(total - 10.0) < 0.000001) {
                parts = parts.stream().map(item -> item * 10).collect(Collectors.toList());
            } else if (Math.abs(total - 100.0) >= 0.000001) {
                parts = parts.stream().map(item -> item / total * 100).collect(Collectors.toList());
            }

            List<Long> roundedParts = parts.stream().map(Math::round).collect(Collectors.toList());
            if (roundedParts.stream().anyMatch(item -> item < 1)) {
                return defaultSplitRatio;
            }
            return String.format(
                    Locale.ROOT,
                    "%d-%d-%d",
                    roundedParts.get(0),
                    roundedParts.get(1),
                    roundedParts.get(2)
            );
        } catch (NumberFormatException e) {
            return defaultSplitRatio;
        }
    }

    /**
     * 等待数据集切分完成
     */
    private boolean waitForDatasetSplitCompletion(String datasetTaskId) {
        int maxRetries = datasetSplitMaxRetries; // 最多等待20分钟 (120 * 10秒)
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                DataResponseBody<TaskStatusVO> statusResponse = dubheDataFeign.getTaskStatus(
                        dubheUtils.getAuthorization(),
                        datasetTaskId
                );

                if (statusResponse != null && statusResponse.succeed()) {
                    TaskStatusVO taskStatus = statusResponse.getData();

                    if (taskStatus != null) {
                        String status = taskStatus.getStatus().toString();
                        System.out.println("数据集任务状态: " + status + ", 进度: " + taskStatus.getProgress() + "%");

                        if ("COMPLETED".equals(status)) {
                            System.out.println("数据集任务完成");

                            return true;
                        } else if ("FAILED".equals(status) || "CANCELLED".equals(status) ) {
                            System.err.println("数据集任务失败: " + taskStatus.getErrorMessage());
                            return false;
                        }
                    }
                }

                // 等待10秒后重试
                Thread.sleep(10000);
                retryCount++;

            } catch (Exception e) {
                System.err.println("查询数据集任务状态失败: " + e.getMessage());
                retryCount++;
                try {
                    Thread.sleep(5000); // 出错时等待5秒
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        System.err.println("等待数据集完成超时");
        return false;
    }

    /**
     * 存储taskId到Redis
     */
    private void storeTaskIdToRedis(Long modelJobId, String datasetTaskId) {
        try {
            String key = DATASET_CUT_TASK_KEY_PREFIX + modelJobId;
            redisUtil.set(key, datasetTaskId, TASK_EXPIRE_TIME, TimeUnit.SECONDS);
            System.out.println("已存储数据集任务ID到Redis: " + key + " -> " + datasetTaskId);
        } catch (Exception e) {
            System.err.println("存储taskId到Redis失败: " + e.getMessage());
        }
    }

    /**
     * 从Redis获取taskId
     */
    public String getTaskIdFromRedis(Long modelJobId) {
        try {
            String key = DATASET_CUT_TASK_KEY_PREFIX + modelJobId;
            return (String) redisUtil.get(key);
        } catch (Exception e) {
            System.err.println("从Redis获取taskId失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从Redis删除taskId
     */
    private void removeTaskIdFromRedis(Long modelJobId) {
        try {
            String key = DATASET_CUT_TASK_KEY_PREFIX + modelJobId;
            redisUtil.del(key);
            System.out.println("已从Redis删除数据集任务ID: " + key);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("从Redis删除taskId失败: " + e.getMessage());
        }
    }

    /**
     * 停止数据集切分任务
     */
    public boolean stopDatasetSplitTask(Long modelJobId) {
        CompletableFuture.runAsync(() -> {
            String datasetTaskId = getTaskIdFromRedis(modelJobId);
            if (datasetTaskId == null) {
                System.err.println("未找到模型任务对应的数据集切分任务ID: " + modelJobId);
            }

            try {
                DataResponseBody<String> response = dubheDataFeign.cancelTask(dubheUtils.getAuthorization(), datasetTaskId);

                // 清理Redis
                removeTaskIdFromRedis(modelJobId);
                String folderPath = "train_temp/" + modelJobId;
                minioUtils.delFolder(minioUtils.getBucketName(), folderPath);
                System.out.println("已停止数据集切分任务: " + datasetTaskId);
            } catch (Exception e) {
                System.err.println("停止数据集切分任务失败: " + e.getMessage());
            }
        }, executorService);

        return true;
    }



    public String submitQuantizedAndCreateJob(ModelJob modelJob, ModelConvertDTO convertDTO,Long total,boolean isOutSide,Optional<ExternalModelConverter> selectedConverterOpt ,boolean isQuantized,List<Long> trainDatasetVersions) {
        AtomicReference<String> datasetTaskId = new AtomicReference<>();
        // 异步任务
        CompletableFuture.runAsync(() -> {
            String targetDir = null;
            try {
                if (isQuantized){
                    // 1. 构建数据集合并请求
                    targetDir = "convert_temp/" + modelJob.getId() +"/";
                    QuantizedMergeRequest request = new QuantizedMergeRequest();
                    request.setTotal(total);
                    request.setVersions(trainDatasetVersions);
                    request.setTargetDir(targetDir);

                    // 2. 调用数据集切分API
                    System.out.println("开始提交量化图片抽取任务...");

                    DataResponseBody<String> response = dubheDataFeign.quantizedMerge(
                            dubheUtils.getAuthorization(),
                            request
                    );

                    if (response == null || !response.succeed()) {
                        throw new RuntimeException("量化图片抽取提交失败: " +
                                (response != null ? response.getMsg() : "response为空"));
                    }

                    datasetTaskId.set(response.getData());
                    System.out.println("量化图片抽取已提交，taskId: " + datasetTaskId);

                    // 3. 存储taskId到Redis
                    storeTaskIdToRedis(modelJob.getId(), datasetTaskId.get());

                    // 4. 等待数据集切分完成
                    boolean splitSuccess = waitForDatasetSplitCompletion(datasetTaskId.get());
                    if (!splitSuccess) {
                        ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
                        modelJobStateMachine.modelJobConvertFailedEvent(Integer.parseInt(String.valueOf(modelJob.getId())));
                        minioUtils.delFolder(bucketName,targetDir);
                        throw new RuntimeException("量化图片抽失败或取消");
                    }

                    System.out.println("量化图片抽取完成，开始创建任务...");
                    removeTaskIdFromRedis(modelJob.getId());
                    DataResponseBody<TaskStatusVO> statusResponse = dubheDataFeign.getTaskStatus(
                            dubheUtils.getAuthorization(),
                            datasetTaskId.get()
                    );
                    if(Objects.equals(statusResponse.getData().getStatus(), "CANCELLED")){
                        System.out.println("量化图片抽取取消");
                        return ;
                    }
                    if(Objects.equals(statusResponse.getData().getStatus(), "FAILED")){
                        System.out.println("量化图片抽取失败");
                        return ;
                    }

                    System.out.println("量化图片抽取完成，开始创建转换任务...");
                }
                if (modelConversionQueueManager == null) {
                    modelConversionQueueManager = new ModelConversionQueueManager(modelJobRepo, jobController, externalModelConverterService);
                }
                if (isOutSide) {
                    modelConversionQueueManager.addConversionTask(modelJobRepo.findModelJobByName(modelJob.getName()), convertDTO, targetDir, selectedConverterOpt.get());
                } else {
                    // 开启模型转换任务
                    jobController.modelConvertStart(convertDTO, targetDir);
                }

                System.out.println("转换任务创建完成");

            } catch (Exception e) {
                // 状态机 - 抽取失败
                ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
                modelJobStateMachine.modelJobConvertFailedEvent(Integer.parseInt(String.valueOf(modelJob.getId())));
                e.printStackTrace();
                if(isQuantized){
                    minioUtils.delFolder(bucketName,targetDir);
                }
                System.err.println("量化数据集抽取或转换任务创建失败：" + datasetTaskId.get());
                // 清理Redis中的taskId
                removeTaskIdFromRedis(modelJob.getId());
            }
        }, executorService);

        return datasetTaskId.get();

    }

    /**
     * 异步删除 MinIO 文件夹
     * @param bucketName 存储桶名称
     * @param folderPath 文件夹路径
     */
    public void asyncDeleteFolder(String bucketName, String folderPath) {
        CompletableFuture.runAsync(() -> {
            try {
                minioUtils.delFolder(bucketName, folderPath);
            } catch (Exception e) {
                System.err.println("异步删除失败 [" + folderPath + "]: " + e.getMessage());
                e.printStackTrace();
            }
        }, executorService);
    }

}
