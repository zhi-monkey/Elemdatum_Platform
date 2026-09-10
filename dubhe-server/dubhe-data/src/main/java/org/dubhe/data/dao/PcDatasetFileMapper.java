package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.dubhe.data.domain.entity.PcDatasetFile;

public interface PcDatasetFileMapper extends BaseMapper<PcDatasetFile> {

    /**
     * 软删除（逻辑删除）单个 PCD 文件
     * <p>
     * 显式使用 SQL 更新 deleted 字段，避免 @TableLogic 与 setDeleted 混用导致
     * 旧版 MyBatis-Plus 的类型推断异常；同时更新 update_time。
     * </p>
     *
     * @param id 文件ID
     * @return 受影响行数
     */
    @Update("UPDATE pc_dataset_file SET deleted = 1, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int logicDeleteById(@Param("id") Long id);
}
