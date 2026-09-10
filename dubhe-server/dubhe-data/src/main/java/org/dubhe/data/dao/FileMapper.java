

package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.bo.FileAnnotationBO;
import org.dubhe.data.domain.bo.TextAnnotationBO;
import org.dubhe.data.domain.dto.EsTransportDTO;
import org.dubhe.data.domain.dto.FileCreateDTO;
import org.dubhe.data.domain.entity.File;
import org.dubhe.data.domain.entity.FileUrlIdMapping;

import java.util.Collection;
import java.util.List;

/**
 * @description 文件信息 Mapper 接口
 * @date 2020-04-10
 */
@DataPermission(ignoresMethod = {"insert", "getOneById", "selectFile","selectList","selectOne", "getVersionImgCount","selectUrls","selectUrlsFromFileStatusNormalOrDeleted"})
public interface FileMapper extends BaseMapper<File> {


    /**
     * 根据文件ID获取文件
     *
     * @param fileId    文件ID
     * @param datasetId 数据集ID
     * @return File     文件对象
     */
    @Select("select * from data_file where id = #{fileId} and dataset_id = #{datasetId}")
    File getOneById(@Param("fileId") Long fileId, @Param("datasetId") long datasetId);

    /**
     * 批量保存
     *
     * @param files         上传文件列表
     * @param userId        用户Id
     * @param datasetUserId 数据集用户id
     */
    void saveList(@Param("files") List<File> files, @Param("userId") Long userId, @Param("datasetUserId") Long datasetUserId);

    /**
     * 查询图片宽高
     *
     * @param name              数据集的版本文件名称
     * @param datasetId         数据集ID
     * @return FileCreateDTO    文件详情
     */
    @Select("select width,height from data_file where name = #{name} and dataset_id = #{datasetId}")
    FileCreateDTO selectWidthAndHeight(@Param("name") String name, @Param("datasetId") Long datasetId);

    /**
     * 获取文件详情
     *
     * @param fileId 文件ID
     * @param datasetId 数据集ID
     * @return File 文件详情
     */
    @Select("select * from data_file where id = #{fileId} and dataset_id=#{datasetId} and deleted=0")
    File selectFile(@Param("fileId") Long fileId, @Param("datasetId") Long datasetId);

    /**
     * 分页获取数据集文件
     *
     * @param datasetId          数据集ID
     * @param currentVersionName 数据集版本名称
     * @param offset             偏移量
     * @param batchSize          批长度
     * @return List 文件列表
     */
    @Select("<script>" +
            "select distinct df.* from data_dataset_version_file ddvf left join data_file df on ddvf.file_id = df.id where ddvf.dataset_id = #{datasetId} " +
            " and df.dataset_id = #{datasetId} " +
            "<if test='currentVersionName != null'> " +
            "and ddvf.version_name =  #{currentVersionName} " +
            "</if>" +
            "and ddvf.annotation_status in " +
            "<foreach item='item' collection='status' separator=',' open='(' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "limit #{offset}, #{batchSize} " +
            "</script>")
    List<File> selectListOne(@Param("datasetId") Long datasetId, @Param("currentVersionName") String currentVersionName, @Param("offset") int offset, @Param("batchSize") int batchSize, @Param("status") Collection<Integer> status);

    /**
     * 更新文件状态
     *
     * @param datasetId  数据集ID
     * @param id         文件ID
     * @param status     文件状态
     */
    @Update("update data_file set status = #{status} where dataset_id = #{datasetId} and id = #{id}")
    void updateFileStatus(@Param("datasetId") Long datasetId, @Param("id") Long id, @Param("status") Integer status);

    /**
     * 根据数据集ID删除文件数据
     *
     * @param datasetId     数据集ID
     * @param limitNumber   删除数量
     * @return int 成功删除条数
     */
    @Delete("delete from data_file where dataset_id = #{datasetId} limit #{limitNumber} ")
    int deleteByDatasetId(@Param("datasetId") Long datasetId, @Param("limitNumber") int limitNumber);

    /**
     * 根据版本和数据集ID获取文件url
     *
     * @param datasetId     数据集ID
     * @param versionName   版本名
     * @return List<String> url列表
     */
    @Select("select df.url from data_file df left join data_dataset_version_file ddvf on df.id = ddvf.file_id " +
            "where ddvf.dataset_id = #{datasetId} and df.dataset_id = #{datasetId} " +
            "and ddvf.version_name = #{versionName}")
    List<String> selectUrls(@Param("datasetId") Long datasetId, @Param("versionName") String versionName);


    @Select("select df.url from data_file df left join data_dataset_version_file ddvf on df.id = ddvf.file_id " +
            "where ddvf.dataset_id = #{datasetId} and df.dataset_id = #{datasetId} " +
            "and (ddvf.status = 1 or ddvf.status = 2) and ddvf.version_name = #{versionName}")
    List<String> selectUrlsFromFileStatusNormalOrDeleted(@Param("datasetId") Long datasetId, @Param("versionName") String versionName);

