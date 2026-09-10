package org.dlut.adv.mineai.model.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.domain.entity.MetricData;
import org.dlut.adv.mineai.model.dto.RecentGpuUsageDTO;
import org.dlut.adv.mineai.model.kubernetes.entity.NodeGpuInfo;
import org.dlut.adv.mineai.model.kubernetes.service.HamiMetricsService;
import org.dlut.adv.mineai.model.kubernetes.service.K8sClusterService;
import org.dlut.adv.mineai.model.service.GpuManagementService;
import org.dlut.adv.mineai.model.service.PrometheusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * @author mingming
 * @date 2024/11/15
 */
@Slf4j
@RestController
@RequestMapping("/prometheus")
@RequiredArgsConstructor
public class PrometheusController {
    private final PrometheusService prometheusService;
    private final K8sClusterService k8sClusterService;

    @Autowired
    private HamiMetricsService hamiMetricsService;
    
    @Autowired
    private GpuManagementService gpuManagementService;

    @GetMapping("/getComputingResource")
    public Msg<List<MetricData>> getComputingResource() {

        // 获取 Prometheus 数据
        Map<String, Object> cpuCountResponse = prometheusService.getCpuCount();
        Map<String, Object> cpuUsedResponse = prometheusService.getCpuUsedCount();
        Map<String, Object> cpuUsageResponse = prometheusService.getCpuUsage();

        Map<String, Object> memoryTotalResponse = prometheusService.getMemoryTotal();
        Map<String, Object> memoryUsedResponse = prometheusService.getMemoryUsed();

        Map<String, Object> diskTotalResponse = prometheusService.getDiskTotal();
        Map<String, Object> diskUsedResponse = prometheusService.getDiskUsed();

        // 解析数据并将每个 instance 的数据封装到 MetricData 对象中
        Map<String, MetricData> instanceMap = new HashMap<>();

        // 解析 CPU 总数
        parseMetricData(cpuCountResponse, (metricData, value) -> {
            metricData.setCpuTotal((String) value.get(1));
        }, instanceMap);

        // 解析 CPU 使用量
        parseMetricData(cpuUsedResponse, (metricData, value) -> {
            metricData.setCpuUsed((String) value.get(1));
        }, instanceMap);

        // 解析 CPU 使用率
        parseMetricData(cpuUsageResponse, (metricData, value) -> {
            metricData.setCpuUsage((String) value.get(1));
        }, instanceMap);

        // 解析 Memory 总量
        parseMetricData(memoryTotalResponse, (metricData, value) -> {
            metricData.setMemoryTotal((String) value.get(1));
        }, instanceMap);

        // 解析 Memory 使用量
        parseMetricData(memoryUsedResponse, (metricData, value) -> {
            metricData.setMemoryUsed((String) value.get(1));
        }, instanceMap);

        // 解析并累计 Disk 总量
        parseMetricData(diskTotalResponse, (metricData, value) -> {
            if (value != null && value.size() > 1) {
                String totalSize = (String) value.get(1);
                String instance = metricData.getInstance();

                // 获取当前 instance 的 MetricData，如果不存在则新建
                MetricData instanceData = instanceMap.computeIfAbsent(instance, k -> new MetricData());
                instanceData.setInstance(instance);

                // 累加磁盘总量
                instanceData.setDiskTotal(addResource(instanceData.getDiskTotal(), totalSize));
            }
        }, instanceMap);

        // 解析并累计 Disk 使用量
        parseMetricData(diskUsedResponse, (metricData, value) -> {
            if (value != null && value.size() > 1) {
                String usedSize = (String) value.get(1);
                String instance = metricData.getInstance();

                // 获取当前 instance 的 MetricData，如果不存在则新建
                MetricData instanceData = instanceMap.computeIfAbsent(instance, k -> new MetricData());
                instanceData.setInstance(instance);

                // 累加磁盘使用量
                instanceData.setDiskUsed(addResource(instanceData.getDiskUsed(), usedSize));
            }
        }, instanceMap);
        // 获取GPU信息并添加到MetricData中
        Map<String, NodeGpuInfo> gpuMetrics = hamiMetricsService.fetchGpuMetrics();
        
        // 将GPU信息添加到对应的节点
        for (Map.Entry<String, NodeGpuInfo> entry : gpuMetrics.entrySet()) {
            String nodeName = entry.getKey();
            NodeGpuInfo nodeGpuInfo = entry.getValue();
            
            // 查找对应的MetricData（通过节点名匹配）
            for (MetricData metricData : instanceMap.values()) {
                // instance格式可能是 "nodeName:port"，需要提取节点名
                String instanceNodeName = metricData.getInstance().split(":")[0];
                if (instanceNodeName.equals(nodeName)) {
                    metricData.setNodeGpuInfo(nodeGpuInfo);
                    break;
                }
            }
        }

        Map<String, String> nodeNameToIp = new HashMap<>();
        try {
            Map<String, org.dlut.adv.mineai.model.kubernetes.entity.NodeInfo> nodes = k8sClusterService.getAllNodes();
            if (nodes != null) {
                for (Map.Entry<String, org.dlut.adv.mineai.model.kubernetes.entity.NodeInfo> entry : nodes.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().getNodeIp() != null) {
                        nodeNameToIp.put(entry.getKey(), entry.getValue().getNodeIp());
                    }
                }
            }
        } catch (Exception ignored) {
        }

