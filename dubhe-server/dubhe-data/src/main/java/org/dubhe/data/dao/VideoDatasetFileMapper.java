package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dubhe.data.domain.entity.VideoDatasetFile;

import java.util.Map;

public interface VideoDatasetFileMapper extends BaseMapper<VideoDatasetFile> {

    /**
     * 按抽帧状态统计视频文件数量（用于详情页信息总览）
     *
     * @param datasetId 视频数据集 id
     * @return 键 extracted / unextracted，值为对应数量
     */
    @Select("SELECT " +
            "SUM(CASE WHEN extract_status = 'EXTRACTED' THEN 1 ELSE 0 END) AS extracted, " +
            "SUM(CASE WHEN extract_status <> 'EXTRACTED' THEN 1 ELSE 0 END) AS unextracted " +
            "FROM video_dataset_file WHERE dataset_id = #{datasetId} AND deleted = 0")
    Map<String, Object> countByExtractStatus(@Param("datasetId") Long datasetId);
}
