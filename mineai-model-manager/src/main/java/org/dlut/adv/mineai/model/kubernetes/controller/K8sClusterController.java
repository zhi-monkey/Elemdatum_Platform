package org.dlut.adv.mineai.model.kubernetes.controller;

import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.kubernetes.entity.*;
import org.dlut.adv.mineai.model.kubernetes.service.HamiMetricsService;
import org.dlut.adv.mineai.model.kubernetes.service.K8sClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/k8s/cluster")
public class K8sClusterController {

    @Autowired
    private K8sClusterService k8sClusterService;

    @Autowired
    private HamiMetricsService hamiMetricsService;

    @GetMapping("/overview")
    public Msg<ClusterInfo> getClusterOverview() {
        try {
            ClusterInfo clusterInfo = k8sClusterService.getClusterInfo();
            return new Msg<>(MsgCode.SUCCEED, clusterInfo);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 集群内存汇总：所有节点内存总量、已使用量以及使用率
     */
    @GetMapping("/memory/summary")
    public Msg<Map<String, Object>> getMemorySummary() {
        try {
            ClusterInfo clusterInfo = k8sClusterService.getClusterInfo();
            if (clusterInfo == null || clusterInfo.getTotalUsage() == null) {
                return new Msg<>(MsgCode.SUCCEED, new HashMap<>());
            }

            ResourceUsage totalUsage = clusterInfo.getTotalUsage();
            Map<String, Object> result = new HashMap<>();
            result.put("totalMemory", totalUsage.getTotalMemory());
            result.put("usedMemory", totalUsage.getUsedMemory());
            result.put("memoryPercent", totalUsage.getMemoryPercent());

            return new Msg<>(MsgCode.SUCCEED, result);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 集群磁盘汇总：所有节点磁盘总量、已使用量以及使用率
     */
    @GetMapping("/disk/summary")
    public Msg<Map<String, Object>> getDiskSummary() {
        try {
            ClusterInfo clusterInfo = k8sClusterService.getClusterInfo();
            if (clusterInfo == null || clusterInfo.getTotalUsage() == null) {
                return new Msg<>(MsgCode.SUCCEED, new HashMap<>());
            }

            ResourceUsage totalUsage = clusterInfo.getTotalUsage();
            Map<String, Object> result = new HashMap<>();
            result.put("totalDisk", totalUsage.getTotalDisk());
            result.put("usedDisk", totalUsage.getUsedDisk());
            result.put("diskPercent", totalUsage.getDiskPercent());

            return new Msg<>(MsgCode.SUCCEED, result);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @GetMapping("/nodes")
    public Msg<Map<String, NodeInfo>> getAllNodes() {
        try {
            Map<String, NodeInfo> nodes = k8sClusterService.getAllNodes();
            return new Msg<>(MsgCode.SUCCEED, nodes);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @GetMapping("/nodes/{nodeName}")
    public Msg<NodeInfo> getNodeInfo(@PathVariable String nodeName) {
        try {
            NodeInfo nodeInfo = k8sClusterService.getNodeInfo(nodeName);
            return new Msg<>(MsgCode.SUCCEED, nodeInfo);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @GetMapping("/nodes/{nodeName}/metrics")
    public Msg<Queue<ResourceMetric>> getNodeMetrics(@PathVariable String nodeName) {
        try {
            NodeInfo nodeInfo = k8sClusterService.getNodeInfo(nodeName);
            Queue<ResourceMetric> metrics = nodeInfo != null ? nodeInfo.getMetricsHistoryCopy() : new LinkedList<>();
            return new Msg<>(MsgCode.SUCCEED, metrics);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @GetMapping("/metrics/recent")
    public Msg<Map<String, List<ResourceMetric>>> getRecentMetrics(@RequestParam(defaultValue = "30") int minutes) {
        try {
            if (minutes <= 0) {
                minutes = 30;
            }
            LocalDateTime fromTime = LocalDateTime.now().minusMinutes(minutes);

            Map<String, NodeInfo> nodes = k8sClusterService.getAllNodes();
            Map<String, List<ResourceMetric>> result = new HashMap<>();

            for (Map.Entry<String, NodeInfo> entry : nodes.entrySet()) {
                String nodeName = entry.getKey();
                NodeInfo nodeInfo = entry.getValue();

                Queue<ResourceMetric> history = nodeInfo != null ? nodeInfo.getMetricsHistoryCopy() : new LinkedList<>();
                List<ResourceMetric> filtered = new ArrayList<>();
                for (ResourceMetric metric : history) {
                    if (metric.getTimestamp() != null &&
                            (metric.getTimestamp().isAfter(fromTime) || metric.getTimestamp().isEqual(fromTime))) {
                        filtered.add(metric);
                    }
                }
                result.put(nodeName, filtered);
            }

            return new Msg<>(MsgCode.SUCCEED, result);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 获取所有节点的 GPU 最近使用率（时间序列）
     */
    @GetMapping("/gpu/metrics/recent")
    public Msg<List<GpuMetric>> getRecentGpuMetrics(@RequestParam(defaultValue = "30") int minutes) {
        try {
            if (minutes <= 0) {
                minutes = 30;
            }
            LocalDateTime fromTime = LocalDateTime.now().minusMinutes(minutes);

            Map<String, NodeInfo> nodes = k8sClusterService.getAllNodes();
            List<GpuMetric> result = new ArrayList<>();

            for (NodeInfo nodeInfo : nodes.values()) {
                Queue<ResourceMetric> history = nodeInfo.getMetricsHistoryCopy();

                for (ResourceMetric metric : history) {
                    if (metric.getTimestamp() != null &&
                            (metric.getTimestamp().isAfter(fromTime) ||
                                    metric.getTimestamp().isEqual(fromTime))) {

                        if (Boolean.TRUE.equals(metric.getHasGpu()) &&
                                metric.getGpuMetrics() != null) {
                            result.addAll(metric.getGpuMetrics());
                        }
                    }
                }
            }

            return new Msg<>(MsgCode.SUCCEED, result);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 获取节点 GPU 信息
     */
    @GetMapping("/nodes/{nodeName}/gpu")
    public Msg<NodeGpuInfo> getNodeGpuInfo(@PathVariable String nodeName) {
        try {
            NodeInfo nodeInfo = k8sClusterService.getNodeInfo(nodeName);
            NodeGpuInfo gpuInfo = nodeInfo != null ? nodeInfo.getGpuInfo() : new NodeGpuInfo();
            return new Msg<>(MsgCode.SUCCEED, gpuInfo);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 获取所有节点的 GPU 信息汇总
     */
    @GetMapping("/gpu/summary")
    public Msg<Map<String, Object>> getGpuSummary() {
        try {
            Map<String, NodeInfo> nodes = k8sClusterService.getAllNodes();

            int totalGpuCount = 0;
            int availableGpuCount = 0;
            long totalGpuMemory = 0;
            long usedGpuMemory = 0;

            List<NodeGpuInfo> nodeGpuList = new ArrayList<>();

            for (NodeInfo node : nodes.values()) {
                NodeGpuInfo gpuInfo = node.getGpuInfo();
                if (gpuInfo.getTotalGpuCount() != null && gpuInfo.getTotalGpuCount() > 0) {
                    nodeGpuList.add(gpuInfo);
                    totalGpuCount += gpuInfo.getTotalGpuCount();
                    availableGpuCount += gpuInfo.getAvailableGpuCount();
                    totalGpuMemory += gpuInfo.getTotalGpuMemory();
                    usedGpuMemory += gpuInfo.getUsedGpuMemory();
                }
            }

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalGpuCount", totalGpuCount);
            summary.put("availableGpuCount", availableGpuCount);
            summary.put("totalGpuMemory", totalGpuMemory);
            summary.put("usedGpuMemory", usedGpuMemory);
            summary.put("gpuMemoryPercent", totalGpuMemory > 0 ?
                    (usedGpuMemory * 100.0 / totalGpuMemory) : 0.0);
            summary.put("nodes", nodeGpuList);

            return new Msg<>(MsgCode.SUCCEED, summary);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 获取 Pod GPU 使用情况
     */
    @GetMapping("/gpu/pods")
    public Msg<List<HamiMetricsService.PodGpuUsage>> getPodGpuUsage() {
        try {
            List<HamiMetricsService.PodGpuUsage> podGpuUsage = hamiMetricsService.fetchPodGpuUsage();
            return new Msg<>(MsgCode.SUCCEED, podGpuUsage);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 获取指定节点上的 Pod GPU 使用情况
     */
    @GetMapping("/nodes/{nodeName}/gpu/pods")
    public Msg<List<HamiMetricsService.PodGpuUsage>> getNodePodGpuUsage(@PathVariable String nodeName) {
        try {
            List<HamiMetricsService.PodGpuUsage> nodePodGpuUsage = hamiMetricsService.fetchPodGpuUsage().stream()
                    .filter(usage -> nodeName.equals(usage.getNodeName()))
                    .collect(java.util.stream.Collectors.toList());
            return new Msg<>(MsgCode.SUCCEED, nodePodGpuUsage);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    @PostMapping("/refresh")
    public Msg<String> refreshCluster() {
        try {
            k8sClusterService.refreshClusterInfo();
            return new Msg<>(MsgCode.SUCCEED, "集群信息刷新成功");
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }

    /**
     * 获取集群详情数据（用于前端显示）
     */
    @GetMapping("/details")
    public Msg<Map<String, Object>> getClusterDetails() {
        try {
            Map<String, NodeInfo> nodes = k8sClusterService.getAllNodes();
            ClusterInfo clusterInfo = k8sClusterService.getClusterInfo();
            
            Map<String, Object> result = new HashMap<>();
            
            // 集群基本信息
            Map<String, Object> clusterBasic = new HashMap<>();
            clusterBasic.put("clusterName", clusterInfo.getClusterName());
            clusterBasic.put("clusterIp", clusterInfo.getClusterIp());
            clusterBasic.put("clusterPort", clusterInfo.getClusterPort());
            clusterBasic.put("nodeCount", clusterInfo.getNodeCount());
            clusterBasic.put("readyNodeCount", clusterInfo.getReadyNodeCount());
            clusterBasic.put("lastUpdateTime", clusterInfo.getLastUpdateTime());
            result.put("cluster", clusterBasic);
            
            // 节点详情列表
            List<Map<String, Object>> nodeList = new ArrayList<>();
            for (NodeInfo node : nodes.values()) {
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("key", node.getNodeName());
                nodeData.put("name", node.getNodeName());
                nodeData.put("status", node.getStatus());
                nodeData.put("role", node.getRole());
                nodeData.put("nodeIp", node.getNodeIp());
                nodeData.put("lastUpdateTime", node.getLastUpdateTime());
                
                // CPU信息
                if (node.getCurrentUsage() != null) {
                    ResourceUsage usage = node.getCurrentUsage();
                    nodeData.put("cpuUsed", usage.getUsedCpu() != null ? usage.getUsedCpu() : 0);
                    nodeData.put("cpuTotal", usage.getTotalCpu() != null ? usage.getTotalCpu()  : 0);
                    nodeData.put("cpuUsage", usage.getCpuPercent() != null ? Math.round(usage.getCpuPercent()) : 0);
                } else {
                    nodeData.put("cpuUsed", 0);
                    nodeData.put("cpuTotal", 0);
                    nodeData.put("cpuUsage", 0);
                }
                
                // 内存信息
                if (node.getCurrentUsage() != null) {
                    ResourceUsage usage = node.getCurrentUsage();
                    nodeData.put("memoryUsed", usage.getUsedMemory() != null ? usage.getUsedMemory() / (1024 * 1024 * 1024) : 0); // 转换为GB
                    nodeData.put("memoryTotal", usage.getTotalMemory() != null ? usage.getTotalMemory() / (1024 * 1024 * 1024) : 0); // 转换为GB
                    nodeData.put("memoryUsage", usage.getMemoryPercent() != null ? Math.round(usage.getMemoryPercent()) : 0);
                } else {
                    nodeData.put("memoryUsed", 0);
                    nodeData.put("memoryTotal", 0);
                    nodeData.put("memoryUsage", 0);
                }

                // GPU信息
                if (node.getGpuInfo() != null && node.getGpuInfo().getGpus() != null && !node.getGpuInfo().getGpus().isEmpty()) {
                    List<Map<String, Object>> gpuList = new ArrayList<>();
                    for (GpuInfo gpu : node.getGpuInfo().getGpus()) {
                        Map<String, Object> gpuData = new HashMap<>();
                        gpuData.put("name", gpu.getGpuModel() != null ? gpu.getGpuModel() : "GPU-" + gpu.getGpuIndex());
                        gpuData.put("status", "Healthy".equals(gpu.getHealth()) ? "Running" : "Idle");
                        gpuData.put("memoryUsed", gpu.getUsedMemory() != null ? gpu.getUsedMemory() / (1024 * 1024 * 1024) : 0); // 转换为GB
                        gpuData.put("memoryTotal", gpu.getTotalMemory() != null ? gpu.getTotalMemory() / (1024 * 1024 * 1024) : 0); // 转换为GB
                        gpuData.put("memoryUsage", gpu.getMemoryPercent() != null ? Math.round(gpu.getMemoryPercent()) : 0);
                        gpuData.put("computeUsed", gpu.getUsedCores() != null ? gpu.getUsedCores() : 0);
                        gpuData.put("computeTotal", gpu.getTotalCores() != null ? gpu.getTotalCores() : 100);
                        gpuData.put("computeUsage", gpu.getCoresPercent() != null ? Math.round(gpu.getCoresPercent()) : 0);
                        gpuList.add(gpuData);
                    }
                    nodeData.put("gpus", gpuList);
                } else {
                    nodeData.put("gpus", new ArrayList<>());
                }
                
                nodeList.add(nodeData);
            }
            result.put("nodes", nodeList);
            
            return new Msg<>(MsgCode.SUCCEED, result);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED);
        }
    }
}
