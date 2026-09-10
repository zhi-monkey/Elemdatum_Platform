package org.dlut.adv.mineai.model.kubernetes.service;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.custom.Quantity;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.model.domain.entity.MetricData;
import org.dlut.adv.mineai.model.kubernetes.entity.*;
import org.dlut.adv.mineai.model.service.PrometheusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class K8sClusterService {

    @Autowired
    private K8sConfig k8sConfig;

    @Autowired
    private HamiMetricsService hamiMetricsService;
    @Autowired
    private PrometheusService prometheusService;

    private ClusterInfo clusterInfo;
    private static final int MAX_HISTORY_SIZE = 100;

    private ApiClient apiClient;
    private CoreV1Api coreV1Api;
    @PostConstruct
    public void init() {
        try {
            apiClient = Config.fromConfig(k8sConfig.getConfig());
            Configuration.setDefaultApiClient(apiClient);
            coreV1Api = new CoreV1Api(apiClient);
            log.info("K8s API客户端初始化成功");
        } catch (Exception e) {
            log.error("K8s API客户端初始化失败，集群监控功能将不可用", e);
            // 设置一个标志位，表示K8s连接不可用
            return;
        }

        try {
            clusterInfo = new ClusterInfo("k8s-cluster", k8sConfig.getIp(), k8sConfig.getPort());
            log.info("集群信息对象创建成功");
        } catch (Exception e) {
            log.error("集群信息对象创建失败", e);
            return;
        }

        try {
            refreshClusterInfo();
            log.info("K8s集群监控服务初始化完成");
        } catch (Exception e) {
            log.error("首次刷新集群信息失败，将在定时任务中重试", e);
            // 不影响初始化完成，让定时任务去重试
        }
    }


    @Scheduled(fixedRate = 30000)
    public void refreshClusterInfo() {
        try {
            //log.info("开始刷新K8s集群信息...");

            V1NodeList nodeList = coreV1Api.listNode(
                    null, null, null, null,
                    "!node-role.kubernetes.io/edge",
                    null, null, null, null, null
            );

            // 获取 GPU metrics 信息
            Map<String, NodeGpuInfo> gpuMetricsMap = hamiMetricsService.fetchGpuMetrics();

            for (V1Node node : nodeList.getItems()) {
                String nodeName = node.getMetadata().getName();

                NodeInfo nodeInfo = clusterInfo.getNodes().get(nodeName);
                if (nodeInfo == null) {
                    nodeInfo = new NodeInfo(nodeName, MAX_HISTORY_SIZE);
                    nodeInfo.setCreateTime(LocalDateTime.now());
                }

                updateNodeBasicInfo(nodeInfo, node);
                updateNodeResourceInfo(nodeInfo, node);

                // 更新 GPU 信息
                NodeGpuInfo gpuInfo = gpuMetricsMap.get(nodeName);
                if (gpuInfo != null) {
                    nodeInfo.updateGpuInfo(gpuInfo);
                } else {
                    // 如果没有 GPU 信息，设置为空
                    nodeInfo.updateGpuInfo(new NodeGpuInfo());
                }

                clusterInfo.addOrUpdateNode(nodeInfo);
            }

            //log.info("K8s集群信息刷新完成，当前节点数: {}, 就绪节点数: {}",
              //      clusterInfo.getNodeCount(), clusterInfo.getReadyNodeCount());

        } catch (ApiException e) {
            log.error("刷新K8s集群信息失败", e);
        }
    }

    private void updateNodeBasicInfo(NodeInfo nodeInfo, V1Node node) {
        if (node.getStatus() != null && node.getStatus().getAddresses() != null) {
            for (V1NodeAddress address : node.getStatus().getAddresses()) {
                if ("InternalIP".equals(address.getType())) {
                    nodeInfo.setNodeIp(address.getAddress());
                    break;
                }
            }
        }

        if (node.getStatus() != null && node.getStatus().getConditions() != null) {
            for (V1NodeCondition condition : node.getStatus().getConditions()) {
                if ("Ready".equals(condition.getType())) {
                    nodeInfo.setStatus("True".equals(condition.getStatus()) ? "Ready" : "NotReady");
                    break;
                }
            }
        }

        if (node.getMetadata() != null && node.getMetadata().getLabels() != null) {
            Map<String, String> labels = node.getMetadata().getLabels();
            if (labels.containsKey("node-role.kubernetes.io/master") ||
                    labels.containsKey("node-role.kubernetes.io/control-plane")) {
                nodeInfo.setRole("master");
            } else {
                nodeInfo.setRole("worker");
            }
        }
    }



    private void updateNodeResourceInfo(NodeInfo nodeInfo, V1Node node) {
        ResourceUsage usage = new ResourceUsage();
        String nodeName = node.getMetadata().getName();

        try {
            Map<String, Object> cpuTotalResponse = prometheusService.getCpuCount();
            Map<String, Object> cpuUsedResponse = prometheusService.getCpuUsedCount();
            Map<String, Object> memoryTotalResponse = prometheusService.getMemoryTotal();
            Map<String, Object> memoryUsedResponse = prometheusService.getMemoryUsed();
            Map<String, Object> diskTotalResponse = prometheusService.getDiskTotal();
            Map<String, Object> diskUsedResponse = prometheusService.getDiskUsed();


            Map<String, MetricData> metricMap = new HashMap<>();
            parseMetricData(cpuTotalResponse, (m, v) -> m.setCpuTotal((String) v.get(1)), metricMap);
            parseMetricData(cpuUsedResponse, (m, v) -> m.setCpuUsed((String) v.get(1)), metricMap);
            parseMetricData(memoryTotalResponse, (m, v) -> m.setMemoryTotal((String) v.get(1)), metricMap);
            parseMetricData(memoryUsedResponse, (m, v) -> m.setMemoryUsed((String) v.get(1)), metricMap);
            //磁盘总量（需要累加所有分区）
            parseMetricData(diskTotalResponse, (metricData, value) -> {
                if (value != null && value.size() > 1) {
                    String diskTotalStr = (String) value.get(1);
                    // 获取当前值并累加
                    double currentTotal = metricData.getDiskTotal() != null ?
                            parseLong(metricData.getDiskTotal()) : 0;
                    double newTotal = currentTotal + parseLong(diskTotalStr);
                    metricData.setDiskTotal(String.valueOf(newTotal));
                }
            }, metricMap);

            // 磁盘使用量（需要累加所有分区）
            parseMetricData(diskUsedResponse, (metricData, value) -> {
                if (value != null && value.size() > 1) {
                    String diskUsedStr = (String) value.get(1);
                    // 获取当前值并累加
                    double currentUsed = metricData.getDiskUsed() != null ?
                            parseLong(metricData.getDiskUsed()) : 0;
                    double newUsed = currentUsed + parseLong(diskUsedStr);
                    metricData.setDiskUsed(String.valueOf(newUsed));
                }
            }, metricMap);

            // 找到当前节点对应的监控数据
            MetricData metric = findMetricByNode(metricMap, nodeName);
            if (metric == null) {
                log.warn("未找到 Prometheus 指标数据：{}", nodeName);
                return;
            }


            usage.setTotalCpu(parseLong(metric.getCpuTotal()));
            usage.setUsedCpu(parseLong(metric.getCpuUsed()));

            usage.setTotalMemory(parseLong(metric.getMemoryTotal()));
            usage.setUsedMemory(parseLong(metric.getMemoryUsed()));

            usage.setTotalDisk(parseLong(metric.getDiskTotal()));
            usage.setUsedDisk(parseLong(metric.getDiskUsed()));

            nodeInfo.updateCurrentUsage(usage);

        } catch (Exception e) {
            log.error("获取 Prometheus 节点资源信息失败: {}", nodeName, e);
        }
    }
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


    private MetricData findMetricByNode(Map<String, MetricData> metricMap, String nodeName) {
        // Prometheus 中的 instance 通常形如： "m1:9100" 或者 "192.168.1.10:9100"
        for (Map.Entry<String, MetricData> entry : metricMap.entrySet()) {
            String instance = entry.getKey();
            if (instance.startsWith(nodeName) || instance.contains(nodeName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private double parseDouble(String val) {
        if (val == null || val.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private long parseLong(String val) {
        if (val == null || val.isEmpty()) return 0L;
        try {
            return Double.valueOf(val).longValue();
        } catch (NumberFormatException e) {
            return 0L;
        }
    }


    private Long quantityToCpu(Quantity quantity) {
        if (quantity == null) return 0L;
        return quantity.getNumber().multiply(new java.math.BigDecimal(1000)).longValue();
    }

    private Long quantityToBytes(Quantity quantity) {
        if (quantity == null) return 0L;
        return quantity.getNumber().longValue();
    }

    public ClusterInfo getClusterInfo() {
        return clusterInfo;
    }

    public NodeInfo getNodeInfo(String nodeName) {
        return clusterInfo.getNodes().get(nodeName);
    }

    public Map<String, NodeInfo> getAllNodes() {
        return clusterInfo.getNodes();
    }
}
