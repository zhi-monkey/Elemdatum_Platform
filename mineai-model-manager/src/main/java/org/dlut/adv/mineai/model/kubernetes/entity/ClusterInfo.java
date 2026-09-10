package org.dlut.adv.mineai.model.kubernetes.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ClusterInfo {
    private String clusterName;
    private String clusterIp;
    private Integer clusterPort;
    private LocalDateTime lastUpdateTime;

    // 节点信息映射 nodeName -> NodeInfo
    private final Map<String, NodeInfo> nodes = new ConcurrentHashMap<>();

    // 集群总体资源情况
    private ResourceUsage totalUsage;

    public ClusterInfo(String clusterName, String clusterIp, Integer clusterPort) {
        this.clusterName = clusterName;
        this.clusterIp = clusterIp;
        this.clusterPort = clusterPort;
        this.totalUsage = new ResourceUsage();
    }

    /**
     * 添加或更新节点
     */
    public void addOrUpdateNode(NodeInfo nodeInfo) {
        nodes.put(nodeInfo.getNodeName(), nodeInfo);
        calculateTotalUsage();
    }

    /**
     * 移除节点
     */
    public void removeNode(String nodeName) {
        nodes.remove(nodeName);
        calculateTotalUsage();
    }

    /**
     * 计算集群总资源使用情况
     */
    private void calculateTotalUsage() {
        ResourceUsage total = new ResourceUsage();
        total.setTotalCpu(0L);
        total.setTotalMemory(0L);
        total.setTotalDisk(0L);
        total.setUsedCpu(0L);
        total.setUsedMemory(0L);
        total.setUsedDisk(0L);

        for (NodeInfo node : nodes.values()) {
            ResourceUsage nodeUsage = node.getCurrentUsage();
            if (nodeUsage != null) {
                total.setTotalCpu(total.getTotalCpu() + nodeUsage.getTotalCpu());
                total.setTotalMemory(total.getTotalMemory() + nodeUsage.getTotalMemory());
                total.setTotalDisk(total.getTotalDisk() + nodeUsage.getTotalDisk());
                total.setUsedCpu(total.getUsedCpu() + nodeUsage.getUsedCpu());
                total.setUsedMemory(total.getUsedMemory() + nodeUsage.getUsedMemory());
                total.setUsedDisk(total.getUsedDisk() + nodeUsage.getUsedDisk());
            }
        }

        total.calculatePercents();
        this.totalUsage = total;
        this.lastUpdateTime = LocalDateTime.now();
    }

    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * 获取就绪节点数量
     */
    public long getReadyNodeCount() {
        return nodes.values().stream()
                .filter(node -> "Ready".equals(node.getStatus()))
                .count();
    }
}