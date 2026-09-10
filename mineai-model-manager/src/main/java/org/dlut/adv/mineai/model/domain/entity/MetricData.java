package org.dlut.adv.mineai.model.domain.entity;

import lombok.Data;
import org.dlut.adv.mineai.model.kubernetes.entity.NodeGpuInfo;

/**
 * 用于封装从prometheus返回的数据
 * @author mingming
 * @date 2024/11/15
 */
@Data
public class MetricData {
    private String instance;
    private String ip;

    private String cpuUsed;
    private String cpuTotal;
    private String cpuUsage;

    private String memoryUsed;
    private String memoryTotal;

    private String diskUsed;
    private String diskTotal;

    private String gpuMemoryUsed;
    private String gpuMemoryTotal;

    private String gpuCoresUsed;
    private String gpuCoresTotal;
    private String gpuUsage;
    
    /**
     * 节点GPU详细信息（包含每个GPU的UUID、型号、使用情况等）
     */
    private NodeGpuInfo nodeGpuInfo;
}
