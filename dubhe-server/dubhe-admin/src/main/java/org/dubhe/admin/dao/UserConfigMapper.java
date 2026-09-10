
package org.dubhe.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.dubhe.admin.domain.entity.UserConfig;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @description 用户配置 Mapper
 * @date 2021-6-30
 */
public interface UserConfigMapper extends BaseMapper<UserConfig> {

    /**
     * 插入或更新配置
     * @param userConfig 用户配置
     */
    Long insertOrUpdate(UserConfig userConfig);
}
