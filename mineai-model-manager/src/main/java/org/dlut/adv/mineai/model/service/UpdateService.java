package org.dlut.adv.mineai.model.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.constant.ModelJobStateCodeConstant;
import org.dlut.adv.mineai.core.constant.NotificationOperationTypeEnum;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.client.DubheUserFeign;
import org.dlut.adv.mineai.model.dto.UserResourceDTO;
import org.dlut.adv.mineai.model.kubernetes.controller.DeploymentController;
import org.dlut.adv.mineai.model.kubernetes.controller.JobController;
import org.dlut.adv.mineai.model.kubernetes.service.JobService;
import org.dlut.adv.mineai.model.kubernetes.service.LogService;
import org.dlut.adv.mineai.model.repository.ModelDeploymentRepo;
import org.dlut.adv.mineai.model.repository.ModelGenerationRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.statusMachine.statemachine.StateMachineFactory;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.MinioUtils;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhensun
 */
@Service
@Slf4j
public class UpdateService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    @Resource(name = "executor")
    ThreadPoolTaskScheduler threadPoolTaskScheduler;
    @Value("${kubernetes.config}")
    private String k8sConfig;

    @Resource
    private ModelJobService modelJobService;

    @Resource
    private ModelJobRepo modelJobRepo;

    @Resource
    private JobService jobService;

    @Resource
    private JobController jobController;

    @Value("${kubernetes.namespace}")
    private String namespace;

    @Resource
    private LogService logService;

    @Resource
    private ModelVersionService modelVersionService;

    @Value("${container.weightPath}")
    private String weightRootPath;

    @Resource
    private ModelDeploymentService modelDeploymentService;

    @Resource
    private DeploymentController deploymentController;

    @Resource
    private ModelDeploymentRepo modelDeploymentRepo;

    @Resource
    private ModelGenerationRepo modelGenerationRepo;

    @Resource
    ModelDatasetService modelDatasetService;

    @Resource
    private DubheUserFeign dubheUserFeign;
    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private DubheUtils dubheUtils;
    @Autowired
    private MinioUtils minioUtils;

    @Resource
    AsyncTaskService asyncTaskService;



    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 更新所有的状态信息
     */
    public void updateAllJobStatus() throws Exception {
        List<ModelJob> modelJobList = modelJobService.getModelJobOnChange();

        // 去重：按Job名称去重，避免处理重复的Job
        Map<String, ModelJob> uniqueJobs = new LinkedHashMap<>();
        for (ModelJob job : modelJobList) {
            uniqueJobs.put(job.getName(), job);
        }
        List<ModelJob> deduplicatedList = new ArrayList<>(uniqueJobs.values());

        List<ModelJob> needCheckModelJobList = new ArrayList<>();

        for (ModelJob modelJob : deduplicatedList) {
            // 只处理执行中的Job
            if (modelJob.isExecuting()) {
                // 跳过数据库中已标记为完成状态的Job
                if (isJobAlreadyCompleted(modelJob)) {
                    String folderPath = "train_temp/" + modelJob.getId();

                    try {
                        asyncTaskService.asyncDeleteFolder(minioUtils.getBucketName(), folderPath);
                    } catch (Exception e) {
                        log.error("处理文件夹删除时发生错误: {}, 错误: {}", folderPath, e.getMessage());
                    }
                    continue;
                }

                // 检查Job在K8s中是否还存在
                if (!jobService.jobExists(namespace, modelJob.getName())) {
                    needCheckModelJobList.add(modelJob);
                    continue;
                }

                // 获取K8s中Job的实时状态
                JSONObject statusJson = (JSONObject) JSON.toJSON(jobService.getJob(namespace, modelJob.getName()));
                JSONObject status = statusJson.getJSONObject("status");

                // 跳过仍在运行中的Job (active > 0表示有Pod在运行)
                if (status.containsKey("active") && status.get("active") != null && status.getInteger("active") > 0) {
                    continue;
                }

                // 解析conditions数组，查找Complete或Failed状态
                JSONArray conditions = status.getJSONArray("conditions");
                if (conditions != null && !conditions.isEmpty()) {
                    for (int i = 0; i < conditions.size(); i++) {
                        JSONObject condition = conditions.getJSONObject(i);

                        // 处理Job成功完成的情况
                        if ("Complete".equals(condition.getString("type")) && "True".equals(condition.getString("status"))) {
                            handleJobSuccess(modelJob);
                            break; // 避免重复处理同一Job
                        }
                        // 处理Job失败的情况
                        else if ("Failed".equals(condition.getString("type")) && "True".equals(condition.getString("status")) || isConvertError(modelJob)) {
                            handleJobFailure(modelJob);
                            break; // 避免重复处理同一Job
                        }
                    }
                }
            }
        }
    }

    // 检查job是否已经完成，避免重复处理
    private boolean isJobAlreadyCompleted(ModelJob modelJob) {
        Integer status = modelJob.getStatus();
        return Objects.equals(status, ModelJobStateCodeConstant.TRAIN_SUCCEEDED) ||
                Objects.equals(status, ModelJobStateCodeConstant.TRAIN_FAILED) ||
                Objects.equals(status, ModelJobStateCodeConstant.CONVERT_SUCCEEDED) ||
                Objects.equals(status, ModelJobStateCodeConstant.CONVERT_FAILED) ||
                Objects.equals(status, ModelJobStateCodeConstant.CANCELED) ||
                Objects.equals(status, ModelJobStateCodeConstant.PUBLISHED);
    }

    // 处理Job成功的统一入口
    private void handleJobSuccess(ModelJob modelJob) {
        System.out.println("更新了succeeded：" + modelJob.getName() + " before状态: " + String.valueOf(modelJob.getStatus()));

        Optional<ModelGeneration> byId = modelGenerationRepo.findById(modelJob.getModelGenerationId());
        byId.ifPresent(modelGeneration -> {
            if (modelJob.getJobType() == ModelJob.TRAIN) {
                handleTrainJobSuccess(modelJob);
            } else if (modelJob.getJobType() == ModelJob.CONVERT) {
                handleConvertJobSuccess(modelJob);
            } else {
                handleInspectJobSuccess(modelJob);
            }

            System.out.println("更新了：" + modelJob.getName() + " after状态: " + String.valueOf(modelJob.getStatus()));
        });
    }

    // 处理Job失败的统一入口
    private void handleJobFailure(ModelJob modelJob) {
        System.out.println("更新了failed：" + modelJob.getName() + " before状态: " + String.valueOf(modelJob.getStatus()));

        Optional<ModelGeneration> byId = modelGenerationRepo.findById(modelJob.getModelGenerationId());
        byId.ifPresent(modelGeneration -> {
            if (modelJob.getJobType() == ModelJob.TRAIN) {
                handleTrainJobFailure(modelJob);
            } else if (modelJob.getJobType() == ModelJob.CONVERT) {
                handleConvertJobFailure(modelJob);
            } else {
                handleInspectJobFailure(modelJob);
            }
            System.out.println("更新了：" + modelJob.getName() + " after状态: " + String.valueOf(modelJob.getStatus()));
        });
    }

    // 处理训练Job成功
    private void handleTrainJobSuccess(ModelJob modelJob) {
        List<ModelJob> convertJoblist = modelJobService.getConvertJobsByTrainJobID(modelJob.getId());
        boolean allSucceeded = true;

        for (ModelJob convertModelJob : convertJoblist) {
            if (Objects.equals(convertModelJob.getStatus(), ModelJobStateCodeConstant.TRAINING)) {
                // 有一个任务还在转换，直接退出，不改变状态
                allSucceeded = false;
                break;
            }
        }
        //如果全部的转换任务都完成了。把训练job的状态置为训练成功吧（主要是给标准化列表服务的）
        if (allSucceeded) {
            // 触发训练成功事件
            ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
            modelJobStateMachine.modelJobTrainSucceededEvent(Integer.parseInt(String.valueOf(modelJob.getId())));
        }
        // 创建训练成功通知
        createTrainNotification(modelJob, true);
        // 释放CPU和内存资源
        log.info("正在由于训练成功释放用户资源");
        try {
            releaseHardWareResource(modelJob);
        } catch (Exception e) {
            log.error("训练成功释放用户资源异常, jobName: {}", modelJob == null ? null : modelJob.getName(), e);
        }
        log.info("由于训练成功释放用户资源");

        String folderPath = "train_temp/" + modelJob.getId();
        try {
            List<String> files = minioUtils.listObjectsInFolder(bucketName, folderPath);
            if (!files.isEmpty()) {
                asyncTaskService.asyncDeleteFolder(minioUtils.getBucketName(), folderPath);
            } else {
                log.debug("文件夹不存在或已为空: {}", folderPath);
            }
        } catch (Exception e) {
            log.error("处理文件夹删除时发生错误: {}, 错误: {}", folderPath, e.getMessage());
        }



        // 触发getLog中的持久化
//        log.info("训练成功 自动持久化日志");
//        logService.getLog(namespace, modelJob.getName(), "FORWARD", 99999L, null, null);
    }

    // 处理转换Job成功
    private void handleConvertJobSuccess(ModelJob modelJob) {
        // 从转换任务名称中提取对应的训练任务名称
        String extractedName = extractJobName(modelJob.getName());
        // 使用截取后的名称查找对应的训练任务
        ModelJob starterJob = modelJobRepo.findModelJobByName(extractedName);

        // 先触发对应训练任务的成功事件
        ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobStateMachine.modelJobTrainSucceededEvent(Integer.parseInt(String.valueOf(starterJob.getId())));

        // 释放转换Job的资源
        log.info("正在由于转换成功释放用户资源");
        try {
            releaseHardWareResource(modelJob);
        } catch (Exception e) {
            log.error("转换成功释放用户资源异常, jobName: {}", modelJob == null ? null : modelJob.getName(), e);
        }
        log.info("由于转换成功释放用户资源");

        String folderPath = "convert_temp/" + modelJob.getId();
        asyncTaskService.asyncDeleteFolder(minioUtils.getBucketName(), folderPath);

        // 再触发转换任务自己的成功事件
        ModelJobStateMachine modelJobConvertStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobConvertStateMachine.modelJobConvertSucceededEvent(Integer.parseInt(String.valueOf(modelJob.getId())));
        // 创建转换成功通知
        createConvertNotification(modelJob, true);
//        log.info("转换成功 自动持久化日志");
//        logService.getLog(namespace, modelJob.getName(), "FORWARD", 99999L, null, null);
    }


    // 处理检查Job成功
    private void handleInspectJobSuccess(ModelJob modelJob) {
        modelJob.setStatus(ModelJob.INSPECT_SUCCEEDED);
        modelJobRepo.save(modelJob);
//        log.info("Inspect成功 自动持久化日志");
//        logService.getLog(namespace, modelJob.getName(), "FORWARD", 99999L, null, null);
    }

    // 处理训练Job失败 - 简化逻辑，直接释放资源
    private void handleTrainJobFailure(ModelJob modelJob) {
        // 直接释放资源，不再等待转换Job
        log.info("正在由于训练失败释放用户资源");
        try {
            releaseHardWareResource(modelJob);
        } catch (Exception e) {
            log.error("训练失败释放用户资源异常, jobName: {}", modelJob == null ? null : modelJob.getName(), e);
        }
        log.info("由于训练失败释放用户资源");


        String folderPath = "train_temp/" + modelJob.getId();
        try {
            List<String> files = minioUtils.listObjectsInFolder(bucketName, folderPath);
            if (!files.isEmpty()) {
                asyncTaskService.asyncDeleteFolder(minioUtils.getBucketName(), folderPath);
            } else {
                log.debug("文件夹不存在或已为空: {}", folderPath);
            }
        } catch (Exception e) {
            log.error("处理文件夹删除时发生错误: {}, 错误: {}", folderPath, e.getMessage());
        }

        // 创建训练失败通知
        createTrainNotification(modelJob, false);
        // 触发训练失败事件
        ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobStateMachine.modelJobTrainFailedEvent(Integer.parseInt(String.valueOf(modelJob.getId())));

//        log.info("训练失败 自动持久化日志");
//        logService.getLog(namespace, modelJob.getName(), "FORWARD", 99999L, null, null);
    }

    // 处理转换Job失败
    private void handleConvertJobFailure(ModelJob modelJob) {
        // 从转换任务名称中提取对应的训练任务名称
        String extractedName = extractJobName(modelJob.getName());
        // 使用截取后的名称查找对应的训练任务
        ModelJob starterJob = modelJobRepo.findModelJobByName(extractedName);

        // 注意：即使转换失败，仍然对训练任务触发成功事件（训练本身是成功的）
        ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobStateMachine.modelJobTrainSucceededEvent(Integer.parseInt(String.valueOf(starterJob.getId())));

        // 释放转换Job的资源
        log.info("正在由于转换失败释放用户资源");
        try {
            releaseHardWareResource(modelJob);
        } catch (Exception e) {
            log.error("转换失败释放用户资源异常, jobName: {}", modelJob == null ? null : modelJob.getName(), e);
        }
        log.info("由于转换失败释放用户资源");

        String folderPath = "convert_temp/" + modelJob.getId();
        asyncTaskService.asyncDeleteFolder(minioUtils.getBucketName(), folderPath);

        // 触发转换任务的失败事件
        ModelJobStateMachine modelJobConvertStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobConvertStateMachine.modelJobConvertFailedEvent(Integer.parseInt(String.valueOf(modelJob.getId())));

        //log.info("转换失败 自动持久化日志");
//        logService.getLog(namespace, modelJob.getName(), "FORWARD", 99999L, null, null);
        // 创建转换成功通知
        createConvertNotification(modelJob, false);
    }

    // 处理检查Job失败
    private void handleInspectJobFailure(ModelJob modelJob) {
        modelJob.setStatus(ModelJob.INSPECT_FAILED);
        modelJobRepo.save(modelJob);

//        log.info("Inspect 失败 自动持久化日志");
//        logService.getLog(namespace, modelJob.getName(), "FORWARD", 99999L, null, null);
    }

    /**
     * 释放cpu memory资源
     *
     * @param modelJob 任务
     */
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

            //这里的auth需要用集群内admin的auth。不能用current会出问题权限不够
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

    private void createTrainNotification(ModelJob modelJob, boolean isSuccess) {
        try {
            Optional<ModelGeneration> generationOpt = modelGenerationRepo.findById(modelJob.getModelGenerationId());
            if (!generationOpt.isPresent()) {
                return;
            }
            boolean isGuided = generationOpt.get().getIsGuided()!= null && generationOpt.get().getIsGuided();
            ModelGeneration generation = generationOpt.get();
            String operationType;
            if (isGuided) {
                operationType = isSuccess ?
                        NotificationOperationTypeEnum.GUIDED_TRAIN_SUCCESS.getCode() :
                        NotificationOperationTypeEnum.GUIDED_TRAIN_FAILED.getCode();
            } else {
                operationType = isSuccess ?
                        NotificationOperationTypeEnum.TRAIN_SUCCESS.getCode() :
                        NotificationOperationTypeEnum.TRAIN_FAILED.getCode();
            }
            // 构建 payload
            JSONObject payload = new JSONObject();
            payload.put("modelJobName", modelJob.getName());
            payload.put("generationName", generation.getName());
            payload.put("generationId", generation.getId());

            // 创建通知
            Notification notification = new Notification();
            notification.setToUserId(generation.getUserId());
            notification.setNotificationType(isSuccess ?
                    Notification.NotificationType.INFO : Notification.NotificationType.ERROR);
            notification.setOperationType(operationType);
            notification.setPayload(payload.toJSONString());
            notification.setReadStatus(0);
            Date now = new Date();
            notification.setCreateTime(now);
            notification.setUpdateTime(now);
            notification.setDeleted(false);

            dubheDataFeign.createAsync(dubheUtils.getAuthorization(),generation.getUserId(),isSuccess ?
                    Notification.NotificationType.INFO : Notification.NotificationType.ERROR,operationType,payload.toJSONString());
        } catch (Exception e) {
            log.error("创建训练通知失败: ", e);
        }
    }

    // 添加创建转换通知的辅助方法
    private void createConvertNotification(ModelJob modelJob, boolean isSuccess) {
        try {
            Optional<ModelGeneration> generationOpt = modelGenerationRepo.findById(modelJob.getModelGenerationId());
            if (!generationOpt.isPresent()) {
                return;
            }
            boolean isGuided = generationOpt.get().getIsGuided()!= null && generationOpt.get().getIsGuided();

            ModelGeneration generation = generationOpt.get();

            String operationType;
            if (isGuided) {
                operationType = isSuccess ?
                        NotificationOperationTypeEnum.GUIDED_CONVERT_SUCCESS.getCode() :
                        NotificationOperationTypeEnum.GUIDED_CONVERT_FAILED.getCode();
            } else {
                operationType = isSuccess ?
                        NotificationOperationTypeEnum.CONVERT_SUCCESS.getCode() :
                        NotificationOperationTypeEnum.CONVERT_FAILED.getCode();
            }
            // 构建 payload
            JSONObject payload = new JSONObject();
            payload.put("modelJobName", modelJob.getName());
            payload.put("generationName", generation.getName());
            payload.put("generationId", generation.getId());


            // 创建通知
            Notification notification = new Notification();
            notification.setToUserId(generation.getUserId());
            notification.setNotificationType(isSuccess ?
                    Notification.NotificationType.INFO : Notification.NotificationType.ERROR);
            notification.setOperationType(operationType);
            notification.setPayload(payload.toJSONString());
            notification.setReadStatus(0);
            Date now = new Date();
            notification.setCreateTime(now);
            notification.setUpdateTime(now);
            notification.setDeleted(false);

            dubheDataFeign.createAsync(dubheUtils.getAuthorization(),generation.getUserId(),isSuccess ?
                    Notification.NotificationType.INFO : Notification.NotificationType.ERROR,operationType,payload.toJSONString());
        } catch (Exception e) {
            log.error("创建转换通知失败: ", e);
        }
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

    private boolean isConvertError(ModelJob convertJob) {
        if (convertJob != null) {
            Map<String, String> logMap = logService.getLog(namespace, convertJob.getName(), "BACKWARD", Long.valueOf(100), null, null);
            if (logMap != null) {
                for (Map.Entry<String, String> entry : logMap.entrySet()) {
                    //System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
                    if (entry.getValue().contains("=[{Error}]=")) {
                        return true;
                    }
                }
            }

        }
        return false;
    }


    public void saveLogFile(Long modelJobId) {
        isFileExist();
        ModelJob modelJob = modelJobRepo.findModelJobById(modelJobId);
        Map<String, String> map = logService.getLog(namespace, modelJob.getName(), "BACKWARD", Long.valueOf("500"), Long.valueOf("1"), null);
        if (map != null) {
            String mapString = map.toString();
//            JSONArray resultArray = new JSONArray();
//            resultArray.addAll(map.entrySet());
//            System.out.println("resultArray:" + resultArray);
            if (modelJob.getJobType() == 1) {
                String filePath = "C:/Log/TRAIN/" + modelJob.getName() + ".txt";
                File file = new File(filePath);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(mapString);
                    writer.close();
                    System.out.println("日志存储成功");
                } catch (IOException e) {
                    System.out.println("日志存储失败");
                    throw new RuntimeException(e);
                }
            } else if (modelJob.getJobType() == 2) {
                String filePath = "C:/Log/INSPECT/" + modelJob.getName() + ".txt";
                File file = new File(filePath);
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write(mapString);
                    writer.close();
                    System.out.println("日志存储成功");
                } catch (IOException e) {
                    System.out.println("日志存储失败");
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void isFileExist() {
        String newFilePath1 = "C:/Log/TRAIN";
        String newFilePath2 = "C:/Log/INSPECT";
        File file1 = new File(newFilePath1);
        File file2 = new File(newFilePath2);
        if (!file1.exists()) {
            file1.mkdirs();
            System.out.println("F1");
        }
        if (!file2.exists()) {
            file2.mkdirs();
            System.out.println("F2");
        }
    }

    public void updateAllDeployMentStatus() {
        List<Deployment> deployments = modelDeploymentService.getAllDeployMent();
        if (deployments != null) {
            for (Deployment deployment : deployments) {
                if (deployment.getStatus() == Deployment.EXECUTING) {
                    if ((deploymentController.getDeploy(deployment.getDeploymentName())).getPayload() == null) {
                        continue;
                    }
                    System.out.println("TTTT" + (deploymentController.getDeploy(deployment.getDeploymentName())).getPayload());
                    if ((deploymentController.getDeploy(deployment.getDeploymentName())).getPayload() == 1) {
                        deployment.setStatus(1);
                        System.out.println("TTTTTTTTTTTT:" + deployment.getStatus());
                        System.out.println("FFFFFFFFF::" + deployment.getDeploymentName());
                        modelDeploymentRepo.save(deployment);
                    }
                }
            }
        }

    }

    public int getModelJobStatus(long modelJobId) {
        ModelJob modelJob = modelJobRepo.findModelJobById(modelJobId);
        return modelJob.getStatus();
    }

//    /**
//     * 更新 ModelGeneration 的状态
//     */
//    public void updateAllModelGeneration() {
//        // 更新所有 Job 的状态信息
//        this.updateAllJobStatus();
//        for (ModelGeneration modelGeneration : modelGenerationRepo.findAll()) {
//
//            if (modelGeneration.getStatus() == ModelGeneration.TRAINING) {
//                ModelJob modelJob = modelGeneration.getModelJobList().stream().max(Comparator.comparing(ModelJob::getCreateTime)).get();
//                if (modelJob.getStatus() == ModelJob.SUCCEEDED) {
//                    modelGeneration.setStatus(ModelGeneration.TRAIN_SUCCESS);
//                } else if (modelJob.getStatus() == ModelJob.FAILED) {
//                    modelGeneration.setStatus(ModelGeneration.TRAIN_FAIL);
//                }
//            }
//        }
//
//    }

//    private int getJobStatus(ModelJob modelJob) {
//        JSONObject statusJson = (JSONObject) JSON.toJSON(jobService.getJob(namespace, modelJob.getName()));
//        if (statusJson == null) {
//            modelJob.setStatus(ModelJob.FAILED);
//        } else if (StringUtils.isNotBlank(statusJson.getJSONObject("status").getString("succeeded")) && "1".equals(statusJson.getJSONObject("status").getString("succeeded"))) {
//            //训练完毕
//            modelJob.setStatus(ModelJob.SUCCEEDED);
//        } else if (StringUtils.isNotBlank(statusJson.getJSONObject("status").getString("failed")) && "1".equals(statusJson.getJSONObject("status").getString("failed"))) {
//            //训练完毕
//            modelJob.setStatus(ModelJob.FAILED);
//        }
//        modelJobRepo.save(modelJob);
//        return modelJob.getStatus();
//    }
}


