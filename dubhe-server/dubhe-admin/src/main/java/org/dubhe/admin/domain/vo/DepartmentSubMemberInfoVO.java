package org.dubhe.admin.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dubhe.admin.domain.entity.User;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "DepartmentSubMemberInfo vo", description = "部门个人资源使用量")
public class DepartmentSubMemberInfoVO {
    @ApiModelProperty(value = "部门ID")
    private Long id;
    @ApiModelProperty(value = "部门名称")
    private String name;
    @ApiModelProperty(value = "部门人数")
    private int userCount;
    @ApiModelProperty(value = "部门内存总量")
    private Long memoryLimit;
    @ApiModelProperty(value = "部门CPU总量")
    private Long cpuLimit;
    @ApiModelProperty(value = "部门GPU显存总量")
    private Long gpuMemoryLimit;
    @ApiModelProperty(value = "部门成员")
    private List<User> users;
}
