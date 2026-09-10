
package org.dubhe.admin.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.admin.domain.dto.*;
import org.dubhe.admin.service.UserService;
import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.dto.UserDTO;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.cloud.authconfig.audit.AuditLogHelper;
import org.dubhe.cloud.authconfig.audit.SystemControllerLog;
import org.dubhe.cloud.authconfig.online.OnlineUserRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description 用户管理
 * @date 2020-11-03
 */
@Api(tags = "系统：用户管理")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private AuditLogHelper auditLogHelper;

    @ApiOperation("查询角色人数统计和总人数")
    @GetMapping("/roleCountsAndTotal")
    public DataResponseBody getRoleUserCountsAndTotalCount() {
        // 直接获取角色统计和总人数
        Map<String, Object> result = userService.getRoleUserCountsAndTotalCount();
        return new DataResponseBody(result);
    }

    @ApiOperation("查询已启用用户数量")
    @GetMapping("/getEnableUserStats")
    public DataResponseBody<Long> getEnableUserStats(){
        return new DataResponseBody<>(userService.getEnabledUserStats());
    }

    @ApiOperation("导出用户数据")
    @GetMapping(value = "/download")
    @PreAuthorize(Permissions.USER_DOWNLOAD)
    public void download(HttpServletResponse response, UserQueryDTO criteria) throws IOException {
        userService.download(userService.queryAll(criteria), response);
    }

    @ApiOperation("查询用户")
    @GetMapping
    public DataResponseBody getUsers(UserQueryDTO criteria, Page page) {
        return new DataResponseBody(userService.queryAll(criteria, page));
    }

    @ApiOperation("新增用户")
    @PostMapping
    @PreAuthorize(Permissions.USER_CREATE)
    @SystemControllerLog(description = "user_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody create(@Valid @RequestBody UserCreateDTO userCreateDTO) {
        return new DataResponseBody(userService.create(userCreateDTO));
    }

    @ApiOperation("修改用户")
    @PutMapping
    @PreAuthorize(Permissions.USER_EDIT)
    public DataResponseBody update(@Valid @RequestBody UserUpdateDTO resources) {
        Object result = userService.update(resources);
        auditLogHelper.saveUpdateAuditLog("user_update",
                "  userId: " + resources.getId() + "  username: " + resources.getUsername());
        return new DataResponseBody(result);
    }

    @ApiOperation("删除用户")
    @DeleteMapping
    @PreAuthorize(Permissions.USER_DELETE)
    @SystemControllerLog(description = "user_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody delete(@Valid @RequestBody UserDeleteDTO userDeleteDTO) {
        userService.delete(userDeleteDTO.getIds());
        return new DataResponseBody();
    }

    @ApiOperation("根据用户ID查询用户配置")
    @GetMapping(value = "/getUserConfig")
    public DataResponseBody getUserConfig(@RequestParam(value = "userId") Long userId) {
        return new DataResponseBody(userService.findUserConfig(userId));
    }

    @ApiOperation("新增或修改用户配置")
    @PutMapping(value = "/setUserConfig")
    @PreAuthorize(Permissions.USER_CONFIG_EDIT)
    public DataResponseBody setUserConfig(@Validated @RequestBody UserConfigDTO userConfigDTO) {
        return new DataResponseBody(userService.createOrUpdateUserConfig(userConfigDTO));
    }

    /**
     * 此接口提供给Auth模块获取用户信息使用
     * 因Auth获取用户信息在登录时是未登录状态，请不要在此添加权限校验
     * @param username
     * @return
     */
    @ApiOperation("根据用户名称查找用户")
    @GetMapping(value = "/findUserByUsername")
    public DataResponseBody<UserContext> findUserByUsername(@RequestParam(value = "username") String username) {
        DataResponseBody<UserContext> userContextDataResponseBody = userService.findUserByUsername(username);
        return userContextDataResponseBody;
    }


    @ApiOperation("根据用户ID查询用户信息(服务内部访问)")
    @GetMapping(value = "/findById")
    public DataResponseBody<UserDTO> getUsers(@RequestParam(value = "userId") Long userId) {
        return new DataResponseBody(userService.findById(userId));
    }

    @ApiOperation("根据用户ID查询用户姓名")
    @GetMapping(value = "/findNameById")
    public String getNameById(@RequestParam(value = "userId") Long userId) {
        return userService.findNameById(userId);
    }

    @ApiOperation("根据用户昵称搜索用户列表")
    @GetMapping(value = "/findByNickName")
    public DataResponseBody<List<UserDTO>> findByNickName(@RequestParam(value = "nickName", required = false) String nickName) {
        return new DataResponseBody(userService.findByNickName(nickName));
    }

    @ApiOperation("根据用户ID批量查询用户信息(服务内部访问)")
    @GetMapping(value = "/findByIds")
    public DataResponseBody<List<UserDTO>> getUserList(@RequestParam(value = "ids") List<Long> ids) {
        return new DataResponseBody(userService.getUserList(ids));
    }

    @ApiOperation("根据用户名模糊匹配返回用户ID列表")
    @GetMapping(value = "/findIdsByUsernameLike")
    public DataResponseBody<List<Long>> findIdsByUsernameLike(@RequestParam(value = "userName") String userName) {
        return new DataResponseBody(userService.findIdsByUsernameLike(userName));
    }

    @ApiOperation("见微平台专用")
    @GetMapping("/decryptVisUser")
    public DataResponseBody<String> encryptVisUser(Authentication authentication) {
        return new DataResponseBody<>(userService.encryptUserForVis(authentication));
    }

    @ApiOperation("统计用户状态信息")
    @GetMapping("/getUserNum")
    public DataResponseBody<UserStatisticsDTO> getUserNum(){
        return new DataResponseBody<>(userService.getUserNum());
    }

    @ApiOperation("查询所有自己创建的用户")
    @GetMapping("/queryAllUsers")
    public DataResponseBody<List<UserDTO>> queryAllUsers(UserQueryDTO criteria){
        return new DataResponseBody<>(userService.queryAll(criteria));
    }

    @ApiOperation("查询拥有单一权限的用户")
    @GetMapping("/getUsersWithPermission")
    public DataResponseBody<List<UserDTO>> getUsersWithPermission(String permissionName){
        return new DataResponseBody<>(userService.getUsersWithPermission(permissionName));
    }

    @ApiOperation("查询拥有单一权限的用户(不排除当前用户）")
    @GetMapping("/getUsersWithPermissionDoNotExcludeCurrentUser")
    public DataResponseBody<List<UserDTO>> getUsersWithPermissionDoNotExcludeCurrentUser(String permissionName){
        return new DataResponseBody<>(userService.getUsersWithPermissionDoNotExcludeCurrentUser(permissionName));
    }

    @ApiOperation("查询在线用户信息（数量、用户、在线时长、用户ID）")
    @GetMapping("/getOnlineUserStats")
    public DataResponseBody<Map<String, Object>> getOnlineUserStats(){
        // 使用基于拦截器的在线用户注册表统计最近一段时间内活跃的用户
        Set<UserContext> onlineUsers = OnlineUserRegistry.getOnlineUsers();
        Map<Long, Long> onlineDurations = OnlineUserRegistry.getOnlineDurations();
        Set<Long> onlineUserIds = OnlineUserRegistry.getOnlineUserIds();

        Map<String, Object> result = new HashMap<>(4);
        result.put("count", onlineUserIds.size());
        result.put("users", onlineUsers);
        result.put("durations", onlineDurations);
        result.put("userIds", onlineUserIds);
        return new DataResponseBody<>(result);
    }

    /**
     * 增加用户的cpu和memory的使用量
     * @param addUserResourceDTO cpu memory user信息
     * @return {@link String }
     */
    @ApiOperation("增加用户的cpu和memory的使用量")
    @PostMapping("/addUserResource")
    public String addUserResource(@RequestBody AddUserResourceDTO addUserResourceDTO){
    	return userService.addUserResource(addUserResourceDTO);
    }

    /**
     * 释放用户占用的cpu和memory资源
     * @param addUserResourceDTO cpu memory user信息
     * @return {@link String }
     */
    @ApiOperation("释放用户占用的cpu和memory资源")
    @PostMapping("/releaseUserResource")
    public String releaseUserResource(@RequestBody AddUserResourceDTO addUserResourceDTO){
    	return userService.releaseUserResource(addUserResourceDTO);
    }

    @ApiOperation("获取用户资源使用情况")
    @GetMapping("/getUserResourceUsed")
    public DataResponseBody getUserResourceUsed(@RequestParam(value = "userId") Long userId){
    	return new DataResponseBody(userService.getUserResourceUsed(userId));
    }
}
