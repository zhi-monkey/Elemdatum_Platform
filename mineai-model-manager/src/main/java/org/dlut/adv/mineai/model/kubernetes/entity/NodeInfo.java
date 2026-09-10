package org.dlut.adv.mineai.model.kubernetes.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Data
public class NodeInfo {
    private String nodeName;
    private String nodeIp;
    private String status;        // Ready, NotReady
    private String role;          // master, worker
    private LocalDateTime createTime;
    private LocalDateTime lastUpdateTime;

    // 当前资源使用情况
    private ResourceUsage currentUsage;

    // GPU 信息
    private NodeGpuInfo gpuInfo;

    // 历史资源使用情况(时间序列，固定长度队列)
    private final Queue<ResourceMetric> metricsHistory;
    private final int maxHistorySize;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public NodeInfo(String nodeName, int maxHistorySize) {
        this.nodeName = nodeName;
        this.maxHistorySize = maxHistorySize;
        this.metricsHistory = new LinkedList<>();
        this.currentUsage = new ResourceUsage();
        this.gpuInfo = new NodeGpuInfo();
        this.gpuInfo.setNodeName(nodeName);
    }

    /**
     * 添加新的资源度量点
     */
    public void addMetric(ResourceMetric metric) {
        lock.writeLock().lock();
        try {
            if (metricsHistory.size() >= maxHistorySize) {
                metricsHistory.poll();
            }
            metricsHistory.offer(metric);
            this.lastUpdateTime = LocalDateTime.now();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取历史度量数据的副本
     */
    public Queue<ResourceMetric> getMetricsHistoryCopy() {
        lock.readLock().lock();
        try {
            return new LinkedList<>(metricsHistory);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 更新当前资源使用情况
     */
    public void updateCurrentUsage(ResourceUsage usage) {
        this.currentUsage = usage;
        this.currentUsage.calculatePercents();

        // 处理 GPU 汇总信息
        boolean hasGpu = false;
        long gpuTotal = 0L;
        long gpuUsed = 0L;
        double gpuPercent = 0.0;
        List<GpuMetric> gpuMetrics = new ArrayList<>();

        if (this.gpuInfo != null && this.gpuInfo.getTotalGpuCount() != null && this.gpuInfo.getTotalGpuCount() > 0) {
            hasGpu = true;
            if (this.gpuInfo.getTotalGpuMemory() != null) {
                gpuTotal = this.gpuInfo.getTotalGpuMemory();
            }
            if (this.gpuInfo.getUsedGpuMemory() != null) {
                gpuUsed = this.gpuInfo.getUsedGpuMemory();
            }
            if (this.gpuInfo.getGpuMemoryPercent() != null) {
                gpuPercent = this.gpuInfo.getGpuMemoryPercent();
            }
            
            // 添加每个GPU的指标
            if (this.gpuInfo.getGpus() != null) {
                for (GpuInfo gpu : this.gpuInfo.getGpus()) {
                    GpuMetric gpuMetric = new GpuMetric(
                        this.nodeName,
                        gpu.getGpuIndex(),
                        gpu.getGpuModel() != null ? gpu.getGpuModel() : ("GPU-" + gpu.getGpuIndex()),
                        LocalDateTime.now(),
                        gpu.getMemoryPercent()
                    );
                    gpuMetric.setGpuCorePercent(gpu.getCoresPercent());
                    gpuMetric.setGpuMemoryTotal(gpu.getTotalMemory());
                    gpuMetric.setGpuMemoryUsed(gpu.getUsedMemory());
                    gpuMetrics.add(gpuMetric);
                }
            }
        }

        ResourceMetric metric = new ResourceMetric(
                LocalDateTime.now(),
                usage.getUsedCpu(),
                usage.getUsedMemory(),
                usage.getUsedDisk(),
                usage.getCpuPercent(),
                usage.getMemoryPercent(),
                usage.getDiskPercent(),
                hasGpu,
                gpuTotal,
                gpuUsed,
                gpuPercent
        );
        metric.setGpuMetrics(gpuMetrics);
        addMetric(metric);
    }

    /**
     * 更新 GPU 信息
     */
    public void updateGpuInfo(NodeGpuInfo gpuInfo) {
        this.gpuInfo = gpuInfo;
        this.gpuInfo.calculateTotals();
    }
}
