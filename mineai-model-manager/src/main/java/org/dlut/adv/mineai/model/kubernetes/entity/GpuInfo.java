package org.dlut.adv.mineai.model.kubernetes.entity;


import lombok.Data;

@Data
public class GpuInfo {
    private String gpuModel;           // GPU 型号
    private Integer gpuIndex;          // GPU 索引
    private String gpuUUID;            // GPU UUID
    private Long totalMemory;          // 总显存(bytes)
    private Long usedMemory;           // 已使用显存(bytes)
    private Double memoryPercent;      // 显存使用率
    private Integer totalCores;        // 总核心数(用于vGPU切分)
    private Integer usedCores;         // 已使用核心数
    private Double coresPercent;       // 核心使用率
    private String health;             // 健康状态

    public void calculatePercents() {
        this.memoryPercent = totalMemory > 0 ? (usedMemory * 100.0 / totalMemory) : 0.0;
        this.coresPercent = totalCores > 0 ? (usedCores * 100.0 / totalCores) : 0.0;
    }
}
