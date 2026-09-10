package org.dlut.adv.mineai.model.kubernetes.entity;

import lombok.Data;

@Data
public class ResourceUsage {
    private Long totalCpu;        // CPU总量(毫核)
    private Long totalMemory;     // 内存总量(bytes)
    private Long totalDisk;       // 磁盘总量(bytes)

    private Long usedCpu;         // CPU已使用量(毫核)
    private Long usedMemory;      // 内存已使用量(bytes)
    private Long usedDisk;        // 磁盘已使用量(bytes)

    private Double cpuPercent;    // CPU使用率
    private Double memoryPercent; // 内存使用率
    private Double diskPercent;   // 磁盘使用率

    public void calculatePercents() {
        this.cpuPercent = totalCpu > 0 ? (usedCpu * 100.0 / totalCpu) : 0.0;
        this.memoryPercent = totalMemory > 0 ? (usedMemory * 100.0 / totalMemory) : 0.0;
        this.diskPercent = totalDisk > 0 ? (usedDisk * 100.0 / totalDisk) : 0.0;
    }
}