package org.dubhe.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.admin.dao.DepartmentMapper;
import org.dubhe.admin.dao.UserMapper;
import org.dubhe.admin.domain.dto.DepartmentDTO;
import org.dubhe.admin.domain.dto.DepartmentQueryDTO;
import org.dubhe.admin.domain.entity.Department;
import org.dubhe.admin.domain.entity.User;
import org.dubhe.admin.domain.vo.DepartmentSubMemberInfoVO;
import org.dubhe.admin.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Autowired
    private UserMapper userMapper; // 假设有 UserMapper

    @Autowired
    private DepartmentMapper departmentMapper;

    @Override
    public IPage<DepartmentQueryDTO> getDepartments(Page<Department> page, String name, String sort, String order) {
        QueryWrapper<Department> queryWrapper = new QueryWrapper<>();

        // 按名称查询
        if (name != null && !name.isEmpty()) {
            queryWrapper.like("name", name);
        }

        // 设置排序
        if ("desc".equalsIgnoreCase(order)) {
            queryWrapper.orderByDesc(sort);
        } else {
            queryWrapper.orderByAsc(sort);
        }

        IPage<Department> departmentPage = this.page(page, queryWrapper);
        List<DepartmentQueryDTO> departmentQueryDTOs = departmentPage.getRecords().stream().map(department -> {
            DepartmentQueryDTO dto = new DepartmentQueryDTO();
            dto.setId(department.getId());
            dto.setName(department.getName());
            dto.setDescription(department.getDescription());
            dto.setMemoryLimit(department.getMemoryLimit());
            dto.setCpuLimit(department.getCpuLimit());
            dto.setGpuMemoryLimit(department.getGpuMemoryLimit());
            dto.setVGpuCoresLimit(department.getVGpuCoresLimit());

            // 获取用户ID列表
            List<Long> userIds = userMapper.findUserIdsByDepartmentId(department.getId());
            dto.setUserIds(userIds);
            dto.setUserCount(userIds.size());
            long memoryUsed = 0L;
            long cpuUsed = 0L;
            long gpuMemoryUsed = 0L;
            long vGpuCoresUsed = 0L;
            for( Long id : userIds){
                Long memoryUsedByUserId = userMapper.findMemoryUsedByUserId(id);
                Long cpuUsedByUserId = userMapper.findCpuUsedByUserId(id);
                Long gpuMemoryUsedByUserId = userMapper.findGpuMemoryUsedByUserId(id);
                Long vGpuCoresUsedByUserId = userMapper.findVGpuCoresUsedByUserId(id);
                if(memoryUsedByUserId != null){
                    memoryUsed += memoryUsedByUserId;
                }
                if(cpuUsedByUserId != null){
                    cpuUsed += cpuUsedByUserId;
                }
                if(gpuMemoryUsedByUserId != null){
                    gpuMemoryUsed += gpuMemoryUsedByUserId;
                }
                if(vGpuCoresUsedByUserId != null){
                    vGpuCoresUsed += vGpuCoresUsedByUserId;
                }
            }
            dto.setMemoryUsed(memoryUsed);
            dto.setCpuUsed(cpuUsed);
            dto.setGpuMemoryUsed(gpuMemoryUsed);
            dto.setVGpuCoresUsed(vGpuCoresUsed);

            return dto;
        }).collect(Collectors.toList());

        // 构建分页结果
        IPage<DepartmentQueryDTO> resultPage = new Page<>(departmentPage.getCurrent(), departmentPage.getSize(), departmentPage.getTotal());
        resultPage.setRecords(departmentQueryDTOs);
        return resultPage;
    }

    @Transactional
    @Override
    public void bindUsersToDepartment(Long departmentId, List<Long> userIds) {
        for (Long userId : userIds) {
            userMapper.updateDepartmentId(userId, departmentId);
        }
    }

    @Transactional
    @Override
    public void unbindUsersFromDepartment(Long departmentId, List<Long> userIds) {
        for (Long userId : userIds) {
            userMapper.updateDepartmentId(userId, null);
        }
    }

    @Transactional
    @Override
    public void updateDepartmentBindings(Long departmentId, List<Long> newUserIds) {
        // 解绑所有当前绑定的用户
        List<Long> currentUserIds = userMapper.findUserIdsByDepartmentId(departmentId);
        unbindUsersFromDepartment(departmentId, currentUserIds);

        // 绑定新的用户
        bindUsersToDepartment(departmentId, newUserIds);
    }

    @Override
    public List<User> getUsersByDepartment(Long departmentId) {
        return userMapper.findUsersByDepartmentId(departmentId);
    }

    @Override
    public List<User> getUsersWithoutDepartment() {
        return userMapper.findUsersWithoutDepartment();
    }

    // 检查部门名称是否存在
    @Override
    public boolean isDepartmentExist(String departmentName) {
        Long departmentId = departmentMapper.findIdByName(departmentName);
        return departmentId != null; // 如果返回的ID不为null，表示该部门存在
    }

    @Transactional
    @Override
    public void saveOrUpdateDepartment(DepartmentDTO departmentDTO) {
        // 保存或更新部门
        Department department = departmentDTO.toEntity();
        boolean isNew = department.getId() == null;

        if (isNew) {
            this.save(department);
        } else {
            this.updateById(department);
        }

        List<Long> userIds = departmentDTO.getUserIds();

        // 如果 userIds 不为空，则进行绑定操作
        if (userIds != null && !userIds.isEmpty()) {
            // 检查用户是否已绑定其他部门
            for (Long userId : userIds) {
                Long existingDepartmentId = userMapper.findDepartmentIdByUserId(userId);
                if (existingDepartmentId != null && !existingDepartmentId.equals(department.getId())) {
                    throw new IllegalArgumentException("用户 " + userId + " 已绑定其他部门");
                }
            }

            // 更新用户与部门的绑定关系
            userMapper.unbindUsersFromDepartment(department.getId());
            userMapper.bindUsersToDepartment(department.getId(), userIds);
        } else if (!isNew) {
            // 如果是修改操作且 userIds 为空，清除该部门的所有绑定关系
            userMapper.unbindUsersFromDepartment(department.getId());
        }
        // 如果是新建操作且 userIds 为空，不进行任何绑定操作
    }

    @Override
    public List<Long> findUserIdsByDepartmentId(Long departmentId) {
        return userMapper.findUserIdsByDepartmentId(departmentId);
    }

    @Override
    public List<Department> getAllDepart() {
        return departmentMapper.selectAll();
    }

