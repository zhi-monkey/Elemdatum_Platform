package org.dlut.adv.mineai.model.kubernetes.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourceMetric {
    private LocalDateTime timestamp;
    // CPU使用量(毫核)
    private Long cpuUsage;
    // 内存使用量(bytes)
    private Long memoryUsage;
    // 磁盘使用量(bytes)
    private Long diskUsage;
    // CPU使用率
    private Double cpuPercent;
    // 内存使用率
    private Double memoryPercent;
    // 磁盘使用率
    private Double diskPercent;
    // 是否有GPU
    private Boolean hasGpu;
    // GPU总显存(bytes)
    private Long gpuMemoryTotal;
    // GPU已使用显存(bytes)
    private Long gpuMemoryUsage;
    // GPU显存使用率
    private Double gpuMemoryPercent;
    // 每个GPU的指标
    private List<GpuMetric> gpuMetrics = new ArrayList<>();

    public ResourceMetric(LocalDateTime timestamp, Long cpuUsage, Long memoryUsage, Long diskUsage, 
                         Double cpuPercent, Double memoryPercent, Double diskPercent,
                         Boolean hasGpu, Long gpuMemoryTotal, Long gpuMemoryUsage, Double gpuMemoryPercent) {
        this.timestamp = timestamp;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.diskUsage = diskUsage;
        this.cpuPercent = cpuPercent;
        this.memoryPercent = memoryPercent;
        this.diskPercent = diskPercent;
        this.hasGpu = hasGpu;
        this.gpuMemoryTotal = gpuMemoryTotal;
        this.gpuMemoryUsage = gpuMemoryUsage;
        this.gpuMemoryPercent = gpuMemoryPercent;
    }
}
