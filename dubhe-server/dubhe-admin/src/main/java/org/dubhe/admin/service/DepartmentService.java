package org.dubhe.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.admin.domain.dto.DepartmentDTO;
import org.dubhe.admin.domain.dto.DepartmentQueryDTO;
import org.dubhe.admin.domain.entity.Department;
import org.dubhe.admin.domain.entity.User;
import org.dubhe.admin.domain.vo.DepartmentSubMemberInfoVO;

import java.util.List;

public interface DepartmentService extends IService<Department> {
    IPage<DepartmentQueryDTO> getDepartments(Page<Department> page, String name, String sort, String order);

    void bindUsersToDepartment(Long departmentId, List<Long> userIds);

    void unbindUsersFromDepartment(Long departmentId, List<Long> userIds);

    void updateDepartmentBindings(Long departmentId, List<Long> newUserIds);

    List<User> getUsersByDepartment(Long departmentId);

    List<User> getUsersWithoutDepartment();

    void saveOrUpdateDepartment(DepartmentDTO departmentDTO);

    List<Long> findUserIdsByDepartmentId(Long departmentId);

    List<Department> getAllDepart();

//    DepartmentSubMemberInfoVO queryMemberUsedInfo(Long departmentId);

    DepartmentSubMemberInfoVO queryMemberUsedInfo(Long departmentId, String sortBy, String order);
    boolean isDepartmentExist(String departmentName);
}

