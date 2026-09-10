package org.dlut.adv.mineai.model.kubernetes.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.dlut.adv.mineai.core.entity.Controller;
import org.dlut.adv.mineai.core.entity.Deployment;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.model.repository.ModelDeploymentRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class DeploymentService {


    @Resource
    ModelDeploymentRepo modelDeploymentRepo;

    @Resource
    ModelJobRepo modelJobRepo;

    /**
     * @param k8sConfig 集群配置文件路径
     * @return 返回api客户端
     */
    public ApiClient getApiClient(String k8sConfig) {
        try {
            return ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(k8sConfig))).build();
        } catch (Exception e) {
            System.out.println("构建K8s-Client异常" + e);
        }
        return null;
    }


    public List<String> getIP(ApiClient apiClient) throws ApiException {
        CoreV1Api api = new CoreV1Api(apiClient);
        List<String> ips = new ArrayList<>();
        V1NodeList nodeList = api.listNode("true", null, null, null, null, null, null, null, null, null);

        System.out.println(nodeList.getItems());
        nodeList.getItems().forEach(node -> {
            String ip = node.getStatus().getAddresses().get(0).getAddress();
            ips.add(ip);
        });
        return ips;
    }


    /**
     * 获得当前节点的gpu总数量
     *
     * @param apiClient
     * @param nodeName
     * @return
     * @throws ApiException
     */
    public Integer getGpu(ApiClient apiClient, String nodeName) throws ApiException {
        CoreV1Api api = new CoreV1Api(apiClient);
        V1Node node = api.readNode(nodeName, null, null, null);
        V1NodeStatus status = node.getStatus();
        if (status != null) {
            if (status.getAllocatable().containsKey("aliyun.com/gpu-mem")) {
                return Integer.parseInt(String.valueOf(Objects.requireNonNull(status.getAllocatable()).get("aliyun.com/gpu-mem").getNumber()));
            } else {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 获得某个边缘控制器中剩余的gpu数量，这里认为controller.label 就是 nodeName
     *
     * @param apiClient
     * @param controller
     * @return
     * @throws ApiException
     */
    public int getGpuRemained(ApiClient apiClient, Controller controller) {
        List<Deployment> deployments = modelDeploymentRepo.findDeploymentsByController(controller);
        int gpuUsed = 0;
        for (Deployment deployment : deployments) {
            if (deployment.getGpuNum() != null) {
                gpuUsed = gpuUsed + Integer.parseInt(deployment.getGpuNum());
            }
        }
        List<ModelJob> modelJobs_T = modelJobRepo.findModelJobsByControllerAndStatus(controller, ModelJobStateCodeConstant.TRAINING);
        List<ModelJob> modelJobs_C = modelJobRepo.findModelJobsByControllerAndStatus(controller, ModelJobStateCodeConstant.CONVERTING);
        List<ModelJob> modelJobs_I = modelJobRepo.findModelJobsByControllerAndStatus(controller, ModelJob.INSPECTING);

        List<ModelJob> modelJobs = new ArrayList<>();

        modelJobs.addAll(modelJobs_T);
        modelJobs.addAll(modelJobs_C);
        modelJobs.addAll(modelJobs_I);

        for (ModelJob modelJob : modelJobs) {
            if (modelJob.getGpus() != null) {
                gpuUsed = gpuUsed + Integer.parseInt(modelJob.getGpus());
            }
        }
        try {
            return getGpu(apiClient, controller.getLabel()) - gpuUsed;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public V1Deployment createDeploy(ApiClient apiClient, String namespace, V1Deployment deployment) {
        AppsV1Api api = new AppsV1Api(apiClient);
        try {
            System.out.println(api);
            System.out.println("--------");
            return api.createNamespacedDeployment(namespace, deployment, "true", null, null);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public V1Status deleteDeploy(ApiClient apiClient, String namespace, String deployName) {
        AppsV1Api api = new AppsV1Api(apiClient);
        try {
            return api.deleteNamespacedDeployment(deployName, namespace, "true", null, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public V1Deployment getDeploy(ApiClient apiClient, String namespace, String deployName) {
        AppsV1Api api = new AppsV1Api(apiClient);
        try {
            return api.readNamespacedDeployment(deployName, namespace, "true", true, null);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return null;
    }
}
