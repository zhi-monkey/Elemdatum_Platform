package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dubhe.data.domain.entity.InferenceDevice;

@Mapper
public interface InferenceDeviceMapper extends BaseMapper<InferenceDevice> {
}
