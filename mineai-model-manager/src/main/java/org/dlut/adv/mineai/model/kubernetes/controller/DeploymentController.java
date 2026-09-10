package org.dlut.adv.mineai.model.kubernetes.controller;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.Controller;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.kubernetes.service.DeploymentService;
import org.dlut.adv.mineai.model.service.ModelControllerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.Path;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/deploy")
public class DeploymentController {

    @Value("${kubernetes.config}")
    private String k8sConfig;

    @Value("${harbor-server.url}")
    String harborUrl;

    @Value("${harbor-server.project}")
    String harborProject;

    @Autowired
    private DeploymentService deploymentService;


    @Autowired
    private ModelControllerService modelControllerService;

    @Value("${kubernetes.namespace}")
    private String namespace;

    /*
    获取集群所有节点的ip
     */
    @RequestMapping("/getIp")
    public Msg<List<String>> getIp() throws ApiException {
        List<String> ips = deploymentService.getIP(deploymentService.getApiClient(k8sConfig));
        return new Msg<>(MsgCode.SUCCEED, ips);
    }

    @RequestMapping("/getGpu")
    public Msg<Map<String, Integer>> getGpu(@RequestParam long controllerId) throws ApiException {
//        Controller controller = modelControllerService.findControllerById(controllerId);
//        Integer gpuNum = deploymentService.getGpu(deploymentService.getApiClient(k8sConfig), controller.getLabel());
//        Integer gpuRemained = deploymentService.getGpuRemained(deploymentService.getApiClient(k8sConfig), controller);
        Map<String, Integer> map = new HashMap<>(2);
        //TODO
        //为了演示，暂时先写死
        map.put("gpuNum", 22);
        map.put("gpuRemained", 20);
        return new Msg<>(MsgCode.SUCCEED, map);
    }

