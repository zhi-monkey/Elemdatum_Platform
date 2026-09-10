package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.dubhe.data.domain.entity.DataTeamUser;

import java.util.List;
import java.util.Set;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.dao
 * @Project：mineai
 * @name：DataTeamUserMapper
 * @Date：2024/3/11 16:04
 * @Filename：DataTeamUserMapper
 * @Desc：
 */
public interface DataTeamUserMapper extends BaseMapper<DataTeamUser> {

    // 批量添加团队用户
    void insertBatchs(List<DataTeamUser> dataTeamUsers);

    // 批量删除团队用户
    void deleteByDataTeamId(@Param("list") Set<Long> dataTeamIds);
}
