package org.dubhe.admin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mingming
 * @date 2024/11/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUserResourceDTO {
    private Long userId;
    private Long cpuUse;
    private Long memoryUse;
    private Long gpuMemoryUse;
    private Long vGpuCoresUse;
}
