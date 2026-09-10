package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.entity.DatasetVersion;

import java.util.List;
import java.util.Map;

/**
 * @description 数据集
 * @date 2020-05-14
 */
@DataPermission(ignoresMethod = {"insert", "selectById", "getCountByDatasetVersionId", "findDatasetVersion", "selectList", "selectOne", "isVersionPublic", "getByDatasetIdAndVersionName", "getPublicVersionCountsByDatasetIds", "getPublishedDatasetVersionCountByDatasetId", "updateById", "selectDatasetVersionWithUserName", "findByIds", "getById", "getMaxVersionName", "getIdListByDatasetId","countPublicVersionsByDatasetIds"})


public interface DatasetVersionMapper extends BaseMapper<DatasetVersion> {

    /**
     * 查询某个数据集的某个版本是否存在
     *
     * @param datasetId   数据集ID
     * @param versionName 数据集版本
     * @return List<DatasetVersion> 数据集的版本信息
     */
    @Select("select * from data_dataset_version where dataset_id = #{datasetId} and version_name = #{versionName}")
    List<DatasetVersion> findDatasetVersion(@Param("datasetId") Long datasetId, @Param("versionName") String versionName);


    /**
     * 获取指定数据集当前使用最大版本号
     *
     * @param datasetId 数据集ID
     * @return String       指定数据集当前使用最大版本号
     */
    @Select("select max(version_name) from data_dataset_version where dataset_id = #{datasetId} and version_name like 'V%'")
    String getMaxVersionName(@Param("datasetId") Long datasetId);

    /**
     * 获取指定数据集当前使用最大切分版本号
     *
     * @param datasetId 数据集ID
     * @return String       指定数据集当前使用最大版本号
     */
    @Select("select max(version_name) from data_dataset_version where dataset_id = #{datasetId} and version_name like 'S%'")
    String getMaxSplitVersionName(Long datasetId);

    /**
     * 获取当前数据集版本的url
     *
     * @param datasetId   数据集ID
     * @param versionName 数据集版本
     * @return List<String>    数据集版本的url
     */
    @Select("SELECT version_url FROM data_dataset_version  WHERE dataset_id = #{datasetId}  and version_name = #{versionName}")
    List<String> selectVersionUrl(@Param("datasetId") Long datasetId, @Param("versionName") String versionName);

    /**
     * 根据数据集ID删除数据信息
     *
     * @param datasetId 数据集ID
     */
    @Delete("delete from data_dataset_version where dataset_id = #{datasetId}")
    void deleteByDatasetId(@Param("datasetId") Long datasetId);


    /**
     * 根据数据集ID查询版本名称列表
     *
     * @param datasetId 数据集ID
     * @return 版本名称列表
     */
    @Select("SELECT version_name FROM data_dataset_version  WHERE dataset_id = #{datasetId}")
    List<String> getDatasetVersionNameListByDatasetId(@Param("datasetId") Long datasetId);

    /**
     * 查询当前版本文件数量
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名称
     * @return Long             版本文件数量
     */
    @Select("select count(1) from data_dataset_version_file where dataset_id = #{datasetId} and version_name = #{versionName}")
    Integer getCountByDatasetVersionId(@Param("datasetId") Long datasetId, @Param("versionName") String versionName);

    /**
     * 根据id查询数据集版本
     *
     * @param datasetVersionId
     * @return
     */
    @Select("select * from data_dataset_version where id = #{datasetVersionId}")
    DatasetVersion getById(Long datasetVersionId);

    /**
     * 根据id list 查询数据集版本 list
     *
     * @param ids
     * @return
     */
    @Select("<script>" +
            "select * from data_dataset_version where id in("
            + "<foreach collection='ids' separator=',' item='id'>"
            + "#{id} "
            + "</foreach> "
            + ")</script>")
    List<DatasetVersion> findByIds(@Param("ids") List<Long> ids);

    @Select("select is_public from data_dataset_version where dataset_id = #{datasetId} and version_name = #{versionName}")
    short isVersionPublic(Long datasetId, String versionName);

    @Update("update data_dataset_version set is_public = 1 where dataset_id = #{datasetId} and version_name = #{versionName}")
    int publishDatasetVersion(Long datasetId, String versionName);

    @Select("select * from data_dataset_version where dataset_id = #{datasetId} and version_name = #{versionName}")
    DatasetVersion getByDatasetIdAndVersionName(Long datasetId, String versionName);

    @Select("select count(1) from data_dataset_version where dataset_id = #{datasetId} and is_public = 1")
    Integer getPublishedDatasetVersionCountByDatasetId(Long datasetId);

    @Select("<script>" +
            "SELECT " +
            "  dv.*, " +
            "  u.username AS createUserName " +
            "FROM " +
            "  data_dataset_version dv " +
            "  LEFT JOIN user u ON dv.create_user_id = u.id " +
            "WHERE " +
            "<if test='ew.sqlSegment != null and ew.sqlSegment != \"\"'>" +
            "  ${ew.sqlSegment} " +
            "</if>" +
            "</script>")
    List<DatasetVersion> selectDatasetVersionWithUserName(@Param("ew") LambdaQueryWrapper<DatasetVersion> lqw);
    @Select("<script>" +
            "SELECT COUNT(*) FROM data_dataset_version " +
            "WHERE dataset_id IN " +
            "<foreach item='id' collection='datasetIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "AND deleted = 0" +
            "</script>")
    Integer countVersionsByDatasetIds(@Param("datasetIds") List<Long> datasetIds);

    /**
     * 统计数据集组中公开版本的总数
     * @param datasetIds 数据集ID列表
     * @return 公开版本总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM data_dataset_version " +
            "WHERE dataset_id IN " +
            "<foreach collection='datasetIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND is_public = 1 " +
            " AND deleted = 0" +
            "</script>")
    Integer countPublicVersionsByDatasetIds(@Param("datasetIds") List<Long> datasetIds);

    @Select("SELECT id FROM data_dataset_version WHERE dataset_id = #{datasetId} AND deleted = false")
    List<Long> getIdListByDatasetId(Long datasetId);

    /**
     * 根据数据集id列表获取公开版本数量
     *
     * @param datasetIds 数据集id列表
     * @return 数据集id和公开版本数量的映射
     */
    @Select("<script>" +
            "SELECT dataset_id, COUNT(*) as public_count FROM data_dataset_version " +
            "WHERE dataset_id IN " +
            "<foreach collection='datasetIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND is_public = 1 " +
            " AND deleted = 0 " +
            "GROUP BY dataset_id" +
            "</script>")
    @MapKey("dataset_id")
    Map<Long, Map<String, Object>> getPublicVersionCountsByDatasetIds(@Param("datasetIds") List<Long> datasetIds);
}
