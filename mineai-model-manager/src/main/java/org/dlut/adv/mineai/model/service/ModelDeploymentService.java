package org.dlut.adv.mineai.model.service;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.utils.FieldRename;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sunzhen
 */
@Service
public class ModelDeploymentService {

    @Resource
    ModelVersionRepo modelVersionRepo;

    @Resource
    ModelRepo modelRepo;

    @Resource
    ModelMonitorRepo modelMonitorRepo;

    @Resource
    MonitorRepo monitorRepo;

    @Value("${deployment.ip}")
    private String ip;

    @Value("${deployment.port}")
    private String port;
    FieldRename fieldRename = new FieldRename();

    @Resource
    ModelDeploymentRepo modelDeploymentRepo;


    /**
     *查询所有正在部署的deployment
     */
    public List<Deployment> getAllDeployMentByStatus(int status){
        return modelDeploymentRepo.findDeploymentsByStatus(status);
    }

    public List<Deployment> getAllDeployMent(){
        return modelDeploymentRepo.findAll();
    }

    /**
     * 根据Id查找Deployment
     */
    public Deployment findDeploymentById(long id) {
        return modelDeploymentRepo.findDeploymentById(id);
    }

    /**
     * 根据modelVersion查找Deployments
     */
    public List<Deployment> findDeploymentsByModelVersion(ModelVersion modelVersion) {
        return modelDeploymentRepo.findDeploymentsByModelVersion(modelVersion);
    }

    /**
     * 根据monitor查找Deployments
     */
    public List<Deployment> findDeploymentsByMonitor(Monitor monitor) {
        return modelDeploymentRepo.findDeploymentsByMonitor(monitor);
    }

    /**
     * 根据Controller查找Deployments
     */
    public List<Deployment> findDeploymentsByController(Controller controller) {
        return modelDeploymentRepo.findDeploymentsByController(controller);
    }

    /**
     * 判断部署是否存在
     */
    public Boolean isDeploymentExisted(Deployment deployment) {
        Deployment deployment1 = modelDeploymentRepo.findDeploymentByModelVersionAndControllerAndMonitor(deployment.getModelVersion(), deployment.getController(), deployment.getMonitor());
        if (deployment1 != null) {
            return false;
        }
        return true;
    }

    /**
     * 新增部署信息
     */

    public Msg<String> saveDeployment(Deployment deployment) {
        if (!isDeploymentExisted(deployment)) {
            return new Msg<>(MsgCode.DEPLOYMENT_EXITED);
        }
        modelDeploymentRepo.save(deployment);
        return new Msg<>(MsgCode.SUCCEED);
    }

    /**
     * 删除部署信息
     */
    public Msg<String> deleteDeployment(@RequestParam long id) {
        Deployment deployment1 = modelDeploymentRepo.findDeploymentById(id);
        modelDeploymentRepo.delete(deployment1);
        return new Msg<>(MsgCode.SUCCEED);
    }

    public Map<String, String> saveConfig(@RequestParam long modelId, @RequestBody Monitor monitor) {
        Monitor monitor1 = modelMonitorRepo.findMonitorById(monitor.getId());
        Model model = modelRepo.findModelById(modelId);
        Map<String, String> config = new HashMap<>();
        config.put("algName", model.getModelEnglishName());
        config.put("mainConfig", model.getMainConfig());
        config.put("monitorName", monitor1.getMonitorName());
        config.put("monitorUrl", monitor1.getProtocolMetaData());
        String streamUrl = modelRepo.findStreamUrlByModelIdAndMonitorId(modelId, monitor1.getId());
        config.put("streamUrl", streamUrl);
        long monitor_model_config_id = modelRepo.findCustomConfigIdByModelIdAndMonitorId(modelId, monitor1.getId());
        String videoName = modelRepo.findVideoNameByCustomConfigId(monitor_model_config_id);
        if (StringUtils.isNotBlank(videoName) && !"".equals(videoName)) {
            config.put("videoName", videoName);
        } else {
            String sceneName = modelRepo.findSceneNameBySceneId(monitor1.getScene().getId());
            config.put("videoName", sceneName + "-" + model.getModelEnglishName() + "-" + monitor1.getMonitorName());
        }
        List<Map<String, Object>> monitorModelConfigs = modelRepo.findMonitorModelConfigByCustomConfigId(monitor_model_config_id);
        JSONObject customConfig = new JSONObject();
        for (Map<String, Object> monitorModelConfig : monitorModelConfigs) {
            String key = (String) monitorModelConfig.get("custom_config_key");
            String value = (String) monitorModelConfig.get("custom_config");
            if ("areaLabel".equals(key)) {
                //针对感兴趣区域标注结果进行特殊处理
                //String转Object再下发
                customConfig.put(key, JSONObject.parseArray(value));
            } else {
                customConfig.put(key, value);
            }

        }
        config.put("customConfig", customConfig.toString());
        config.put("IP",ip);
        config.put("PORT",port);
        config.put("IMG_SAVE_PATH","/alert");
        return config;
    }

    public Map<String, String> saveModelGenerationConfig(@RequestBody ModelGeneration modelGeneration, @RequestBody Monitor monitor, @RequestParam String streamUrl) {

        Map<String, String> config = new HashMap<>();
        config.put("algName", modelGeneration.getName());
        config.put("monitorName", monitor.getMonitorName());
        config.put("monitorUrl", monitor.getProtocolMetaData());
        config.put("streamUrl", streamUrl);
        config.put("customConfig","{}");
        config.put("IP",ip);
        config.put("PORT",port);
        config.put("IMG_SAVE_PATH","/alert");
        System.out.println("config:" + config);
        return config;
    }


    public Map<String, String> saveModelConfig(@RequestBody Model model, @RequestBody Monitor monitor, @RequestParam String streamUrl) {

        Map<String, String> config = new HashMap<>();
        config.put("algName", model.getModelName());
        config.put("monitorName", monitor.getMonitorName());
        config.put("monitorUrl", monitor.getProtocolMetaData());
        config.put("streamUrl", streamUrl);
        config.put("customConfig","{}");
        config.put("IP",ip);
        config.put("PORT",port);
        config.put("IMG_SAVE_PATH","/alert");
        System.out.println("config:" + config);
        return config;
    }
    public Monitor findMonitorByMonitorId(long monitorId){
        Monitor monitor = monitorRepo.findMonitorById(monitorId);
        return monitor;
    }

}