        for (MetricData metricData : instanceMap.values()) {
            if (metricData == null || metricData.getInstance() == null) {
                continue;
            }
            String host = metricData.getInstance().split(":")[0];
            if (isIpv4(host)) {
                metricData.setIp(host);
            } else {
                metricData.setIp(nodeNameToIp.get(host));
            }
        }

        // 将所有 instance 的数据添加到 metricDataList
        List<MetricData> metricDataList = new ArrayList<>(instanceMap.values());

        // System.out.println("MetricDataList = " + metricDataList);

        return new Msg<>(MsgCode.SUCCEED, metricDataList);
    }

    private boolean isIpv4(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        return host.matches("^(?:\\d{1,3}\\.){3}\\d{1,3}$");
    }

    /**
     * 获取最近的GPU 使用率
     * @return
     */
    @GetMapping("getRecentGpuUsage")
    public Msg<List<RecentGpuUsageDTO>> getRecentGpuUsage() {
        List<RecentGpuUsageDTO> recentGpuUsageDTOList = new ArrayList<>();

        Map<String, Object> gpuUsageResponse = prometheusService.getGpuUsage();
        Map<String, Object> data = (Map<String, Object>) gpuUsageResponse.get("data");
        if (data == null) {
            log.info("the data of gpuUsageResponse is null");
           return new Msg<>(MsgCode.SUCCEED, recentGpuUsageDTOList);
        }
        List<?> result = (List<?>) data.get("result");
        if (result == null) {
            log.info("the result of gpuUsageResponse's data is null");
            return new Msg<>(MsgCode.SUCCEED, recentGpuUsageDTOList);
        }
        result.forEach(item -> {
            // metric 内是键值对
            Map<String, String> metric = (Map<String, String>) ((Map<String, Object>) item).get("metric");
            // 分段时间拿到的 value 是二维数组
            List<List<Object>> values = (List<List<Object>>) ((Map<String, Object>) item).get("values");

            List<RecentGpuUsageDTO.UsageClass> usageList = new ArrayList<>();
            for (List<Object> value : values) {
                String timeStamp = String.valueOf(value.get(0)); // 获取时间戳
                String usage = String.valueOf(value.get(1)); // 获取使用率
                usageList.add(new RecentGpuUsageDTO.UsageClass(timeStamp, usage));
            }
            recentGpuUsageDTOList.add(RecentGpuUsageDTO.builder()
                    .gpuIndex(metric.get("gpu"))
                    .ip(metric.get("ip"))
                    .modelName(metric.get("modelName"))
                    .nodeName(metric.get("node_name"))
                    .recentUsage(usageList)
                    .build());
        });
        return new Msg<>(MsgCode.SUCCEED, recentGpuUsageDTOList);
    }

    /**
     * 获取 GPU 资源 仅返回当前时间点的信息
     * @return {@link Msg }<{@link List }<{@link MetricData }>>
     */
    @GetMapping("getGpuResource")
    public Msg<List<MetricData>> getGpuResource() {
        // 解析数据并将每个 instance 的数据封装到 MetricData 对象中
        Map<String, MetricData> instanceMap = new HashMap<>();

        Map<String, Object> gpuMemoryTotalResponse = prometheusService.getGpuMemoryTotal();
        Map<String, Object> gpuMemoryUsedResponse = prometheusService.getGpuMemoryUsed();

        Map<String, Object> gpuCoresUsedResponse = prometheusService.getGpuCoresUsed();
        Map<String, Object> gpuCoresTotalResponse = prometheusService.getGpuCoresTotal();

        // 解析并累计 GPU 内存总量
        parseMetricData(gpuMemoryTotalResponse, (metricData, value) -> {
            if (value != null && value.size() > 1) {
                String totalMemory = (String) value.get(1);
                String instance = metricData.getInstance();

                MetricData instanceData = instanceMap.computeIfAbsent(instance, k -> new MetricData());
                instanceData.setInstance(instance);

                // 累加 GPU 内存总量
                instanceData.setGpuMemoryTotal(addResource(instanceData.getGpuMemoryTotal(), totalMemory));
            }
        }, instanceMap);

        // 解析并累计 GPU 内存使用量
        parseMetricData(gpuMemoryUsedResponse, (metricData, value) -> {
            if (value != null && value.size() > 1) {
                String usedMemory = (String) value.get(1);
                String instance = metricData.getInstance();

                MetricData instanceData = instanceMap.computeIfAbsent(instance, k -> new MetricData());
                instanceData.setInstance(instance);

                // 累加 GPU 内存使用量
                instanceData.setGpuMemoryUsed(addResource(instanceData.getGpuMemoryUsed(), usedMemory));
            }
        }, instanceMap);

        // 解析并累计 GPU 核心使用情况
        parseMetricData(gpuCoresUsedResponse, (metricData, value) -> {
            if (value != null && value.size() > 1) {
                String usedCores = (String) value.get(1);
                String instance = metricData.getInstance();

                MetricData instanceData = instanceMap.computeIfAbsent(instance, k -> new MetricData());
                instanceData.setInstance(instance);

                // 累加已使用的 GPU 核心
                instanceData.setGpuCoresUsed(addResource(instanceData.getGpuCoresUsed(), usedCores));
            }
        }, instanceMap);

        // 解析并累计 GPU 核心总数
        parseMetricData(gpuCoresTotalResponse, (metricData, value) -> {
            if (value != null && value.size() > 1) {
                String totalCores = (String) value.get(1);
                String instance = metricData.getInstance();

                MetricData instanceData = instanceMap.computeIfAbsent(instance, k -> new MetricData());
                instanceData.setInstance(instance);

                // 累加 GPU 核心总数
                instanceData.setGpuCoresTotal(addResource(instanceData.getGpuCoresTotal(), totalCores));
            }
        }, instanceMap);
        return new Msg<>(MsgCode.SUCCEED, new ArrayList<>(instanceMap.values()));
    }

    /**
     * 一个通用的方法来解析 Prometheus 数据并填充 MetricData
     *
     * @param response Prometheus 数据
     * @param valueSetter 一个function 用于设置 MetricData 的值
     * @param instanceMap 用于存储 MetricData 的 Map<\instance, MetricData>
     */
    @SuppressWarnings("unchecked")
    private void parseMetricData(
            Map<String, Object> response,
            BiConsumer<MetricData, List<Object>> valueSetter,
            Map<String, MetricData> instanceMap
    ) {
        if (response == null || response.get("data") == null) {
            return;
        }

        Map<String, Object> responseData = (Map<String, Object>) response.get("data");
        List<Map<String, Object>> result = (List<Map<String, Object>>) responseData.get("result");

        for (Map<String, Object> item : result) {
            Map<String, Object> metric = (Map<String, Object>) item.get("metric");
            String instance = (String) metric.get("instance");

            List<Object> value = (List<Object>) item.get("value");
            MetricData metricData = instanceMap.computeIfAbsent(instance, k -> new MetricData());
            metricData.setInstance(instance);

            valueSetter.accept(metricData, value);
        }
    }

    /**
     * 用于累加磁盘大小的辅助方法
     *
     * @param currentSize    当前磁盘大小
     * @param additionalSize 需要增加的磁盘大小
     * @return {@link String }
     */
    private String addResource(String currentSize, String additionalSize) {
        if (currentSize == null) {
            return additionalSize;
        }
        try {
            long current = Long.parseLong(currentSize);
            long additional = Long.parseLong(additionalSize);
            return String.valueOf(current + additional);
        } catch (NumberFormatException e) {
            // 如果数据格式不正确，返回当前的值
            return currentSize;
        }
    }
    
    /**
     * 禁用指定的GPU
     * @param gpuUuid GPU的UUID
     * @return 操作结果
     */
    @PostMapping("/disableGpu")
    public Msg<String> disableGpu(@RequestParam String gpuUuid) {
        try {
            gpuManagementService.disableGpu(gpuUuid);
            return new Msg<>(MsgCode.SUCCEED, "GPU已禁用: " + gpuUuid);
        } catch (Exception e) {
            log.error("禁用GPU失败", e);
            return new Msg<>(MsgCode.FAILED, "禁用GPU失败: " + e.getMessage());
        }
    }
    
    /**
     * 启用指定的GPU
     * @param gpuUuid GPU的UUID
     * @return 操作结果
     */
    @PostMapping("/enableGpu")
    public Msg<String> enableGpu(@RequestParam String gpuUuid) {
        try {
            gpuManagementService.enableGpu(gpuUuid);
            return new Msg<>(MsgCode.SUCCEED, "GPU已启用: " + gpuUuid);
        } catch (Exception e) {
            log.error("启用GPU失败", e);
            return new Msg<>(MsgCode.FAILED, "启用GPU失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有禁用的GPU UUID列表
     * @return 禁用的GPU UUID集合
     */
    @GetMapping("/getDisabledGpus")
    public Msg<Set<String>> getDisabledGpus() {
        return new Msg<>(MsgCode.SUCCEED, gpuManagementService.getDisabledGpuUuids());
    }
}
