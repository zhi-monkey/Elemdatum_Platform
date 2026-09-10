package org.dubhe.admin.domain.dto;

import java.util.List;

import lombok.Data;
import java.util.List;

import lombok.Data;
import java.util.List;

@Data
public class DepartmentQueryDTO {
    private Long id;
    private String name;
    private String description;
    private int userCount;
    private List<Long> userIds;
    private Long memoryLimit;
    private Long memoryUsed;
    private Long cpuLimit;
    private Long cpuUsed;
    private Long gpuMemoryLimit;
    private Long gpuMemoryUsed;
    private Long vGpuCoresLimit;
    private Long vGpuCoresUsed;
}
