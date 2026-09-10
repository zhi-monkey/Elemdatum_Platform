package org.dlut.adv.mineai.model.kubernetes.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpuMetric {
    /** 节点名称 */
    private String nodeName;
    
    /** GPU 索引 */
    private Integer gpuIndex;
    
    /** GPU 名称（型号或自定义名称） */
    private String gpuName;
    
    /** 采样时间 */
    private LocalDateTime timestamp;
    
    /** GPU 显存使用率（0-100） */
    private Double gpuMemoryPercent;
    
    /** GPU 核心使用率（0-100） */
    private Double gpuCorePercent;
    
    /** GPU 总显存（bytes） */
    private Long gpuMemoryTotal;
    
    /** GPU 已使用显存（bytes） */
    private Long gpuMemoryUsed;
    
    public GpuMetric(String nodeName, Integer gpuIndex, String gpuName, LocalDateTime timestamp, Double gpuMemoryPercent) {
        this.nodeName = nodeName;
        this.gpuIndex = gpuIndex;
        this.gpuName = gpuName;
        this.timestamp = timestamp;
        this.gpuMemoryPercent = gpuMemoryPercent;
    }
}
