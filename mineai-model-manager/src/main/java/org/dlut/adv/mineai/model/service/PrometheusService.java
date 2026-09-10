package org.dlut.adv.mineai.model.service;

import lombok.RequiredArgsConstructor;
import org.dlut.adv.mineai.model.client.PrometheusFeign;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author mingming
 * @date 2024/11/15
 */
@Service
@RequiredArgsConstructor
public class PrometheusService {
    private final PrometheusFeign prometheusFeign;
    // CPU 查询相关常量
    private final static String CPU_USAGE_QUERY = "100 * (1 - avg(rate(node_cpu_seconds_total{mode=\"idle\"}[5m])) by (instance))";
    private final static String CPU_COUNT_QUERY = "count(node_cpu_seconds_total{mode=\"idle\"}) by (instance)";
    //使用率 × 总核心数 = 已使用的等效核心数向上取整
    private final static String CPU_USED_COUNT_QUERY =
            "ceil(count(node_cpu_seconds_total{mode=\"idle\"}) by (instance) * " +
                    "(1 - avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[5m]))))";

    // 内存查询相关常量
    private final static String MEMORY_TOTAL_QUERY = "node_memory_MemTotal_bytes";
    private final static String MEMORY_USED_QUERY = "node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes";

    // 磁盘查询相关常量
    private final static String DISK_TOTAL_QUERY = "node_filesystem_size_bytes";
    private final static String DISK_USED_QUERY = "node_filesystem_size_bytes - node_filesystem_avail_bytes";

    private final static String GPU_MEMORY_TOTAL_QUERY = "GPUDeviceMemoryLimit";
    private final static String GPU_MEMORY_USED_QUERY = "GPUDeviceMemoryAllocated";

    private final static String GPU_CORES_TOTAL_QUERY = "GPUDeviceCoreLimit";
    private final static String GPU_CORES_USED_QUERY = "GPUDeviceCoreAllocated";

    private final static String GPU_USAGE_QUERY = "DCGM_FI_DEV_GPU_UTIL[30m:3m]";


    public Map<String, Object> getCpuCount() {
        return prometheusFeign.getCpuCount(CPU_COUNT_QUERY);
    }

    public Map<String, Object> getCpuUsage() {
        return prometheusFeign.getCpuUsage(CPU_USAGE_QUERY);
    }
    public Map<String, Object> getCpuUsedCount() {
        return prometheusFeign.getCpuUsed(CPU_USED_COUNT_QUERY);
    }
    public Map<String, Object> getMemoryTotal() {
        return prometheusFeign.getMemoryTotal(MEMORY_TOTAL_QUERY);
    }
    public Map<String, Object> getMemoryUsed() {
        return prometheusFeign.getMemoryUsed(MEMORY_USED_QUERY);
    }
    public Map<String, Object> getDiskTotal() {
        return prometheusFeign.getDiskTotal(DISK_TOTAL_QUERY);
    }
    public Map<String, Object> getDiskUsed() {
        return prometheusFeign.getDiskUsed(DISK_USED_QUERY);
    }
    public Map<String, Object> getGpuMemoryTotal() {
        return prometheusFeign.getMemoryTotal(GPU_MEMORY_TOTAL_QUERY);
    }
    public Map<String, Object> getGpuMemoryUsed() {
        return prometheusFeign.getMemoryUsed(GPU_MEMORY_USED_QUERY);
    }
    public Map<String, Object> getGpuCoresTotal() {
        return prometheusFeign.getGpuCoresTotal(GPU_CORES_TOTAL_QUERY);
    }
    public Map<String, Object> getGpuCoresUsed() {
        return prometheusFeign.getGpuCoresTotal(GPU_CORES_USED_QUERY);
    }
    public Map<String, Object> getGpuUsage() {
        return prometheusFeign.getGpuUsage(GPU_USAGE_QUERY);
    }
}
