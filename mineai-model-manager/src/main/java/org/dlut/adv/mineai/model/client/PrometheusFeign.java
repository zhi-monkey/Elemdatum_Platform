package org.dlut.adv.mineai.model.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 用于请求prometheus
 * @author mingming
 * @date 2024/11/15
 */
@Component
@FeignClient(name = "prometheus-client", url = "${prometheus.ip}:${prometheus.port}/api/v1/query")
public interface PrometheusFeign {
    @GetMapping
    Map<String, Object> getCpuCount(@RequestParam("query") String query);

    @GetMapping
    Map<String, Object> getCpuUsage(@RequestParam("query") String query);

    @GetMapping
    Map<String, Object> getCpuUsed(@RequestParam("query") String query);

    @GetMapping
    Map<String, Object> getMemoryUsed(@RequestParam("query") String query);

    @GetMapping
    Map<String, Object> getMemoryTotal(@RequestParam("query") String query);

    @GetMapping
    Map<String, Object> getDiskUsed(@RequestParam("query") String query);

    @GetMapping
    Map<String, Object> getDiskTotal(@RequestParam("query") String query);
    @GetMapping
    Map<String, Object> getGpuCoresTotal(@RequestParam("query") String query);
    @GetMapping
    Map<String, Object> getGpuCoresUsed(@RequestParam("query") String query);

    @GetMapping
    Map<String, Object> getGpuUsage(@RequestParam("query") String query);
}
