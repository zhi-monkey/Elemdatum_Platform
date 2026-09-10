package org.dlut.adv.mineai.model.dto;

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
public class UserResourceDTO {
    private Long userId;

    private Long cpuUse;
    public final static String CPU_USED_OVER_LIMIT = "cpu used over limit";
    public final static String RELEASING_CPU_TO_LARGE = "释放的CPU数量大于用户已使用的CPU数量";

    private Long memoryUse;
    public final static String MEMORY_USED_OVER_LIMIT = "memory used over limit";
    public final static String RELEASING_MEMORY_TO_LARGE = "释放的MEMORY数量大于用户已使用的MEMORY数量";

    private Long gpuMemoryUse;
    public final static String GPU_MEMORY_USED_OVER_LIMIT = "gpu memory used over limit";
    public final static String RELEASING_GPU_MEMORY_TO_LARGE = "释放的GPU MEMORY数量大于用户已使用的GPU MEMORY数量";

    private Long vGpuCoresUse;
    public final static String VGPU_CORES_USED_OVER_LIMIT = "vgpu cores used over limit";
    public final static String RELEASING_VGPU_CORES_TO_LARGE = "释放的VGPU CORES数量大于用户已使用的VGPU CORES数量";


    public final static String DISTRIBUTED_SUCCESS = "分配CPU和MEMORY成功";
    public final static String RELEASED_SUCCESS = "释放资源成功";

    public final static String DISTRIBUTED_FAILED_UNKNOWN_REASON = "未知原因导致分配失败";
    public final static String RELEASED_FAILED_UNKNOWN_REASON = "未知原因导致释放失败";
}
