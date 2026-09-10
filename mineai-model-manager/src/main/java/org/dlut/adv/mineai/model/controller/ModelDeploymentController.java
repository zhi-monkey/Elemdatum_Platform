package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.xml.bind.v2.TODO;
import io.kubernetes.client.util.ModelMapper;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.kubernetes.controller.DeploymentController;
import org.dlut.adv.mineai.model.kubernetes.service.DeploymentService;
import org.dlut.adv.mineai.model.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Gaozixuan
 */
@RequestMapping("/modelDeployment")
@RestController
public class ModelDeploymentController {
    @Resource
    private ModelDeploymentService modelDeploymentService;
    @Resource
    private ModelVersionService modelVersionService;
    @Resource
    private DeploymentController deploymentController;

    @Resource
    private ModelMonitorService modelMonitorService;

    @Resource
    private DeploymentService deploymentService;

    @Resource
    private ModelGenerationService modelGenerationService;

    @Value("${kubernetes.rootPath.weightRootPath}")
    private String weightRootPath;

    @Value("${kubernetes.namespace}")
    private String namespace;

    @Value("${zlm.rtsp.prefix}")
    private String rtsp;
    /**
     * 新增算法部署
     */
//    @RequestMapping("/addModelVersionDeployment")
//    public Msg<Long> addModelVersionDeployment(@RequestBody JSONObject modelDeployJson) {
//
//        Deployment deployment = new Deployment();
//        //获取Json中的参数
//        String namespace = modelDeployJson.getString("namespace");
//        deployment.setNamespace(namespace);
//        String deploymentName = modelDeployJson.getString("deploymentName");
//        deployment.setDeploymentName(deploymentName);
//        String image = modelDeployJson.getString("image");
//        deployment.setImage(image);
//        String weightPath = modelDeployJson.getString("weightPath");
//        deployment.setWeightPath(weightPath);
//        String weightRootPath = modelDeployJson.getString("weightRootPath");
//        deployment.setWeightRootPath(weightRootPath);
//        String label = modelDeployJson.getString("label");
//        deployment.setLabel(label);
//        String cpuNum = modelDeployJson.getString("cpuNum");
//        deployment.setCpuNum(cpuNum);
//        String memoryNum = modelDeployJson.getString("memoryNum");
//        deployment.setMemoryNum(memoryNum);
//        String gpuNum = modelDeployJson.getString("gpuNum");
//        deployment.setGpuNum(gpuNum);
//        deployment.setStatus(Deployment.EXECUTING);
////        int hostPort = modelDeployJson.getInteger("hostPort");
////        deployment.setHostPort(hostPort);
//        ModelVersion mv = JSON.toJavaObject(modelDeployJson.getJSONObject("modelVersion"), ModelVersion.class);
//        deployment.setModelVersion(mv);
//        Controller cc = JSON.toJavaObject(modelDeployJson.getJSONObject("controller"), Controller.class);
//        deployment.setController(cc);
//        Monitor mn = JSON.toJavaObject(modelDeployJson.getJSONObject("monitor"), Monitor.class);
//        deployment.setMonitor(mn);
//
//        Monitor monitor = modelMonitorService.getMonitorById(mn.getId());
//        Date nowDate = new Date();
//        String storagePath = "modelAlert/" + "sceneId-" + monitor.getScene().getId() + "/" + "monitorId-" + monitor.getId() + "/" + "modelVersionId-" + mv.getId();
//        String description = "部署 " + new SimpleDateFormat("yyyyMMdd").format(nowDate) + " 算法ID_" + mv.getId() + " 摄像头ID_" + mn.getId() + " 控制器ID_" + cc.getId();
//        deployment.setDescription(description);
//        Map<String, String> config = modelDeploymentService.saveConfig(mv.getId(), mn);
//        config.put("MODEL_WORKING_MODE", "3");
//        //生成Deployment数据
//        Msg msg = deploymentController.createDeployment(namespace, deploymentName, image, weightRootPath, weightPath, gpuNum, cpuNum, memoryNum, label, storagePath, config);
//        if (Objects.equals(msg.getCode(), MsgCode.SUCCEED.getCode())) {
//            modelDeploymentService.saveDeployment(deployment);
//            return new Msg<>(MsgCode.SUCCEED, deployment.getId());
//        }
//        return new Msg<>(MsgCode.FAILED);
//    }

//    /**
//     * 新增算法部署
//     */
//    @RequestMapping("/addModelDeployment")
//    public Msg<Long> addModelDeployment(@RequestBody JSONObject modelDeployJson) {
//
//        Model model = JSON.toJavaObject(modelDeployJson.getJSONObject("model"), Model.class);
//        ModelVersion modelVersion = modelVersionService.findBestOrBasicDeployMVByModel(model);
//        //获取controller
//        Controller cc = JSON.toJavaObject(modelDeployJson.getJSONObject("controller"), Controller.class);
//        Deployment deployment = new Deployment();
//        //获取Json中的参数
//        deployment.setNamespace(namespace);
//        String deploymentName = modelDeployJson.getString("deploymentName");
//        deployment.setDeploymentName(deploymentName);
//        //通过查询到的ModelVersion获取镜像url
//        String image = modelVersion.getUrl();
//        deployment.setImage(image);
//        //通过部署Model的DeployModelVersion获取到对应的权重文件路径
//        String weightPath = modelVersion.getWeightPath();
//        deployment.setWeightPath(weightPath);
//        deployment.setWeightRootPath(weightRootPath);
//        String label = cc.getLabel();
//        deployment.setLabel(label);
//        String cpuNum = modelDeployJson.getString("cpuNum");
//        deployment.setCpuNum(cpuNum);
//        String memoryNum = modelDeployJson.getString("memoryNum");
//        deployment.setMemoryNum(memoryNum);
//        String gpuNum = modelDeployJson.getString("gpuNum");
//        deployment.setGpuNum(gpuNum);
//        deployment.setStatus(Deployment.EXECUTING);
////        int hostPort = modelDeployJson.getInteger("hostPort");
////        deployment.setHostPort(hostPort);
//
//        deployment.setModelVersion(modelVersion);
//        deployment.setController(cc);
//        Monitor mn = JSON.toJavaObject(modelDeployJson.getJSONObject("monitor"), Monitor.class);
//        deployment.setMonitor(mn);
//
//        Monitor monitor = modelMonitorService.getMonitorById(mn.getId());
//        Date nowDate = new Date();
//        String storagePath = "modelAlert/" + "sceneId-" + monitor.getScene().getId() + "/" + "monitorId-" + monitor.getId() + "/" + "modelId-" + model.getId();
//        String description = "部署 " + new SimpleDateFormat("yyyyMMdd").format(nowDate) + " 算法ID_" + model.getId() + " 摄像头ID_" + mn.getId() + " 控制器ID_" + cc.getId();
//        deployment.setDescription(description);
//        Map<String, String> config = modelDeploymentService.saveConfig(model.getId(), mn);
//        config.put("MODEL_WORKING_MODE", "3");
//        //生成Deployment数据
//        Msg msg = deploymentController.createDeployment(deploymentName, image, weightRootPath, weightPath, gpuNum, cpuNum, memoryNum, label, storagePath, config);
//        if (Objects.equals(msg.getCode(), MsgCode.SUCCEED.getCode())) {
//            modelDeploymentService.saveDeployment(deployment);
//            return new Msg<>(MsgCode.SUCCEED, deployment.getId());
//        }
//        return new Msg<>(MsgCode.FAILED);
//    }

