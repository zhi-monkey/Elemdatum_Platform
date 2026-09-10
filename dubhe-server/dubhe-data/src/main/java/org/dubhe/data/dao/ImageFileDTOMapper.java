package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dubhe.data.domain.dto.DatasetVersionFileDTO;
import org.dubhe.data.domain.dto.ImageFileDTO;

import java.util.List;

@Mapper
public interface ImageFileDTOMapper extends BaseMapper<ImageFileDTO> {
    @Select({
            "<script>",
            "SELECT dvf.file_id as id, dvf.dataset_id, dvf.version_name, dvf.file_id, dvf.status, dvf.annotation_status, dvf.backup_status, dvf.changed, dvf.file_name, df.url FROM data_dataset_version_file dvf",
            "LEFT JOIN data_file df ON dvf.file_id = df.id",
            "WHERE dvf.dataset_id = #{datasetId}",
            "AND dvf.status in (0,2)", // 状态1是删除文件
            "<if test='versionName != null'> AND dvf.version_name = #{versionName}</if>",
            "<if test='status != null'> AND dvf.annotation_status = #{status}</if>",
            "<if test='fileName != null'> AND dvf.file_name LIKE CONCAT('%', #{fileName}, '%')</if>",
            "ORDER BY dvf.file_id ${order}",
            "</script>"
    })
    IPage<DatasetVersionFileDTO> selectDatasetVersionFilesByConditions(
            Page<DatasetVersionFileDTO> page,
            @Param("datasetId") Long datasetId,
            @Param("versionName") String versionName,
            @Param("status") Integer status,
            @Param("fileName") String fileName,
            @Param("order") String order
    );


    @Select({
            "<script>",
            "SELECT dvf.file_id as id, dvf.dataset_id, dvf.version_name, dvf.file_id, dvf.status, dvf.annotation_status, dvf.backup_status, dvf.changed, dvf.file_name, df.url FROM data_dataset_version_file dvf",
            "LEFT JOIN data_file df ON dvf.file_id = df.id",
            "WHERE dvf.dataset_id = #{datasetId}",
            "AND dvf.status in (0,2)", // 状态1是删除文件
            "<if test='versionName != null'> AND dvf.version_name = #{versionName}</if>",
            "ORDER BY dvf.file_id",
            "</script>"
    })
    List<DatasetVersionFileDTO> selectAllDatasetVersionFiles(
            @Param("datasetId") Long datasetId,
            @Param("versionName") String versionName
    );

}
