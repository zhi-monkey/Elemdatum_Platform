package org.dlut.adv.mineai.model.service;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.model.repository.ModelMonitorRepo;
import org.dlut.adv.mineai.model.repository.ModelRepo;
import org.dlut.adv.mineai.model.repository.MonitorModelConfigRepo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * @author haoxiaoyang
 */
@Service
public class ModelMonitorService {
    @Resource
    ModelMonitorRepo modelMonitorRepo;

    @Resource
    ModelRepo modelRepo;

    @Resource
    MonitorModelConfigRepo monitorModelConfigRepo;

    /**
     * 保存监控设备
     * @param monitor
     * @return
     */
    public boolean save(Monitor monitor) {
        if (StringUtils.isBlank(monitor.getName())) {
            return false;   //编号为空
        }
        return modelMonitorRepo.save(monitor) != null;
    }
    /**
     * 根据monitorId查询拿到对应的monitor
     *
     * @param monitorId
     * @return
     */
    public Monitor getMonitorById(Long monitorId) {
        Optional<Monitor> optional = modelMonitorRepo.findById(monitorId);
        return optional.orElse(null);
    }

    public void deleteMonitorAndModel(Long modelId){
        Model model = modelRepo.findModelById(modelId);
        monitorModelConfigRepo.deleteMonitorModelConfigByModel(model);
    }

    public Model getModelByModelId(Long modelId){
       return modelRepo.findModelById(modelId);
    }



}
