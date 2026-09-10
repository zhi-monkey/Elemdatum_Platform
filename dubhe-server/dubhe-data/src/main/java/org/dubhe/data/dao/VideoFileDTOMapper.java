package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.*;
import org.dubhe.data.domain.dto.*;

import java.util.List;

@Mapper
public interface VideoFileDTOMapper {
    //通过数据集id获取其中的视频
    @Select({
            "<script>",
            "SELECT id, name, status, create_time, update_time, url FROM data_file WHERE dataset_id = #{datasetId} AND file_type = 1 AND deleted = 0",
            "<if test='status != null'> AND status = #{status}</if>",
            "<if test='name != null'> AND name LIKE CONCAT('%', #{name}, '%')</if>",
            "<if test='createTimeStart != null'> AND create_time &gt;= #{createTimeStart}</if>",
            "<if test='createTimeEnd != null'> AND create_time &lt;= #{createTimeEnd}</if>",
            "ORDER BY id ${order}",
            "</script>"
    })
    IPage<VideoFileDTO> selectVideosByDatasetId(
            Page<ImageFileDTO> page,
            @Param("datasetId") Long datasetId,
            @Param("status") Integer status,
            @Param("name") String name,
            @Param("createTimeStart") String createTimeStart,
            @Param("createTimeEnd") String createTimeEnd,
            @Param("order") String order
    );
    //通过数据集id查询其中对应的视频数量，包括视频总数，已标注数量和未标注数量
    @Select("SELECT " +
            "  (SELECT COUNT(*) FROM data_file WHERE dataset_id = #{datasetId} AND file_type = 1 AND deleted = 0) AS totalVideos," +
            "  (SELECT COUNT(*) FROM data_file WHERE dataset_id = #{datasetId} AND file_type = 1 AND status = 101 AND deleted = 0) AS unExtractedVideos," +
            "  (SELECT COUNT(*) FROM data_file WHERE dataset_id = #{datasetId} AND file_type = 1 AND status = 103 AND deleted = 0) AS extractedVideos")
    @Results({
            @Result(column = "totalVideos", property = "totalVideos"),
            @Result(column = "unExtractedVideos", property = "unExtractedVideos"),
            @Result(column = "extractedVideos", property = "extractedVideos")
    })
    VideoStatisticsDTO getVideoStatistics(@Param("datasetId") Long datasetId);


    @Select({
            "<script>",
            "SELECT id, name, status, create_time, update_time, url FROM data_file WHERE dataset_id = #{datasetId} AND file_type = 1 AND deleted = 0",
            "ORDER BY id",
            "</script>"
    })
    List<VideoFileDTO> selectAllVideosByDatasetId(
            @Param("datasetId") Long datasetId
    );
}
