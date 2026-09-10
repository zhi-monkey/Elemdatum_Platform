package org.dubhe.admin.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.admin.domain.dto.DepartmentDTO;
import org.dubhe.admin.domain.dto.DepartmentDeleteDTO;
import org.dubhe.admin.domain.entity.Department;
import org.dubhe.admin.domain.entity.User;
import org.dubhe.admin.service.DepartmentService;
import org.dubhe.admin.service.UserService;
import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.cloud.authconfig.audit.AuditLogModel;
import org.dubhe.cloud.authconfig.audit.AuditLogHelper;
import org.dubhe.cloud.authconfig.audit.SystemControllerLog;
import org.dubhe.cloud.authconfig.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Api(tags = "部门管理")
@RestController
@RequestMapping("/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserContextService userContextService;
    @Autowired
    private AuditLogHelper auditLogHelper;

    @ApiOperation("分页查询部门")
    @GetMapping
    public DataResponseBody getDepartments(
            @RequestParam(required = false, defaultValue = "1") int current,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "id") String sort,
            @RequestParam(required = false, defaultValue = "asc") String order) {

        Page<Department> page = new Page<>(current, size);
        return new DataResponseBody(departmentService.getDepartments(page, name, sort, order));
    }

    @ApiOperation("新增或修改部门")
    @PostMapping
    public DataResponseBody saveOrUpdate(@Valid @RequestBody DepartmentDTO departmentDTO) {
        boolean isCreate = departmentDTO.getId() == null;
        departmentService.saveOrUpdateDepartment(departmentDTO);
        if (isCreate) {
            saveAddAuditLog("department_add", "  name: " + departmentDTO.getName());
        } else {
            auditLogHelper.saveUpdateAuditLog("department_update",
                    "  departmentId: " + departmentDTO.getId() + "  name: " + departmentDTO.getName());
        }
        return new DataResponseBody();
    }

    @ApiOperation("删除部门")
    @DeleteMapping("/{departmentId}")
    @SystemControllerLog(description = "department_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody delete(@PathVariable Long departmentId) {
        List<Long> userIds = departmentService.findUserIdsByDepartmentId(departmentId);
        departmentService.unbindUsersFromDepartment(departmentId, userIds);
        departmentService.removeById(departmentId);
        return new DataResponseBody();
    }

    @ApiOperation("批量删除部门")
    @DeleteMapping()
    public DataResponseBody delete(@RequestBody DepartmentDeleteDTO dto) {
        userService.unbindUsersFromDepartment(dto.getIds());
        departmentService.removeByIds(dto.getIds());
        return new DataResponseBody();
    }

    @ApiOperation("绑定用户到部门")
    @PostMapping("/bindUsers")
    public DataResponseBody bindUsersToDepartment(@RequestParam Long departmentId, @RequestBody List<Long> userIds) {
        departmentService.bindUsersToDepartment(departmentId, userIds);
        return new DataResponseBody();
    }

    @ApiOperation("解绑用户从部门")
    @PostMapping("/unbindUsers")
    public DataResponseBody unbindUsersFromDepartment(@RequestParam Long departmentId, @RequestBody List<Long> userIds) {
        departmentService.unbindUsersFromDepartment(departmentId, userIds);
        return new DataResponseBody();
    }

    @ApiOperation("更新部门用户绑定")
    @PostMapping("/updateBindings")
    public DataResponseBody updateDepartmentBindings(@RequestParam Long departmentId, @RequestBody List<Long> newUserIds) {
        departmentService.updateDepartmentBindings(departmentId, newUserIds);
        auditLogHelper.saveUpdateAuditLog("department_binding_update", "  departmentId: " + departmentId);
        return new DataResponseBody();
    }

    @ApiOperation("查询部门内的用户和未分配部门的用户")
    @GetMapping("/{departmentId}/users")
    public DataResponseBody getDepartmentAndUnassignedUsers(@PathVariable Long departmentId) {
        List<User> departmentUsers = departmentService.getUsersByDepartment(departmentId);
        List<User> unassignedUsers = departmentService.getUsersWithoutDepartment();
        List<User> allUsers = new ArrayList<>();
        allUsers.addAll(departmentUsers);
        allUsers.addAll(unassignedUsers);
        return new DataResponseBody(allUsers);
    }

    @ApiOperation("查询部门内的用户")
    @GetMapping("/{departmentId}/department-users")
    public DataResponseBody getDepartmentUsers(@PathVariable Long departmentId) {
        List<User> departmentUsers = departmentService.getUsersByDepartment(departmentId);
        List<User> allUsers = new ArrayList<>();
        allUsers.addAll(departmentUsers);
        return new DataResponseBody(allUsers);
    }

    @ApiOperation("查询未分配部门的用户")
    @GetMapping("/unassigned-users")
    public DataResponseBody getUnassignedUsers() {
        return new DataResponseBody(departmentService.getUsersWithoutDepartment());
    }

    @ApiOperation("查询个人资源使用量")
    @GetMapping("/{departmentId}/submember-info")
    public DataResponseBody querySubMemberInfo(
            @PathVariable Long departmentId,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        return new DataResponseBody(departmentService.queryMemberUsedInfo(departmentId, sortBy, order));
    }

    @ApiOperation("查询所有部门")
    @GetMapping("/all")
    public DataResponseBody getAll() {
        List<Department> departments = new ArrayList<>();
        departments.addAll(departmentService.getAllDepart());
        return new DataResponseBody(departments);
    }

    @GetMapping("/isnameexist/{name}")
    public DataResponseBody isDepartmentNameExists(@PathVariable String name) {
        boolean exists = departmentService.isDepartmentExist(name);
        return new DataResponseBody(exists);
    }

    private void saveAddAuditLog(String description, String params) {
        UserContext userContext = userContextService.getCurUser();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (userContext == null || attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        AuditLogModel auditLogModel = new AuditLogModel();
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setOperationType(AuditLogConstants.OperationType.ADD);
        auditLogModel.setUid(userContext.getId().intValue());
        auditLogModel.setUname(userContext.getUsername());
        auditLogModel.setCreateDate(new Date());
        auditLogModel.setIp(request.getRemoteAddr());
        auditLogModel.setRequestUri(request.getRequestURI());
        auditLogModel.setMethod(request.getMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(description);
        auditLogModel.setParams(params);
        auditLogService.saveAuditLog(auditLogModel);
    }
}
