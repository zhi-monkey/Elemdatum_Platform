package org.dlut.adv.mineai.model.controller;

import org.dlut.adv.mineai.core.entity.MonitorModelConfig;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.MonitorModelConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/monitorModelConfig")
public class MonitorModelConfigController {

    @Resource
    private MonitorModelConfigService monitorModelConfigService;


    /**
     * 添加 monitorModelConfig 记录
     * @param modelName 算法名称
     *        monitorName 监控设备序列号 name
     *        payload 自定义配置
     * @return 是否插入成功
     */
    @RequestMapping("/addMonitorModelConfig")
    public Msg<Boolean> addMonitorModelConfig(String modelName, String monitorName, String payload) {
        boolean result = monitorModelConfigService.addMonitorModelConfig(modelName, monitorName, payload);
        if (result) {
            return new Msg<>(MsgCode.SUCCEED, true);
        } else {
            return new Msg<>(MsgCode.FAILED, false);
        }
    }

    @RequestMapping("/getMonitorModelConfigList")
    public Msg<List<MonitorModelConfig>> getMonitorModelConfigList() {
        List<MonitorModelConfig> monitorModelConfigList = monitorModelConfigService.getMonitorModelConfigList();
        return new Msg<>(MsgCode.SUCCEED, monitorModelConfigList);
    }

}
