package org.dubhe.admin.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.FetchType;
import org.dubhe.admin.domain.dto.RoleUserCountDTO;
import org.dubhe.admin.domain.entity.User;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @description Demo服务mapper
 * @date 2020-11-26
 */

public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据ID查询name
     *
     * @param id 用户id
     * @return 用户
     */
    @Select("select * from user where id=#{id} and deleted = 0")
    User selectNameById(Long id);

    /**
     * 根据ID查询实体及关联对象
     *
     * @param id 用户id
     * @return 用户
     */
    @Select("select * from user where id=#{id} and deleted = 0")
    @Results(id = "userMapperResults",
            value = {
                    @Result(property = "id", column = "id"),
                    @Result(property = "roles",
                            column = "id",
                            many = @Many(select = "org.dubhe.admin.dao.RoleMapper.findRolesByUserId",
                                    fetchType = FetchType.LAZY)),
                    @Result(property = "userAvatar",
                            column = "avatar_id",
                            one = @One(select = "org.dubhe.admin.dao.UserAvatarMapper.selectById",
                                    fetchType = FetchType.LAZY))})
    User selectCollById(Long id);

    // 获取各角色的人数统计
    @Select("SELECT r.name AS roleName, COUNT(u.id) AS userCount " +
            "FROM user u " +
            "LEFT JOIN users_roles ur ON u.id = ur.user_id " +
            "LEFT JOIN role r ON ur.role_id = r.id " +
            "WHERE u.deleted = false " +  // 只统计未删除的用户
            "AND r.deleted = false " +   // 只统计未删除的角色
            "GROUP BY r.name")
    List<RoleUserCountDTO> getRoleUserCounts();



    // 获取总用户数
    @Select("SELECT COUNT(*) FROM user WHERE deleted = false")
    Integer getTotalUserCount();

    // 查询启用的用户数量
    @Select("SELECT COUNT(*) FROM user WHERE enabled = true")
    Long countEnabledUsers();

    /**
     * 根据用户名查询
     *
     * @param username 用户名
     * @return 用户
     */
    @Select("select * from user where  username=#{username} and deleted = 0")
    @ResultMap(value = "userMapperResults")
    User findByUsername(String username);

    /**
     * 根据邮箱查询
     *
     * @param email 邮箱
     * @return 用户
     */
    @Select("select * from user where  email=#{email} and deleted = 0")
    @ResultMap(value = "userMapperResults")
    User findByEmail(String email);

    /**
     * 修改密码
     *
     * @param username              用户名
     * @param pass                  密码
     * @param lastPasswordResetTime 密码最后一次重置时间
     */

    @Update("update user set password = #{pass} , last_password_reset_time = #{lastPasswordResetTime} where username = #{username}")
    void updatePass(String username, String pass, Date lastPasswordResetTime);

    /**
     * 修改邮箱
     *
     * @param username 用户名
     * @param email    邮箱
     */
    @Update("update user set email = #{email} where username = #{username}")
    void updateEmail(String username, String email);

    /**
     * 查找用户权限
     *
     * @param userId 用户id
     * @return 权限集合
     */
    Set<String> queryPermissionByUserId(Long userId);


    /**
     * 查询实体及关联对象
     *
     * @param queryWrapper 用户wrapper对象
     * @return 用户集合
     */
    @Select("select * from user ${ew.customSqlSegment}")
    @ResultMap(value = "userMapperResults")
    List<User> selectCollList(@Param("ew") Wrapper<User> queryWrapper);

    /**
     * 分页查询实体及关联对象
     *
     * @param page         分页对象
     * @param queryWrapper 用户wrapper对象
     * @return 分页user集合
     */
    @Select("select * from user ${ew.customSqlSegment} order by id desc")
    @ResultMap(value = "userMapperResults")
    IPage<User> selectCollPage(Page<User> page, @Param("ew") Wrapper<User> queryWrapper);

    /**
     * 根据角色分页查询实体及关联对象
     *
     * @param page         分页对象
     * @param queryWrapper 用户wrapper对象
     * @param roleId       角色id
     * @return 分页用户集合
     */
    @Select("select u.* from user u join  users_roles ur on u.id=ur.user_id and ur.role_id=#{roleId} ${ew.customSqlSegment}  order by u.id desc")
    @ResultMap(value = "userMapperResults")
    IPage<User> selectCollPageByRoleId(Page<User> page, @Param("ew") Wrapper<User> queryWrapper, Long roleId);

    // 查询用户单一权限的用户
    @Select("SELECT u.* FROM user u INNER JOIN users_roles ur ON ur.user_id = u.id INNER JOIN roles_auth ra ON ur.role_id = ra.role_id INNER JOIN auth_permission ap ON ra.auth_id = ap.auth_id INNER JOIN permission p ON ap.permission_id = p.id WHERE p.permission = #{permissionName}")
    @ResultMap(value = "userMapperResults")
    List<User> findUsersWithPermission(String permissionName);

    /**
     * 根据角色名查询用户（支持多个角色名）
     */
    @Select("SELECT DISTINCT u.* FROM user u " +
            "INNER JOIN users_roles ur ON u.id = ur.user_id " +
            "INNER JOIN role r ON ur.role_id = r.id " +
            "WHERE r.deleted = 0 AND u.deleted = 0 " +
            "AND (r.name = '管理员' OR r.name = '管理人员')")
    List<User> findAdminUsers();


    /**
     * 更新用户的部门ID
     *
     * @param userId       用户ID
     * @param departmentId 部门ID
     */
    @Update("UPDATE user SET department_id = #{departmentId} WHERE id = #{userId}")
    void updateDepartmentId(@Param("userId") Long userId, @Param("departmentId") Long departmentId);

    /**
     * 根据部门ID查找用户ID列表
     *
     * @param departmentId 部门ID
     * @return 用户ID列表
     */
    @Select("SELECT id FROM user WHERE department_id = #{departmentId} AND deleted = 0")
    List<Long> findUserIdsByDepartmentId(@Param("departmentId") Long departmentId);


    @Select("SELECT * FROM user WHERE department_id = #{departmentId} AND deleted = 0")
    List<User> findUsersByDepartmentId(@Param("departmentId") Long departmentId);

    @Select("SELECT * FROM user WHERE department_id IS NULL AND deleted = 0")
    List<User> findUsersWithoutDepartment();

    @Update("UPDATE user SET department_id = NULL WHERE department_id = #{departmentId}")
    void unbindUsersFromDepartment(@Param("departmentId") Long departmentId);

    @Update({
            "<script>",
            "UPDATE user",
            "<set>",
            "  department_id = #{departmentId}",
            "</set>",
            "<where>",
            "  <if test='userIds != null and userIds.size() > 0'>",
            "    id IN",
            "    <foreach item='userId' collection='userIds' open='(' separator=',' close=')'>",
            "      #{userId}",
            "    </foreach>",
            "  </if>",
            "  <if test='userIds == null or userIds.size() == 0'>",
            "    1 = 0", // 防止更新所有记录
            "  </if>",
            "</where>",
            "</script>"
    })
    void bindUsersToDepartment(@Param("departmentId") Long departmentId, @Param("userIds") List<Long> userIds);

    @Select("SELECT department_id FROM user WHERE id = #{userId}")
    Long findDepartmentIdByUserId(@Param("userId") Long userId);

    @Select("SELECT memory_used FROM user WHERE id = #{userId}")
    Long findMemoryUsedByUserId(@Param("userId") Long userId);

    @Select("SELECT cpu_used FROM user WHERE id = #{userId}")
    Long findCpuUsedByUserId(@Param("userId") Long userId);

    @Update("UPDATE user SET cpu_used = cpu_used + #{cpuUse}, memory_used = memory_used + #{memoryUse}, gpu_memory_used = gpu_memory_used + #{gpuMemoryUse},v_gpu_cores_used = v_gpu_cores_used + #{vGpuCoreUse}  WHERE id = #{userId}")
    int updateResourceUsed(Long userId, Long cpuUse, Long memoryUse, Long gpuMemoryUse, Long vGpuCoreUse);

    @Select("SELECT * FROM user WHERE department_id = #{departmentId} AND deleted = 0")
    List<User> findUsersInDepartment(Long departmentId);

    @Select("SELECT gpu_memory_used FROM user WHERE id = #{userId}")
    Long findGpuMemoryUsedByUserId(Long id);

    @Select("SELECT v_gpu_cores_used FROM user WHERE id = #{userId}")
    Long findVGpuCoresUsedByUserId(Long id);

    @Delete("DELETE FROM users_roles WHERE user_id = #{userId}")
    void deleteUserRoleByUserId(Long userId);

    @Update("<script>UPDATE user SET department_id = null WHERE department_id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    void unbindUserDepartmentByDepartmentIds(@Param("ids") Set<Long> ids);
}
