package org.dlut.adv.mineai.core.entity;

import lombok.Data;
import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private int id;                // 用户ID
    private String username;       // 用户名
    private String email;          // 邮箱
    private String sex;            // 性别
    private String phone;          // 电话
    private String nickName;       // 昵称
    private boolean enabled;        // 是否启用
    private List<Role> roles;      // 角色列表
}