    /**
     * 新增算法部署
     */
    @RequestMapping("/addModelDeployment")
    public Msg<Long> addModelGenerationDeployment(@RequestBody JSONObject modelGenerationDeployJson) {
        // 获取model对象
        JSONObject modelJson = modelGenerationDeployJson.getJSONObject("model");
        // 从model对象中获取id
        long modelId = modelJson.getLong("id");
        // 获取model对象
        JSONObject monitorJson = modelGenerationDeployJson.getJSONObject("monitor");
        // 从model对象中获取id
        long monitorId = monitorJson.getLong("id");

        Model model = modelMonitorService.getModelByModelId(modelId);
        long generationId = model.getGenerationId();
        Monitor monitor = modelMonitorService.getMonitorById(monitorId);
        String deploymentName = modelGenerationDeployJson.getString("deploymentName");

        Deployment deployment = new Deployment();
        //获取Json中的参数
        deployment.setNamespace(namespace);
        deployment.setDeploymentName(deploymentName);
        ModelVersion deployModelVersion = model.getDeployModelVersion();

        //通过查询到的ModelVersion获取镜像url
        String image = deployModelVersion.getUrl();
        deployment.setImage(image);
        //通过部署Model的DeployModelVersion获取到对应的权重文件路径
        String weightPath = deployModelVersion.getWeightPath();
        deployment.setWeightPath(weightPath);
        deployment.setWeightRootPath(weightRootPath);
        //TODO 标签暂时写死
        deployment.setLabel("m1");
        String cpuNum = modelGenerationDeployJson.getString("cpuNum");
        deployment.setCpuNum(cpuNum);
        String memoryNum = modelGenerationDeployJson.getString("memoryNum");
        deployment.setMemoryNum(memoryNum);
        String gpuNum = modelGenerationDeployJson.getString("gpuNum");
        deployment.setGpuNum(gpuNum);
        deployment.setStatus(Deployment.EXECUTING);

        deployment.setModelVersion(deployModelVersion);
        Date nowDate = new Date();
        //TODO 报警路径
        String storagePath = "modelAlert/" + "sceneId-" + monitor.getScene().getId() + "/" + "monitorId-" + monitor.getId() + "/" + "modelId-" + model.getId();
        String description = "部署 " + new SimpleDateFormat("yyyyMMdd").format(nowDate) + " 算法ID_" + model.getId() + " 摄像头ID_" + monitor.getId();
        deployment.setDescription(description);
        String streamUrl = rtsp + "/" + "model_" + model.getId() + "/" + "monitor_" + monitorId;
        deployment.setStreamUrl(streamUrl);
        Map<String, String> config = modelDeploymentService.saveModelConfig(model, monitor, streamUrl);
        config.put("MODEL_WORKING_MODE", "3");
        //生成Deployment数据
        Msg msg = deploymentController.createDeployment(deploymentName, image, weightRootPath, weightPath, gpuNum, cpuNum, memoryNum, "m1", storagePath, config);
        if (Objects.equals(msg.getCode(), MsgCode.SUCCEED.getCode())) {
            modelDeploymentService.saveDeployment(deployment);
            return new Msg<>(MsgCode.SUCCEED, deployment.getId());
        }
        return new Msg<>(MsgCode.FAILED);
    }

    /**
     * 根据数据Id查询部署信息
     */
    @RequestMapping("/findModelDeploymentById")
    public Msg<Deployment> getModelDeploymentById(@RequestParam long id) {
        Deployment deployment = modelDeploymentService.findDeploymentById(id);
        return new Msg<>(MsgCode.SUCCEED, deployment);
    }

    /**
     * 根据算法Id查询部署信息
     */
    @RequestMapping("/findModelDeploymentByModelVersionId")
    public Msg<List<Deployment>> findModelDeploymentByModelVersionId(@RequestParam long modelVersionId) {
        ModelVersion modelVersion = modelVersionService.findModelVersionById(modelVersionId);
        List<Deployment> deployments = modelDeploymentService.findDeploymentsByModelVersion(modelVersion);
        return new Msg<>(MsgCode.SUCCEED, deployments);
    }

    /**
     * 取消算法部署
     */
    @RequestMapping("/deleteModelDeploymentById")
    public Msg<String> deleteDeployment(@RequestBody JSONObject jsonObject) {
        Long id = Long.valueOf(jsonObject.getString("id"));
        return modelDeploymentService.deleteDeployment(id);
    }
}
