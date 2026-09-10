package org.dubhe.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@TableName("department")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "name")
    private String name;

    @TableField(value = "description")
    private String description;

    @TableField(value = "memory_limit")
    private Long memoryLimit;

    @TableField(value = "cpu_limit")
    private Long cpuLimit;

    @TableField(value = "gpu_memory_limit")
    private Long gpuMemoryLimit;

    @TableField(value = "v_gpu_cores_limit")
    private Long vGpuCoresLimit;
}