    /**
     * 根据version.changed获取文件name列表
     *
     * @param datasetId     数据集ID
     * @param changed       版本文件是否改动
     * @param versionName   版本名称
     * @return List<FileAnnotationBO>   名称列表
     */
    @Select("select df.id as fileId, df.name as fileName ,df.url as fileUrl ,df.width as fileWidth,df.height as fileHeight from data_file df left join data_dataset_version_file ddvf on df.id = ddvf.file_id " +
            "where ddvf.dataset_id = #{datasetId} and df.dataset_id = #{datasetId} " +
            "and ddvf.changed = #{changed} and ddvf.version_name = #{versionName}")
    List<FileAnnotationBO> selectFileAnnotations(@Param("datasetId") Long datasetId, @Param("changed") Integer changed,@Param("versionName")String versionName);

    /**
     * 获取当前版本下原图文件数量
     *
     * @param datasetId 数据集ID
     * @param versionName 版本名称
     * @return 数据集文件数量
     */
    int getOriginalFileCountOfDataset(@Param("datasetId") Long datasetId, @Param("versionName") String versionName);

    /**
     * 批量新增文件数据
     *
     * @param fileList 数据文件列表
     */
    void insertBatch(@Param("listDataFile")List<File> fileList);

    /**
     * 选择需要同步到ES的数据
     *
     * @param datasetId 数据集ID
     * @param fileIdsNotToEs 需要同步的文件ID
     * @return List<EsTransportDTO>  ES数据同步DTO
     */
    List<EsTransportDTO> selectTextDataNoTransport(@Param("datasetId") Long datasetId,@Param("fileIdsNotToEs")List<Long> fileIdsNotToEs,
                                                   @Param("ifImport") Boolean ifImport);

    /**
     * 更新同步es标志
     *
     * @param datasetId  数据集ID
     * @param fileIds    文件ID列表
     */
    void updateEsStatus(@Param("datasetId") Long datasetId,@Param("fileIds")List<Long> fileIds);

    /**
     * 置回es标志
     *
     * @param datasetId  数据集ID
     * @param fileId     文件ID
     */
    @Update("update data_file set es_transport = 0 where dataset_id = #{datasetId} and id = #{fileId}")
    void recoverEsStatus(@Param("datasetId") Long datasetId, @Param("fileId") Long fileId);

    /**
     * 根据版本和数据集ID获取文件url 宽高
     *
     * @param datasetId     数据集ID
     * @param versionName   版本名
     * @return List<FileAnnotationBO> url列表
     */
    @Select("select df.id as fileId,  df.name as fileName ,df.url as fileUrl ,df.width as fileWidth,df.height as fileHeight" +
            " from data_file df left join data_dataset_version_file ddvf on df.id = ddvf.file_id " +
            "where ddvf.dataset_id = #{datasetId} and df.dataset_id = #{datasetId} " +
            "and ddvf.version_name = #{versionName}")
    List<FileAnnotationBO> listByDatasetIdAndVersionName(@Param("datasetId") Long datasetId, @Param("versionName") String versionName);

    List<TextAnnotationBO> selectTextAnnotation(@Param("datasetId") Long datasetId, @Param("fileIdsNotToEs")List<Long> fileIdsNotToEs);


    // 删除视频文件接口
    @Update("<script>" +
            "UPDATE data_file SET deleted = 1 WHERE " +
            "id IN " +
            "<foreach item='fileId' collection='fileIds' open='(' separator=',' close=')'>" +
            "#{fileId}" +
            "</foreach>" +
            "</script>")
    void deleteFiles(@Param("fileIds") Long[] fileIds);


    @Select("select count(1) from data_dataset_version_file where dataset_id = #{datasetId} and version_name = #{versionName} ")
    Integer getVersionImgCount(Long datasetId, String versionName);

    /**
     * 批量根据文件名获取宽高信息
     *
     * @param names     文件名列表
     * @param datasetId 数据集ID
     * @return 文件信息列表
     */
    @Select("<script>" +
            "select name, width, height from data_file " +
            "where dataset_id = #{datasetId} " +
            "and name in " +
            "<foreach collection='names' item='name' open='(' separator=',' close=')'>" +
            "#{name}" +
            "</foreach>" +
            "</script>")
    List<FileCreateDTO> selectWidthAndHeightBatch(@Param("names") List<String> names, @Param("datasetId") Long datasetId);

    /**
     * 根据URL批量获取文件ID和URL的映射
     */
    @Select("<script>" +
            "SELECT url, id FROM data_file " +
            "WHERE dataset_id = #{datasetId} AND url IN " +
            "<foreach collection='urls' item='url' open='(' separator=',' close=')'>" +
            "#{url}" +
            "</foreach>" +
            "</script>")
    List<FileUrlIdMapping> getFileIdsByUrls(@Param("datasetId") Long datasetId,
                                            @Param("urls") List<String> urls);

    /**
     * 根据ID批量获取文件信息
     */
    @Select("<script>" +
            "SELECT * FROM data_file " +
            "WHERE dataset_id = #{datasetId} AND id IN " +
            "<foreach collection='fileIds' item='fileId' open='(' separator=',' close=')'>" +
            "#{fileId}" +
            "</foreach>" +
            "</script>")
    List<File> getFilesByIds(@Param("datasetId") Long datasetId,
                             @Param("fileIds") List<Long> fileIds);

