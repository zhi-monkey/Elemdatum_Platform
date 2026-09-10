package org.dubhe.admin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mingming
 * @date 2024/11/14
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResourceDTO {
    private Long userId;
    private Long cpuUsed;
    private Long memoryUsed;
    private Long gpuMemoryUsed;
    private Long vGpuCoresUsed;

    private Long cpuTotal;
    private Long memoryTotal;
    private Long gpuMemoryTotal;
    private Long vGpuCoresTotal;
}
