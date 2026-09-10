
package org.dubhe.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dubhe.admin.domain.entity.Group;
import org.dubhe.admin.domain.entity.User;

import java.util.List;

/**
 * @description 用户组mapper接口
 * @date 2021-05-08
 */
public interface UserGroupMapper extends BaseMapper<Group> {

    /**
     *  获取用户组成员信息
     *
     * @param groupId 用户组id
     * @return List<User> 用户列表
     */
    @Select("select u.* from user u left join user_group gu on u.id = gu.user_id where gu.group_id=#{groupId} and deleted=0 ")
    List<User> queryUserByGroupId(Long groupId);

    /**
     * 删除用户组
     *
     * @param groupId 用户组id
     */
    @Update("update pt_group set deleted=1 where id=#{groupId} ")
    void delUserGroupByGroupId(Long groupId);

    /**
     * 清空用户组成员
     *
     * @param groupId 用户组id
     */
    @Delete("delete from user_group where group_id=#{groupId} ")
    void delUserByGroupId(Long groupId);

    /**
     * 新增用户组成员
     *
     * @param groupId 用户组id
     * @param userId 用户id
     */
    @Insert("insert into user_group values (#{groupId},#{userId})")
    void addUserWithGroup(Long groupId, Long userId);

    /**
     * 删除用户组成员
     *
     * @param groupId 用户组id
     * @param userId 用户id
     */
    @Delete("delete from user_group where group_id=#{groupId} and user_id=#{userId}")
    void delUserWithGroup(Long groupId, Long userId);

    /**
     * 获取还未分组的用户
     *
     * @return List<User> 用户组成员列表
     */
    @Select("select * from user where id not in (select user_id from user_group) and deleted=0")
    List<User> findUserWithOutGroup();

}
