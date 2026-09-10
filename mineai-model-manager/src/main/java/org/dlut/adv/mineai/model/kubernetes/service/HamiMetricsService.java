package org.dlut.adv.mineai.model.kubernetes.service;

import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.model.kubernetes.entity.GpuInfo;
import org.dlut.adv.mineai.model.kubernetes.entity.K8sConfig;
import org.dlut.adv.mineai.model.kubernetes.entity.NodeGpuInfo;
import org.dlut.adv.mineai.model.kubernetes.util.PrometheusMetricsParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HamiMetricsService {

    @Autowired
    private K8sConfig k8sConfig;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 从 HAMi metrics endpoint 获取并解析 GPU 信息
     */
    public Map<String, NodeGpuInfo> fetchGpuMetrics() {
        try {
            // 构建 HAMi metrics URL
            String metricsUrl = String.format("http://%s:%d/metrics",
                    k8sConfig.getIp(),
                    k8sConfig.getHamiPort()); // HAMi metrics 端口

            log.debug("开始获取 HAMi metrics: {}", metricsUrl);

            String metricsText = restTemplate.getForObject(metricsUrl, String.class);

            if (metricsText == null || metricsText.isEmpty()) {
                log.warn("HAMi metrics 返回空数据");
                return new HashMap<>();
            }

            return parseGpuMetrics(metricsText);

        } catch (Exception e) {
            log.error("获取 HAMi metrics 失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 解析 GPU metrics 数据
     */
    private Map<String, NodeGpuInfo> parseGpuMetrics(String metricsText) {
        Map<String, List<PrometheusMetricsParser.MetricData>> metricsMap =
                PrometheusMetricsParser.parse(metricsText);

        // 按节点组织 GPU 信息
        Map<String, NodeGpuInfo> nodeGpuMap = new HashMap<>();

        // 解析 nodeGPUOverview - 包含 GPU 基本信息
        List<PrometheusMetricsParser.MetricData> overviewMetrics =
                metricsMap.get("nodeGPUOverview");

        if (overviewMetrics != null) {
            for (PrometheusMetricsParser.MetricData metric : overviewMetrics) {
                String nodeName = metric.getLabel("nodeid");
                String deviceUuid = metric.getLabel("deviceuuid");
                String deviceIdx = metric.getLabel("deviceidx");
                String deviceType = metric.getLabel("devicetype");
                String deviceMemoryLimit = metric.getLabel("devicememorylimit");
                String sharedContainers = metric.getLabel("sharedcontainers");

                // 获取或创建节点 GPU 信息
                NodeGpuInfo nodeGpuInfo = nodeGpuMap.computeIfAbsent(
                        nodeName,
                        k -> new NodeGpuInfo()
                );
                nodeGpuInfo.setNodeName(nodeName);

                // 创建 GPU 信息
                GpuInfo gpuInfo = new GpuInfo();
                gpuInfo.setGpuUUID(deviceUuid);
                gpuInfo.setGpuIndex(Integer.parseInt(deviceIdx));
                gpuInfo.setGpuModel(parseDeviceType(deviceType));
                gpuInfo.setTotalMemory(Long.parseLong(deviceMemoryLimit) * 1024 * 1024); // MiB to bytes
                gpuInfo.setHealth("Healthy");

                // 判断是否为 vGPU

                nodeGpuInfo.getGpus().add(gpuInfo);
            }
        }

        // 解析 GPUDeviceMemoryAllocated - GPU 已分配显存
        List<PrometheusMetricsParser.MetricData> memoryAllocatedMetrics =
                metricsMap.get("GPUDeviceMemoryAllocated");

        if (memoryAllocatedMetrics != null) {
            for (PrometheusMetricsParser.MetricData metric : memoryAllocatedMetrics) {
                String nodeName = metric.getLabel("nodeid");
                String deviceUuid = metric.getLabel("deviceuuid");
                double allocatedMemory = metric.getValue();

                NodeGpuInfo nodeGpuInfo = nodeGpuMap.get(nodeName);
                if (nodeGpuInfo != null) {
                    GpuInfo gpu = findGpuByUuid(nodeGpuInfo.getGpus(), deviceUuid);
                    if (gpu != null) {
                        gpu.setUsedMemory((long) allocatedMemory);
                    }
                }
            }
        }

        // 解析 GPUDeviceCoreLimit - GPU 总核心数
        List<PrometheusMetricsParser.MetricData> coreLimitMetrics =
                metricsMap.get("GPUDeviceCoreLimit");

        if (coreLimitMetrics != null) {
            for (PrometheusMetricsParser.MetricData metric : coreLimitMetrics) {
                String nodeName = metric.getLabel("nodeid");
                String deviceUuid = metric.getLabel("deviceuuid");
                int totalCores = (int) metric.getValue(); // Prometheus value 即为 core limit

                NodeGpuInfo nodeGpuInfo = nodeGpuMap.get(nodeName);
                if (nodeGpuInfo != null) {
                    GpuInfo gpu = findGpuByUuid(nodeGpuInfo.getGpus(), deviceUuid);
                    if (gpu != null) {
                        gpu.setTotalCores(totalCores);
                    }
                }
            }
        }


        // 解析 GPUDeviceCoreAllocated - GPU 已分配核心
        List<PrometheusMetricsParser.MetricData> coreAllocatedMetrics =
                metricsMap.get("GPUDeviceCoreAllocated");

        if (coreAllocatedMetrics != null) {
            for (PrometheusMetricsParser.MetricData metric : coreAllocatedMetrics) {
                String nodeName = metric.getLabel("nodeid");
                String deviceUuid = metric.getLabel("deviceuuid");
                int allocatedCores = (int) metric.getValue();

                NodeGpuInfo nodeGpuInfo = nodeGpuMap.get(nodeName);
                if (nodeGpuInfo != null) {
                    GpuInfo gpu = findGpuByUuid(nodeGpuInfo.getGpus(), deviceUuid);
                    if (gpu != null) {
                        gpu.setUsedCores(allocatedCores);
                    }
                }
            }
        }

        // 解析 GPUDeviceSharedNum - GPU 共享容器数
        List<PrometheusMetricsParser.MetricData> sharedNumMetrics =
                metricsMap.get("GPUDeviceSharedNum");

        if (sharedNumMetrics != null) {
            for (PrometheusMetricsParser.MetricData metric : sharedNumMetrics) {
                String nodeName = metric.getLabel("nodeid");
                String deviceUuid = metric.getLabel("deviceuuid");
                // 可以将共享数添加到 GpuInfo 的扩展字段中
            }
        }

        // 计算每个 GPU 的使用率
        for (NodeGpuInfo nodeGpuInfo : nodeGpuMap.values()) {
            for (GpuInfo gpu : nodeGpuInfo.getGpus()) {
                gpu.calculatePercents();
            }
            nodeGpuInfo.calculateTotals();
        }

        //log.info("解析 HAMi metrics 完成，共 {} 个节点", nodeGpuMap.size());

        return nodeGpuMap;
    }

    /**
     * 根据 UUID 查找 GPU
     */
    private GpuInfo findGpuByUuid(List<GpuInfo> gpus, String uuid) {
        return gpus.stream()
                .filter(gpu -> uuid.equals(gpu.getGpuUUID()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析设备类型
     */
    private String parseDeviceType(String deviceType) {
        if (deviceType == null) {
            return "Unknown";
        }
        // 格式: NVIDIA-NVIDIA GeForce RTX 2080 Ti
        String[] parts = deviceType.split("-", 2);
        return parts.length > 1 ? parts[1] : deviceType;
    }

    /**
     * 获取 Pod 级别的 GPU 使用信息
     */
    public List<PodGpuUsage> fetchPodGpuUsage() {
        try {
            String metricsUrl = String.format("http://%s:%d/metrics",
                    k8sConfig.getIp(), k8sConfig.getHamiPort());

            String metricsText = restTemplate.getForObject(metricsUrl, String.class);

            if (metricsText == null || metricsText.isEmpty()) {
                return new ArrayList<>();
            }

            return parsePodGpuUsage(metricsText);

        } catch (Exception e) {
            log.error("获取 Pod GPU 使用信息失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析 Pod GPU 使用信息
     */
    private List<PodGpuUsage> parsePodGpuUsage(String metricsText) {
        List<PodGpuUsage> podUsages = new ArrayList<>();

        Map<String, List<PrometheusMetricsParser.MetricData>> metricsMap =
                PrometheusMetricsParser.parse(metricsText);

        // 解析 vGPUPodsDeviceAllocated
        List<PrometheusMetricsParser.MetricData> podMetrics =
                metricsMap.get("vGPUPodsDeviceAllocated");

        if (podMetrics != null) {
            for (PrometheusMetricsParser.MetricData metric : podMetrics) {
                PodGpuUsage usage = new PodGpuUsage();
                usage.setPodName(metric.getLabel("podname"));
                usage.setPodNamespace(metric.getLabel("podnamespace"));
                usage.setNodeName(metric.getLabel("nodename"));
                usage.setDeviceUuid(metric.getLabel("deviceuuid"));
                usage.setContainerIdx(metric.getLabel("containeridx"));
                usage.setUsedCore(Integer.parseInt(metric.getLabel("deviceusedcore")));
                usage.setUsedMemory((long) metric.getValue());

                podUsages.add(usage);
            }
        }

        // 补充 vGPUMemoryPercentage 信息
        List<PrometheusMetricsParser.MetricData> memPercentMetrics =
                metricsMap.get("vGPUMemoryPercentage");

        if (memPercentMetrics != null) {
            for (PrometheusMetricsParser.MetricData metric : memPercentMetrics) {
                String podName = metric.getLabel("podname");
                String deviceUuid = metric.getLabel("deviceuuid");
                double memoryPercent = metric.getValue();

                podUsages.stream()
                        .filter(p -> p.getPodName().equals(podName) &&
                                p.getDeviceUuid().equals(deviceUuid))
                        .findFirst()
                        .ifPresent(p -> p.setMemoryPercent(memoryPercent * 100));
            }
        }

        // 补充 vGPUCorePercentage 信息
        List<PrometheusMetricsParser.MetricData> corePercentMetrics =
                metricsMap.get("vGPUCorePercentage");

        if (corePercentMetrics != null) {
            for (PrometheusMetricsParser.MetricData metric : corePercentMetrics) {
                String podName = metric.getLabel("podname");
                String deviceUuid = metric.getLabel("deviceuuid");
                double corePercent = metric.getValue();

                podUsages.stream()
                        .filter(p -> p.getPodName().equals(podName) &&
                                p.getDeviceUuid().equals(deviceUuid))
                        .findFirst()
                        .ifPresent(p -> p.setCorePercent(corePercent));
            }
        }

        return podUsages;
    }

    /**
     * Pod GPU 使用信息
     */
    @lombok.Data
    public static class PodGpuUsage {
        private String podName;
        private String podNamespace;
        private String nodeName;
        private String deviceUuid;
        private String containerIdx;
        private Integer usedCore;
        private Long usedMemory;
        private Double memoryPercent;
        private Double corePercent;
    }
}
