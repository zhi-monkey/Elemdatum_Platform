package org.dlut.adv.mineai.model.openfeign;

import org.dlut.adv.mineai.core.entity.Monitor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(value = "mineai-monitor-accessor")
public interface ModelAlertMonitorFeign {

    /*
    feign调用 根据monitorName返回monitor
    */
    @RequestMapping("/monitor/findMonitorByMonitorName")
    Monitor findMonitorByMonitorName(@RequestParam String monitorName);

}
