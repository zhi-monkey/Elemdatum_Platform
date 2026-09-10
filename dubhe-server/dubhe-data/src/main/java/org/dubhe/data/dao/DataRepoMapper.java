package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.entity.DataRepo;

@DataPermission(ignoresMethod = {"insert", "selectById", "selectCountByPublic", "selectList", "dataVersionListVO", "selectPage"})
public interface DataRepoMapper extends BaseMapper<DataRepo> {
    @Select("SELECT id FROM data_repo WHERE name = #{repoName}")
    Long findIdByName(String repoName);
}
