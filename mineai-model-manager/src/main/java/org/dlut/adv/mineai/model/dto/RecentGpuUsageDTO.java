package org.dlut.adv.mineai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author mingming
 * @date 2024/12/06
 */
@Data
@Builder
public class RecentGpuUsageDTO {
    private String nodeName;
    private String ip;
    private List<UsageClass> recentUsage;
    private String gpuIndex;
    private String modelName;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UsageClass{
        private String timeStamp;
        private String usage;
    }
}
