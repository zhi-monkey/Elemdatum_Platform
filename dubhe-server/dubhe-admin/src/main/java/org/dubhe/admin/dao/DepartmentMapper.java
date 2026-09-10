package org.dubhe.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.dubhe.admin.domain.entity.Department;
import org.dubhe.biz.base.annotation.DataPermission;

import java.util.List;

@Mapper
@DataPermission(ignoresMethod = {"selectById", "selectAll", "selectPage", "updateById", "insert", "findIdByName","deleteById","deleteBatchIds"})
public interface DepartmentMapper extends BaseMapper<Department> {
    @Select("SELECT * FROM department")
    List<Department> selectAll();


    @Select("SELECT id FROM department WHERE name = #{departmentName}")
    Long findIdByName(String departmentName);

}