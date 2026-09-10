
package org.dubhe.data.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dubhe.data.domain.entity.PcDataset;

/**
 * @description 点云数据集
 * @date 2022-04-01
 **/
public interface PcDatasetMapper extends BaseMapper<PcDataset> {
    /**
     * 根据标签组ID查询关联的点云数据集数量
     *
     * @param labelGroupId 标签组ID
     * @return int 数量
     */
    @Select("SELECT count(1) FROM pc_dataset where label_group_id = #{labelGroupId}")
    int getCountPCByLabelGroupId(@Param("labelGroupId") Long labelGroupId);

    @Update("UPDATE pc_dataset SET file_count = COALESCE(file_count, 0) + 1, " +
            "status = 1003, upload_status = 'READY', upload_error = NULL WHERE id = #{datasetId} AND deleted = 0")
    int incrementFileCountAfterUpload(@Param("datasetId") Long datasetId);

    @Update("UPDATE pc_dataset SET file_count = GREATEST(COALESCE(file_count, 0) - 1, 0) " +
            "WHERE id = #{datasetId} AND deleted = 0")
    int decrementFileCountAfterDelete(@Param("datasetId") Long datasetId);

    @Update("UPDATE pc_dataset SET status = 1002, upload_status = 'UPLOADING', upload_error = NULL " +
            "WHERE id = #{datasetId} AND deleted = 0")
    int markUploading(@Param("datasetId") Long datasetId);
}
