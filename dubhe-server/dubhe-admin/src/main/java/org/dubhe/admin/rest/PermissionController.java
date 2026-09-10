
package org.dubhe.admin.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.admin.domain.dto.PermissionCreateDTO;
import org.dubhe.admin.domain.dto.PermissionDeleteDTO;
import org.dubhe.admin.domain.dto.PermissionQueryDTO;
import org.dubhe.admin.domain.dto.PermissionUpdateDTO;
import org.dubhe.admin.service.PermissionService;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description 操作权限控制器
 * @date 2021-04-26
 */
@Api(tags = "系统：操作权限")
@RestController
@RequestMapping("/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @ApiOperation("返回全部的操作权限")
    @GetMapping(value = "/tree")
    public DataResponseBody getPermissionTree() {
        return new DataResponseBody(permissionService.getPermissionTree(permissionService.findByPid(0L)));
    }

    @ApiOperation("查询权限")
    @GetMapping
    @PreAuthorize(Permissions.AUTH_CODE)
    public DataResponseBody queryAll(PermissionQueryDTO queryDTO) {
        return new DataResponseBody(permissionService.queryAll(queryDTO));
    }

    @ApiOperation("新增权限")
    @PostMapping
    @PreAuthorize(Permissions.PERMISSION_CREATE)
    public DataResponseBody create(@Validated @RequestBody PermissionCreateDTO permissionCreateDTO) {
        permissionService.create(permissionCreateDTO);
        return new DataResponseBody();
    }

    @ApiOperation("修改权限")
    @PutMapping
    @PreAuthorize(Permissions.PERMISSION_EDIT)
    public DataResponseBody update(@Validated @RequestBody PermissionUpdateDTO permissionUpdateDTO) {
        permissionService.update(permissionUpdateDTO);
        return new DataResponseBody();
    }

    @ApiOperation("删除权限")
    @DeleteMapping
    @PreAuthorize(Permissions.PERMISSION_DELETE)
    public DataResponseBody delete(@RequestBody PermissionDeleteDTO permissionDeleteDTO) {
        permissionService.delete(permissionDeleteDTO);
        return new DataResponseBody();
    }


}
