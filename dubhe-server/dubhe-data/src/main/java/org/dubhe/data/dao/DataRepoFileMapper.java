package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dubhe.data.domain.dto.FileTypeCountDTO;
import org.dubhe.data.domain.entity.DataRepoFile;

import java.util.List;

public interface DataRepoFileMapper extends BaseMapper<DataRepoFile> {
    @Select("SELECT " +
            "SUM(CASE WHEN file_type = 0 THEN 1 ELSE 0 END) AS imageCount, " +
            "SUM(CASE WHEN file_type = 1 THEN 1 ELSE 0 END) AS videoCount, " +
            "SUM(CASE WHEN file_type = 2 THEN 1 ELSE 0 END) AS otherCount " +
            "FROM data_repo_file " +
            "WHERE dataset_id = #{datasetId} AND deleted = 0")
    FileTypeCountDTO countFileTypesByDatasetId(@Param("datasetId") Long datasetId);

    @Select("SELECT " +
            "SUM(CASE WHEN file_type = 0 THEN 1 ELSE 0 END) AS imageCount, " +
            "SUM(CASE WHEN file_type = 1 THEN 1 ELSE 0 END) AS videoCount, " +
            "SUM(CASE WHEN file_type = 2 THEN 1 ELSE 0 END) AS otherCount " +
            "FROM data_repo_file " +
            "WHERE deleted = 0")
    FileTypeCountDTO countAllDatasetFileNumsByType();

    @Select("SELECT * FROM data_repo_file WHERE dataset_id = #{datasetId} AND file_type = #{fileType} AND deleted = 0")
    List<DataRepoFile> selectFilesByDatasetIdAndFileType(@Param("datasetId")Long datasetId, @Param(("fileType") )Integer fileType);
}