    /**
     * 获取所有节点中剩余GPU资源最多的节点的  剩余GPU/总GPU
     *
     * @return
     */
    @RequestMapping("/getGpuMax")
    public Msg<Map<String, String>> getGpuMax() {
        List<Controller> controllers = modelControllerService.findControllersByStatusAndStatusUsingAndArchitecture();
        Optional<Controller> max = controllers.stream().max((c1, c2) ->
                deploymentService.getGpuRemained(deploymentService.getApiClient(k8sConfig), c1) - deploymentService.getGpuRemained(deploymentService.getApiClient(k8sConfig), c2));
        Controller controller = max.isPresent() ? max.get() : null;
        //如果没有可用的节点，则返回0，0
        if (controller == null) {
            Map<String, String> map = new HashMap<>(2);
            map.put("gpuRemained", "0");
            map.put("gpuNum", "0");
            map.put("controllerName", "暂无可用控制器");
            return new Msg<>(MsgCode.SUCCEED, map);
        }
        Integer gpuNum;
        try {
            gpuNum = deploymentService.getGpu(deploymentService.getApiClient(k8sConfig), controller.getLabel());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        Integer gpuRemained = deploymentService.getGpuRemained(deploymentService.getApiClient(k8sConfig), controller);
        Map<String, String> map = new HashMap<>(3);
        map.put("gpuRemained", String.valueOf(gpuRemained));
        map.put("gpuNum", String.valueOf(gpuNum));
        map.put("controllerName", controller.getName());
        return new Msg<>(MsgCode.SUCCEED, map);
    }

    /**
     * 创建部署
     *
     * @param deploymentName
     * @param image
     * @param weightRootPath
     * @param weightPath
     * @param gpuNum
     * @param cpuNum
     * @param memoryNum
     * @param label
     * @param storagePath
     * @param config
     * @return
     */
    @RequestMapping("/createDeployment")
    public Msg<String> createDeployment(String deploymentName, String image, String weightRootPath, String weightPath, String gpuNum, String cpuNum, String memoryNum, String label, String storagePath, @RequestBody Map<String, String> config) {
//        if(jobService.getJob(jobService.getApiClient(k8sConfig),namespace, jobName)!=null){
//            return null;
//        }
        System.out.println("namespace:" + namespace);
        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);
        //挂载卷声明  使用pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName("file-data");

        List<V1Volume> volumes = new ArrayList<>();
        V1Volume pvcVolume = new V1Volume();
        pvcVolume.setName("file-data");
        pvcVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(pvcVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();

        if (StringUtils.isNotBlank(weightPath)) {
            V1VolumeMount weightMount = new V1VolumeMount();
            weightMount.setName("file-data");
            weightMount.setMountPath("/weight");
            //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
            weightMount.setSubPathExpr(weightRootPath + "/" + weightPath);
            volumeMounts.add(weightMount);
        }

        V1VolumeMount alertMount = new V1VolumeMount();
        alertMount.setName("file-data");
        alertMount.setMountPath("/alert");
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        alertMount.setSubPathExpr(storagePath);
        volumeMounts.add(alertMount);

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : config.keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(config.get(key));
            envVars.add(v1EnvVar);
        }

//        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();

        if (StringUtils.isNotBlank(cpuNum)) {
            Quantity cpuQuantity = new Quantity(cpuNum);
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(memoryNum)) {
            Quantity memoryQuantity = new Quantity(memoryNum);
            limits.put("memory", memoryQuantity);
        }

        v1ResourceRequirements.setLimits(limits);

        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("app", deploymentName);
        // ports
        List<V1ContainerPort> portList = new ArrayList<>();
        V1ContainerPort port = new V1ContainerPort();
        port.setContainerPort(80);
        portList.add(port);

        V1Affinity v1Affinity = new V1Affinity();
        V1NodeAffinity v1NodeAffinity = new V1NodeAffinity();
        V1NodeSelector v1NodeSelector = new V1NodeSelector();

        List<V1NodeSelectorTerm> v1NodeSelectorTerms = new ArrayList<>();
        V1NodeSelectorTerm v1NodeSelectorTerm = new V1NodeSelectorTerm();


        List<V1NodeSelectorRequirement> matchExpressions = new ArrayList<>();
        V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
        nodeSelectorRequirement.setKey("kubernetes.io/hostname");
        nodeSelectorRequirement.setOperator("In");
        nodeSelectorRequirement.setValues(Collections.singletonList(label));
        matchExpressions.add(nodeSelectorRequirement);
        v1NodeSelectorTerm.setMatchExpressions(matchExpressions);

        v1NodeSelectorTerms.add(v1NodeSelectorTerm);
        v1NodeSelector.setNodeSelectorTerms(v1NodeSelectorTerms);
        v1NodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(v1NodeSelector);
        v1Affinity.setNodeAffinity(v1NodeAffinity);

        V1Deployment deployment;
        deployment = new V1DeploymentBuilder()
                .withApiVersion("apps/v1")
                .withKind("Deployment")
                .withNewMetadata()
                .withName(deploymentName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()

                .withReplicas(1)
                .withNewSelector()
                .withMatchLabels(matchLabels)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(matchLabels)
                .endMetadata()
                .withNewSpec()
                .withAffinity(v1Affinity)
                .withContainers(new V1Container()
                        .name(deploymentName)
                        .image(fullImageName)
                        .imagePullPolicy("IfNotPresent")
                        .volumeMounts(volumeMounts)
                        .env(envVars).resources(v1ResourceRequirements)
                        .ports(portList))
                .withVolumes(volumes)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        deploymentService.createDeploy(deploymentService.getApiClient(k8sConfig), namespace, deployment);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/createOnlineInspection")
    public Msg<String> createOnlineInspection(String deploymentName, String image, String weightRootPath, String weightPath, String gpuNum, String cpuNum, String memoryNum, String storagePath, @RequestBody Map<String, String> config) {
//        if(jobService.getJob(jobService.getApiClient(k8sConfig),namespace, jobName)!=null){
//            return null;
//        }
        System.out.println("namespace:" + namespace);
        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);
        //挂载卷声明  使用pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName("file-data");

        List<V1Volume> volumes = new ArrayList<>();
        V1Volume pvcVolume = new V1Volume();
        pvcVolume.setName("file-data");
        pvcVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(pvcVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();

        if (StringUtils.isNotBlank(weightPath)) {
            V1VolumeMount weightMount = new V1VolumeMount();
            weightMount.setName("file-data");
            weightMount.setMountPath("/weight");
            //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
            weightMount.setSubPathExpr(weightRootPath + "/" + weightPath);
            volumeMounts.add(weightMount);
        }

        V1VolumeMount alertMount = new V1VolumeMount();
        alertMount.setName("file-data");
        alertMount.setMountPath("/alert");
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        alertMount.setSubPathExpr(storagePath);
        volumeMounts.add(alertMount);

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : config.keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(config.get(key));
            envVars.add(v1EnvVar);
        }

//        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();

        if (StringUtils.isNotBlank(cpuNum)) {
            Quantity cpuQuantity = new Quantity(cpuNum);
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(memoryNum)) {
            Quantity memoryQuantity = new Quantity(memoryNum);
            limits.put("memory", memoryQuantity);
        }

        v1ResourceRequirements.setLimits(limits);


        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("app", deploymentName);
        // ports
        List<V1ContainerPort> portList = new ArrayList<>();
        V1ContainerPort port = new V1ContainerPort();
        port.setContainerPort(80);
        portList.add(port);

        V1Deployment deployment;
        deployment = new V1DeploymentBuilder()
                .withApiVersion("apps/v1")
                .withKind("Deployment")
                .withNewMetadata()
                .withName(deploymentName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewSelector()
                .withMatchLabels(matchLabels)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(matchLabels)
                .endMetadata()
                .withNewSpec()
                .withContainers(new V1Container()
                        .name(deploymentName)
                        .image(fullImageName)
                        .imagePullPolicy("IfNotPresent")
                        .volumeMounts(volumeMounts)
                        .env(envVars).resources(v1ResourceRequirements)
                        .ports(portList))
                .withVolumes(volumes)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
        deploymentService.createDeploy(deploymentService.getApiClient(k8sConfig), namespace, deployment);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/deleteDeploy")
    public Msg<String> deleteDeploy(@RequestParam String deployName) {
        V1Status v1Status = deploymentService.deleteDeploy(deploymentService.getApiClient(k8sConfig), namespace, deployName);
        if (v1Status == null) {
            return new Msg<>(MsgCode.FAILED);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/updateDeploy")
    public Msg<String> updateDeploy(String deploymentName, String image, String weightRootPath, String weightPath, String gpuNum, String cpuNum, String memoryNum, String label, String storgePath, @RequestBody Map<String, String> config) {
        deleteDeploy(deploymentName);
        createDeployment(deploymentName, image, weightRootPath, weightPath, gpuNum, cpuNum, memoryNum, label, storgePath, config);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/getDeploy")
    public Msg<Integer> getDeploy(String deployName) {
        V1Deployment v1Deployment = deploymentService.getDeploy(deploymentService.getApiClient(k8sConfig), namespace, deployName);
        return new Msg<>(MsgCode.SUCCEED, v1Deployment.getStatus().getReadyReplicas());
    }
}
