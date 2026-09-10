

package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.dubhe.data.domain.dto.AutoLabelModelServiceQueryDTO;
import org.dubhe.data.domain.entity.AutoLabelModelService;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since 2022-05-25
 */
public interface AutoLabelModelServiceMapper extends BaseMapper<AutoLabelModelService> {

    int deleteByIds(@Param("ids") List<Long> ids);

    List<AutoLabelModelService> selectByIds(@Param("ids") List<Long> ids);

    int updataStatusById(@Param("id") Long id,@Param("status") Integer status);

    @Update("update auto_label_model_service set status = #{status} where id = #{modelServiceId}")
    void updateStatus(@Param("modelServiceId") Long modelServiceId, @Param("status") Integer status);
}