//    @Override
//    public DepartmentSubMemberInfoVO queryMemberUsedInfo(Long departmentId) {
//        Department department = departmentMapper.selectById(departmentId);
//        List<User> users = userMapper.findUsersByDepartmentId(departmentId);
//        DepartmentSubMemberInfoVO departmentSubMemberInfoVO = DepartmentSubMemberInfoVO.builder().build();
//        departmentSubMemberInfoVO.setId(department.getId());
//        departmentSubMemberInfoVO.setName(department.getName());
//        departmentSubMemberInfoVO.setUserCount(users.size());
//        departmentSubMemberInfoVO.setMemoryLimit(department.getMemoryLimit());
//        departmentSubMemberInfoVO.setCpuLimit(department.getCpuLimit());
//        departmentSubMemberInfoVO.setGpuMemoryLimit(department.getGpuMemoryLimit());
//        departmentSubMemberInfoVO.setUsers(users);
//        return departmentSubMemberInfoVO;
//    }

    @Override
    public DepartmentSubMemberInfoVO queryMemberUsedInfo(Long departmentId, String sortBy, String order) {
        Department department = departmentMapper.selectById(departmentId);
        List<User> users = userMapper.findUsersByDepartmentId(departmentId);

        // 排序用户列表
        if (users != null && !users.isEmpty()) {
            if ("asc".equalsIgnoreCase(order)) {
                users.sort(Comparator.comparing(user -> getUserFieldValue(user, sortBy)));
            } else if ("desc".equalsIgnoreCase(order)) {
                users.sort((u1, u2) -> getUserFieldValue(u2, sortBy).compareTo(getUserFieldValue(u1, sortBy)));
            }
        }

        // 构建返回的 VO
        DepartmentSubMemberInfoVO departmentSubMemberInfoVO = DepartmentSubMemberInfoVO.builder().build();
        departmentSubMemberInfoVO.setId(department.getId());
        departmentSubMemberInfoVO.setName(department.getName());
        departmentSubMemberInfoVO.setUserCount(users.size());
        departmentSubMemberInfoVO.setMemoryLimit(department.getMemoryLimit());
        departmentSubMemberInfoVO.setCpuLimit(department.getCpuLimit());
        departmentSubMemberInfoVO.setGpuMemoryLimit(department.getGpuMemoryLimit());
        departmentSubMemberInfoVO.setUsers(users);

        return departmentSubMemberInfoVO;
    }

    // 根据传入的字段名获取对应的用户属性值
    private Comparable getUserFieldValue(User user, String field) {
        switch (field) {
            case "id":
                return user.getId();
//            case "name":
//                return user.getName();
//            case "cpuUsage":
//                return user.getCpuUsage();
//            case "memoryUsage":
//                return user.getMemoryUsage();
            // 添加更多的字段支持
            default:
                return user.getId(); // 默认按 id 排序
        }
    }
}
