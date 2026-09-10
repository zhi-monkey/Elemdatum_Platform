
package org.dubhe.admin.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dubhe.admin.domain.entity.DictDetail;

import java.io.Serializable;
import java.util.List;

/**
 * @description  字典详情 mapper
 * @date 2020-03-26
 */
public interface DictDetailMapper extends BaseMapper<DictDetail> {
    /**
     * 根据字典ID查找
     *
     * @param dictId
     * @return
     */
    @Select("select * from dict_detail where dict_id =#{dictId} order by sort")
    List<DictDetail> selectByDictId(Serializable dictId);

    /**
     * 根据字典ID和标签查找
     *
     * @param dictId
     * @param label
     * @return
     */
    @Select("select * from dict_detail where dict_id=#{dictId} and label=#{label}")
    DictDetail selectByDictIdAndLabel(Serializable dictId, String label);

    /**
     * 根据字典ID删除
     *
     * @param dictId
     * @return
     */
    @Update("delete from dict_detail where dict_id =#{dictId}")
    int deleteByDictId(Serializable dictId);
}
