package org.dubhe.admin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dubhe.admin.domain.entity.Department;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDTO {

    private Long id; // 用于修改操作时传递部门ID

    @NotEmpty(message = "部门名称不能为空")
    private String name;

    private String description;

    private List<Long> userIds;

    private Long memoryLimit;

    private Long cpuLimit;

    private Long gpuMemoryLimit;

    private Long vGpuCoresLimit = 300L;


    // Getters and Setters

    public Department toEntity() {
        return Department.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .memoryLimit(this.memoryLimit)
                .cpuLimit(this.cpuLimit)
                .gpuMemoryLimit(this.gpuMemoryLimit)
                .vGpuCoresLimit(this.vGpuCoresLimit)
                .build();
    }
}
