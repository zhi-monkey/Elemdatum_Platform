package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.entity.LabelMapping;

import java.util.List;

/**
 * @author 10230
 */
@DataPermission(ignoresMethod = {"insert", "selectById","findByDatasetVersionId"})
public interface LabelMappingMapper extends BaseMapper<LabelMapping> {

    /**
     * 根据数据集版本ID查询对应的标签映射关系
     *
     * @param datasetVersionId 数据集版本ID
     * @return 标签映射关系列表
     */
    @Select("SELECT * FROM label_mapping WHERE dataset_version_id = #{datasetVersionId}")
    List<LabelMapping> findByDatasetVersionId(Long datasetVersionId);


    /**
     * 重写 insert 方法，进行自定义插入操作
     *
     * @param labelMapping 标签映射对象
     * @return 插入结果
     */
    @Override
    @Insert("INSERT INTO label_mapping (dataset_version_id, source_label_id, target_label_id, deleted) " +
            "VALUES (#{datasetVersionId}, #{sourceLabelId}, #{targetLabelId}, #{deleted})")
    int insert(LabelMapping labelMapping);

}
