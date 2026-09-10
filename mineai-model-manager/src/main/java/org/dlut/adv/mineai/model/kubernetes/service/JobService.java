package org.dlut.adv.mineai.model.kubernetes.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.constant.RedisConstants;
import org.dlut.adv.mineai.core.utils.RedisUtil;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.entity.TrainingJob;
import org.dlut.adv.mineai.model.domain.vo.ModelValidationVO;
import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.statusMachine.statemachine.StateMachineFactory;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class JobService {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    @Resource
    DubheDataFeign dubheDataFeign;
    @Resource
    DubheUtils dubheUtils;
    @Resource
    RedisUtil redisUtil;
    private volatile ApiClient apiClient;
    @Value("${kubernetes.config}")
    private String k8sConfig;

    // 用于存储正在排队的训练任务
    private final ConcurrentLinkedQueue<TrainingJob> jobQueue = new ConcurrentLinkedQueue<>();
    // 用于记录排队任务的数量
    private final AtomicInteger queuedTaskCount = new AtomicInteger(0);

    private ApiClient getApiClient() {
        if (apiClient != null) {
            return apiClient;
        }
        synchronized (this) {
            if (apiClient == null) {
                try {
                    apiClient = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(k8sConfig))).build();
                    // 如果报错可以解开看ApiException具体内容
                    // apiClient.setDebugging(true);
                } catch (Exception e) {
                    System.out.println("构建K8s-Client异常" + e);
                }
            }
        }
        return apiClient;
    }

    public V1Job createJob(String namespace, V1Job job) {
        // 无论任务的 spec 是否为空，加入训练任务队列并增加计数, job.getMetadata().getName().split("-").length == 4 用于判断是否为转换任务 普通任务job-local-cz-826执行后应等于4
        if (job.getMetadata() != null && job.getMetadata().getName() != null && job.getMetadata().getName().split("-").length == 4) {
            jobQueue.offer(new TrainingJob(job.getMetadata().getName(), namespace));
            queuedTaskCount.incrementAndGet();
        }

        ApiClient apiClient = getApiClient();
        BatchV1Api api = new BatchV1Api(apiClient);
        try {
            return api.createNamespacedJob(namespace, job, null, null, null);
        } catch (ApiException ex) {
            // 增强错误日志输出
            System.err.println("Kubernetes API Error:");
            System.err.println("HTTP status code: " + ex.getCode());
            System.err.println("Response headers: " + ex.getResponseHeaders());
            System.err.println("Response body: " + ex.getResponseBody());
            throw new RuntimeException("Job创建失败: " + ex.getResponseBody(), ex);
        }
    }

    public V1Status deleteJob(String namespace, String jobName) {
        ApiClient apiClient = getApiClient();
        BatchV1Api api = new BatchV1Api(apiClient);
        try {
            return api.deleteNamespacedJob(jobName, namespace, "true", null, null, null, "Foreground", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public V1Job getJob(String namespace, String jobName) {
        ApiClient apiClient = getApiClient();
        BatchV1Api api = new BatchV1Api(apiClient);
        try {
            return api.readNamespacedJob(jobName, namespace, "true", true, null);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 更新训练任务队列状态
     */
    @Scheduled(fixedRate = 5000)
    public void updateQueueStatus() {
        if (!jobQueue.isEmpty()) {
            ApiClient apiClient = getApiClient();
            BatchV1Api batchApi = new BatchV1Api(apiClient);
            CoreV1Api coreApi = new CoreV1Api(apiClient);

            List<TrainingJob> jobsToRemove = new ArrayList<>();

            for (TrainingJob trainingJob : jobQueue) {
                boolean isSchedulable = true;
                try {
                    V1Job job = batchApi.readNamespacedJob(trainingJob.getJobName(), trainingJob.getNamespace(), null, null, null);

                    if (job != null && job.getStatus() != null) {
                        V1PodList podList = coreApi.listNamespacedPod(trainingJob.getNamespace(), null, null, null, null,
                                "job-name=" + trainingJob.getJobName(), null, null, null, null, null);


                        for (V1Pod pod : podList.getItems()) {
                            String podName = Objects.requireNonNull(pod.getMetadata()).getName();
                            String podStatus = Objects.requireNonNull(pod.getStatus()).getPhase();

                            // 检查 Pods 是否 Unschedulable
                            if ("Pending".equals(podStatus) && pod.getStatus().getConditions() != null) {
                                for (V1PodCondition condition : pod.getStatus().getConditions()) {
                                    if ("PodScheduled".equals(condition.getType()) && "False".equals(condition.getStatus())) {
                                        isSchedulable = false;
                                        System.out.println("   * Pod " + podName + " is Unschedulable.");
                                    }
                                }
                            }
                        }
                    }

                    // 如果所有的 Pod 都是可调度的，则将 Job 标记为要移除
                    if (isSchedulable) {
                        jobsToRemove.add(trainingJob);
                    }
                } catch (ApiException e) {
                    log.info("Error occurred while checking job status: {}", e.getMessage());
                }
            }

            // 从 jobQueue 中移除所有标记为可调度的 Job
            jobQueue.removeAll(jobsToRemove);
            // 更新新的训练任务状态
            if (!CollectionUtils.isEmpty(jobsToRemove)) {
                for (TrainingJob job : jobsToRemove) {
                    // 状态转换
                    try {
                        String jobName = job.getJobName();
                        String[] parts = jobName.split("-");
                        // 只处理标准格式的 job 名称（job-xxx-xxx-{id}）
                        if (parts.length >= 4 && parts[3].matches("\\d+")) {
                            ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
                            modelJobStateMachine.modelJobTrainEvent(Integer.parseInt(parts[3]));
                        } else {
                            log.debug("跳过非标准格式的 Job 名称: {}", jobName);
                        }
                    } catch (Exception e) {
                        log.warn("处理 Job 状态转换失败: {}", job.getJobName(), e);
                    }
                }
            }
            System.out.println("Remaining jobs in queue: " + jobQueue);
        }
    }

    // 返回当前排队的训练任务数量
    public int getQueuedTaskCount() {
        return jobQueue.size();
    }

    // 从队列中移除任务
    public boolean removeFromQueue(String jobName) {
        for (TrainingJob trainingJob : jobQueue) {
            if (trainingJob.getJobName().equals(jobName)) {
                return jobQueue.remove(trainingJob);
            }
        }
        return false;
    }


    public boolean jobExists(String namespace, String jobName) {
        try {
            ApiClient client = getApiClient();
            BatchV1Api batchV1Api = new BatchV1Api(client);
            // 补全所有参数（最后三个参数设为null）
            batchV1Api.readNamespacedJob(jobName, namespace, null, null, null);
            return true;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 异步监听作业状态变化
     */
    @Async
    public void watchJobAsync(String namespace, String jobName, Long datasetId) {
        boolean jobCompleted = false;
        while (!jobCompleted) {
            try {
                Thread.sleep(5000); // 每5秒轮询一次
                V1Job job = getJob(namespace, jobName);

                if (job != null && job.getStatus() != null) {
                    V1JobStatus status = job.getStatus();

                    //先判断 conditions 状态字段
                    List<V1JobCondition> conditions = status.getConditions();
                    if (conditions != null) {
                        for (V1JobCondition condition : conditions) {
                            String type = condition.getType();
                            String conditionStatus = condition.getStatus();

                            if ("Failed".equals(type) && "True".equalsIgnoreCase(conditionStatus)) {
                                jobCompleted = true;
                                System.out.println("Job " + jobName + " has FAILED. Reason: " + condition.getReason());
                                dubheDataFeign.autoLabelEnd(dubheUtils.getAuthorization(), datasetId, true);
                                break;
                            } else if ("Complete".equals(type) && "True".equalsIgnoreCase(conditionStatus)) {
                                jobCompleted = true;
                                System.out.println("Job " + jobName + " has SUCCEEDED.");
                                dubheDataFeign.autoLabelEnd(dubheUtils.getAuthorization(), datasetId, false);
                                break;
                            }
                        }
                    }

                    // 检查失败/成功数量字段
                    if (!jobCompleted) {
                        Integer succeeded = status.getSucceeded();
                        Integer failed = status.getFailed();

                        if (succeeded != null && succeeded > 0) {
                            jobCompleted = true;
                            System.out.println("Job " + jobName + " has SUCCEEDED with " + succeeded + " pods.");
                            dubheDataFeign.autoLabelEnd(dubheUtils.getAuthorization(), datasetId, false);
                        } else if (failed != null && failed > 0) {
                            jobCompleted = true;
                            System.out.println("Job " + jobName + " has FAILED with failed count: " + failed);
                            dubheDataFeign.autoLabelEnd(dubheUtils.getAuthorization(), datasetId, true);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                System.err.println("Error while watching job: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * 异步监听作业状态变化 - 模型验证
     */
    @Async
    public void watchJobAsync(String namespace, String jobName, String key, ModelValidationVO modelValidationVO) {
        boolean jobCompleted = false;
        while (!jobCompleted) {
            try {
                // Check job status every 5 seconds
                Thread.sleep(5000);
                V1Job job = getJob(namespace, jobName);
                if (job != null) {
                    if (job.getStatus() != null) {
                        // 检查是否所有Pod都已成功完成
                        if (job.getStatus().getSucceeded() != null && job.getStatus().getSucceeded() > 0) {
                            jobCompleted = true;
                            System.out.println("Job " + jobName + " has succeeded with " + job.getStatus().getSucceeded() + " pods.");
                            // 更新zSet中验证状态,设置score为当前时间
                            redisUtil.zDel(key, modelValidationVO);
                            long currentTime = System.currentTimeMillis();
                            modelValidationVO.setFinishTime(currentTime);
                            redisUtil.zSet(key, modelValidationVO, currentTime, RedisConstants.CACHE_TTL, TimeUnit.MINUTES);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


}
