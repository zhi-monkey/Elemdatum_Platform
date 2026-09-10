package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.dubhe.data.domain.entity.VideoDataset;

public interface VideoDatasetMapper extends BaseMapper<VideoDataset> {
    @Update("UPDATE video_dataset SET file_count = COALESCE(file_count, 0) + 1, " +
            "status = 2003, upload_status = 'READY', upload_error = NULL WHERE id = #{datasetId} AND deleted = 0")
    int incrementFileCountAfterUpload(@Param("datasetId") Long datasetId);

    @Update("UPDATE video_dataset SET status = 2002, upload_status = 'UPLOADING', upload_error = NULL " +
            "WHERE id = #{datasetId} AND deleted = 0")
    int markUploading(@Param("datasetId") Long datasetId);
}