    /**
     * 查询当前文件之后最近的未标注图片的ID
     * 
     * @param datasetId       数据集ID
     * @param currentFileId   当前文件ID
     * @param versionName     版本名称（可选）
     * @param labelIds        标签ID列表（可选）
     * @return 最近未标注图片的文件ID，如果没有则返回null
     */
    @Select("<script>" +
            "SELECT df.id " +
            "FROM data_dataset_version_file ddvf " +
            "LEFT JOIN data_file df ON ddvf.file_id = df.id " +
            "WHERE ddvf.dataset_id = #{datasetId} " +
            "AND df.dataset_id = #{datasetId} " +
            "AND ddvf.annotation_status = 101 " +
            "AND df.id &gt;= #{currentFileId} " +
            "AND ddvf.status IN (0, 2) " +
            "AND df.deleted = 0 " +
            "<if test='versionName != null and versionName != \"\"'>" +
            "AND ddvf.version_name = #{versionName} " +
            "</if>" +
            "<if test='labelIds != null and labelIds.size() > 0'>" +
            "AND EXISTS (" +
            "  SELECT 1 FROM data_file_annotation dfa " +
            "  WHERE dfa.file_id = df.id " +
            "  AND dfa.label_id IN " +
            "  <foreach collection='labelIds' item='labelId' open='(' separator=',' close=')'>" +
            "    #{labelId}" +
            "  </foreach>" +
            ") " +
            "</if>" +
            "ORDER BY df.id ASC " +
            "LIMIT 1" +
            "</script>")
    Long findNearestUnannotatedFileId(@Param("datasetId") Long datasetId,
                                      @Param("currentFileId") Long currentFileId,
                                      @Param("versionName") String versionName,
                                      @Param("labelIds") List<Long> labelIds);
    
    /**
     * 根据 datasetId 和 fileId 获取 data_dataset_version_file 表的 id
     * 
     * @param datasetId       数据集ID
     * @param fileId          文件ID
     * @param versionName     版本名称（可选）
     * @return ddvf 表的 id，如果不存在返回 null
     */
    @Select("<script>" +
            "SELECT ddvf.id " +
            "FROM data_dataset_version_file ddvf " +
            "INNER JOIN data_file df ON ddvf.file_id = df.id " +
            "WHERE ddvf.dataset_id = #{datasetId} " +
            "AND df.id = #{fileId} " +
            "AND df.deleted = 0 " +
            "AND ddvf.status IN (0, 2) " +
            "<if test='versionName != null and versionName != \"\"'>" +
            "AND ddvf.version_name = #{versionName} " +
            "</if>" +
            "LIMIT 1" +
            "</script>")
    Long getDatasetVersionFileId(@Param("datasetId") Long datasetId,
                                 @Param("fileId") Long fileId,
                                 @Param("versionName") String versionName);
    
    /**
     * 计算两个 ddvf.id 之间的记录数
     * 
     * @param datasetId       数据集ID
     * @param startDdvfId     起始 ddvf.id
     * @param endDdvfId       结束 ddvf.id
     * @param versionName     版本名称（可选）
     * @return 两个 ddvf.id 之间的记录数（不包含 startDdvfId，包含 endDdvfId）
     */
    @Select("<script>" +
            "SELECT COUNT(1) " +
            "FROM data_dataset_version_file ddvf " +
            "INNER JOIN data_file df ON ddvf.file_id = df.id " +
            "WHERE ddvf.dataset_id = #{datasetId} " +
            "AND df.deleted = 0 " +
            "AND ddvf.status IN (0, 2) " +
            "AND ddvf.id &gt; #{startDdvfId} " +
            "AND ddvf.id &lt;= #{endDdvfId} " +
            "<if test='versionName != null and versionName != \"\"'>" +
            "AND ddvf.version_name = #{versionName} " +
            "</if>" +
            "</script>")
    Integer calculateDistanceBetweenDdvfIds(@Param("datasetId") Long datasetId,
                                           @Param("startDdvfId") Long startDdvfId,
                                           @Param("endDdvfId") Long endDdvfId,
                                           @Param("versionName") String versionName);
    
    /**
     * 计算两个文件ID之间的距离（记录数）
     * 通过先找到对应的 ddvf.id，再计算 ddvf.id 之间的记录数
     * 
     * @param datasetId       数据集ID
     * @param startFileId     起始文件ID
     * @param endFileId       结束文件ID
     * @param versionName     版本名称（可选）
     * @return 两个文件之间的记录数
     */
    default Integer calculateDistanceBetweenFiles(Long datasetId, Long startFileId, Long endFileId, String versionName) {
        Long startDdvfId = getDatasetVersionFileId(datasetId, startFileId, versionName);
        Long endDdvfId = getDatasetVersionFileId(datasetId, endFileId, versionName);
        if (startDdvfId == null || endDdvfId == null) {
            return null;
        }
        return calculateDistanceBetweenDdvfIds(datasetId, startDdvfId, endDdvfId, versionName);
    }
}
