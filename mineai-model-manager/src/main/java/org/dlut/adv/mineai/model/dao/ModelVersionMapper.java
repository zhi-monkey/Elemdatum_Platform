package org.dlut.adv.mineai.model.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dlut.adv.mineai.core.entity.ModelVersion;

@Mapper
public interface ModelVersionMapper extends BaseMapper<ModelVersion> {

    ModelVersion getModelVersionByShowNameAndLevel(@Param("showName") String showName, @Param("level") String level);

    ModelVersion getModelVersionByName(@Param("name") String name);

    ModelVersion getModelVersionByNameAndNotDelete(@Param("name") String name);
}
