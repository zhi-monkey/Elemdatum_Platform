package org.dlut.adv.mineai.model.service;

import com.alibaba.fastjson.JSON;
import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.core.entity.MonitorModelConfig;
import org.dlut.adv.mineai.model.repository.ModelRepo;
import org.dlut.adv.mineai.model.repository.MonitorModelConfigRepo;
import org.dlut.adv.mineai.model.repository.MonitorRepo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author haoxiaoyang
 */
@Service
public class MonitorModelConfigService {
    @Resource
    MonitorModelConfigRepo monitorModelConfigRepo;

    @Resource
    private ModelRepo modelRepo;

    @Resource
    private MonitorRepo monitorRepo;

    public void saveMonitorModelConfig(MonitorModelConfig monitorModelConfig) {
        monitorModelConfigRepo.save(monitorModelConfig);
    }

    public MonitorModelConfig findMonitorModelConfigByMonitorIdAndModelId(Long modelId, Long monitorId) {
        return monitorModelConfigRepo.findMonitorModelConfigByModelIdAndMonitorId(modelId, monitorId);
    }

    public List<MonitorModelConfig> findMonitorModelConfigsByModelId(Long modelId) {
        List<MonitorModelConfig> monitorModelConfigList = monitorModelConfigRepo.findMonitorModelConfigsByModelId(modelId);
        return monitorModelConfigList;
    }


    public Boolean deleteMonitorModelConfigByMonitorAndModel(Long monitorId, Long modelId) {
        Model model = modelRepo.findModelById(modelId);
        Monitor monitor = monitorRepo.findMonitorById(monitorId);
        return monitorModelConfigRepo.deleteMonitorModelConfigByMonitorAndModel(monitor, model) > 0;
    }


    /**
     * 添加 monitorModelConfig 记录
     *
     * @param modelName 算法名称
     *                  monitorName 监控设备序列号 name
     *                  payload 自定义配置
     *                  streamUrl 推流地址
     * @return 是否添加成功
     */
    public boolean addMonitorModelConfig(String modelName, String monitorName, String payload) {
        Model model = modelRepo.findModelByModelName(modelName);
        // 将monitorName以‘-’分割，取第一个字符串作为monitorName
        String[] monitorNameArray = monitorName.split("-");
        Monitor monitor = monitorRepo.findMonitorByName(monitorNameArray[0]);
        // 解析 payload.
        Map<String, String> map = JSON.parseObject(payload, Map.class);
        if (model == null || monitor == null || map == null) {
            return false;
        }
        MonitorModelConfig monitorModelConfig =
                monitorModelConfigRepo.findMonitorModelConfigByModelIdAndMonitorId(model.getId(), monitor.getId());

        // 读取原先config
        Map<String, String> oldMap = monitorModelConfig.getCustomConfig();
        // 将新的config加入到原先的config中
        oldMap.putAll(map);
        // 将新的config存入数据库
        monitorModelConfig.setCustomConfig(oldMap);
        monitorModelConfigRepo.save(monitorModelConfig);

        return true;
    }

    public List<MonitorModelConfig> getMonitorModelConfigList() {
        return (List<MonitorModelConfig>) monitorModelConfigRepo.findAll();
    }
}
