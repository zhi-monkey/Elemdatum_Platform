/**
 * Copyright 2019-2020 Zheng Jie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dubhe.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dubhe.biz.db.entity.BaseEntity;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * @description 用户实体
 * @date 2020-11-29
 */
@Data
@TableName("user")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -3836401769559845765L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "username")
    private String username;
    public final static String ADMIN_USERNAME = "admin";

    /**
     * 用户昵称
     */
    @TableField(value = "nick_name")
    private String nickName;

    /**
     * 性别
     */
    @TableField(value = "sex")
    private String sex;

    @TableField(value = "email")
    private String email;

    @TableField(value = "phone")
    private String phone;

    @TableField(value = "enabled")
    private Boolean enabled;

    @TableField(value = "password")
    private String password;

    @TableField(value = "last_password_reset_time")
    private Date lastPasswordResetTime;

    @TableField(value = "remark")
    private String remark;

    @TableField(value = "avatar_id")
    private Long avatarId;

    @TableField(value = "deleted",fill = FieldFill.INSERT)
    private Boolean deleted = false;

    @TableField(exist = false)
    private UserAvatar userAvatar;


    @NotEmpty
    @TableField(exist = false)
    private List<Role> roles;

    @TableField(value = "department_id")
    private Long departmentId;

    @TableField(value = "memory_used")
    private Long memoryUsed;
    public final static String MEMORY_USED_OVER_LIMIT = "memory used over limit";
    public final static String RELEASING_MEMORY_TO_LARGE = "释放的MEMORY数量大于用户已使用的MEMORY数量";

    @TableField(value = "cpu_used")
    private Long cpuUsed;
    public final static String CPU_USED_OVER_LIMIT = "cpu used over limit";
    public final static String DISTRIBUTED_SUCCESS = "分配CPU和MEMORY成功";
    public final static String RELEASING_CPU_TO_LARGE = "释放的CPU数量大于用户已使用的CPU数量";
    public final static String DISTRIBUTED_FAILED_UNKNOWN_REASON = "未知原因导致分配失败";
    public final static String RELEASED_SUCCESS = "释放资源成功";
    public final static String RELEASED_FAILED_UNKNOWN_REASON = "未知原因导致释放失败";

    @TableField(value = "gpu_memory_used")
    private Long gpuMemoryUsed;
    public final static String GPU_MEMORY_USED_OVER_LIMIT = "gpu memory used over limit";
    public final static String RELEASING_GPU_MEMORY_TO_LARGE = "分配GPU和GPU MEMORY成功";

    @TableField(value = "v_gpu_cores_used")
    private Long vGpuCoresUsed;
    public final static String V_GPU_CORES_USED_OVER_LIMIT = "v gpu cores used over limit";
    public final static String RELEASING_V_GPU_CORES_TO_LARGE = "释放的VGPU数量大于用户已使用的VGPU数量";
}
