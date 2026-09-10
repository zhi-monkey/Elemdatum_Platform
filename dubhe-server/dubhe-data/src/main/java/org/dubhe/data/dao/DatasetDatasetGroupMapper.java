package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.entity.DatasetDatasetGroup;

import java.util.List;
import java.util.Map;

/**
 * @author mingming
 * @date 2025/09/05
 */
@DataPermission(ignoresMethod = {"insert", "selectById", "queryTotal", "selectPage", "selectOne", "selectList", "getDatasetGroupStat", "getGroupDatasetIdsByUserId", "getGroupAllDatasetIds",  "getDatasetsByDatasetGroupId", "getDatasetsByDatasetGroupIdWithCreateUserId", "countGroupDatasetCount", "getGroupLatestDatasetIds", "countLabelsByDatasetGroupId" ,"unbind", "selectCount", "getGroupLatestDatasetIds", "getGroupIdsByDatasetId", "countLabelsByDatasetIds", "getDatasetGroupIdsByDatasetId", "getDatasetsByDatasetGroupIdAndAnnotateType", "getPublicDatasetsByDatasetGroupIdAndAnnotateType"})
@Mapper
public interface DatasetDatasetGroupMapper extends BaseMapper<DatasetDatasetGroup> {

    /**
     * 获取数据集组的标签，版本总数的方法
     *
     * @param datasetGroupId 数据集组id
     * @param userIdParam 用户id参数
     * @return 统计信息Map
     */
    @Select("SELECT " +
            "COUNT(DISTINCT dv.id) as version_count, " +
            "COUNT(DISTINCT LOWER(dl.name)) as label_count " +
            "FROM dataset_dataset_group ddg " +
            "LEFT JOIN data_dataset_version dv ON ddg.dataset_id = dv.dataset_id AND dv.deleted = 0 " +
            "AND (#{userIdParam} IS NULL OR dv.create_user_id = #{userIdParam}) " +  // version表权限
            "LEFT JOIN data_dataset_label ddl ON ddg.dataset_id = ddl.dataset_id AND ddl.deleted = 0 " +
            "LEFT JOIN data_label dl ON ddl.label_id = dl.id AND dl.deleted = 0 " +
            "AND (#{userIdParam} IS NULL OR dl.create_user_id = #{userIdParam}) " +  // label表权限
            "WHERE ddg.dataset_group_id = #{datasetGroupId}")
    Map<String, Object> getDatasetGroupStat(@Param("datasetGroupId") Long datasetGroupId,
                                             @Param("userIdParam") Long userIdParam);

    /**
     * 根据数据集组id获取数据集id列表
     *
     * @param datasetGroupId 数据集组id
     * @return 数据集id列表
     */
    @Select("select dataset_id from dataset_dataset_group where dataset_group_id = #{datasetGroupId}")
    List<Long> getDatasetsByDatasetGroupId(Long datasetGroupId);

    /**
     * 按顺序获取数据集id列表
     *
     * @param datasetGroupId 数据集组id
     * @return 数据集组数据集id列表
     */
    @Select("select dataset_id from dataset_dataset_group where dataset_group_id = #{datasetGroupId} order by id desc")
    List<Long> getGroupAllDatasetIds(Long datasetGroupId);

    /**
     * 根据数据集组ID和创建用户ID，获取该用户在该组中创建的数据集ID列表
     * 先筛选关联表获取指定组的关联记录，再内连接数据集表过滤创建者
     * 按关联记录ID倒序排列
     *
     * @param datasetGroupId 数据集组ID
     * @param createUserId 创建用户ID
     * @return 指定用户在该组中创建的数据集ID列表
     */
    @Select("SELECT ddg.dataset_id " +
            "FROM ( " +
            "    SELECT dataset_id, id " +
            "    FROM dataset_dataset_group " +
            "    WHERE dataset_group_id = #{datasetGroupId} " +
            "    ORDER BY id DESC " +
            ") ddg " +
            "INNER JOIN data_dataset d ON ddg.dataset_id = d.id " +
            "WHERE d.create_user_id = #{createUserId} ")
    List<Long> getGroupDatasetIdsByUserId(@Param("datasetGroupId") Long datasetGroupId,
                                          @Param("createUserId") Long createUserId);

    /**
     * 根据数据集组id获取数据集id列表
     *
     * @param datasetGroupId 数据集组id
     * @param createUserId   创建人id
     * @return 数据集id列表
     */
    @Select("select dataset_id from dataset_dataset_group where dataset_group_id = #{datasetGroupId} and create_user_id = #{createUserId}")
    List<Long> getDatasetsByDatasetGroupIdWithCreateUserId(Long datasetGroupId, Long createUserId);


    /**
     * 获取数据集组数据集数量
     *
     * @param id 数据集组id
     * @return 数据集组数据集数量
     */
    @Select("select count(dataset_id) from dataset_dataset_group where dataset_group_id = #{id}")
    Integer countGroupDatasetCount(Long id);

    /**
     * 获取数据集组最新三条数据集id列表
     *
     * @param datasetGroupId 数据集组id
     * @return 数据集组最新数据集id列表
     */
    @Select("select dataset_id from dataset_dataset_group where dataset_group_id = #{datasetGroupId} order by id desc limit 1")
    List<Long> getGroupLatestDatasetIds(Long datasetGroupId);

    @Select("SELECT COUNT(DISTINCT LOWER(dl.name)) FROM dataset_dataset_group ddg JOIN data_dataset_label ddl ON ddg.dataset_id = ddl.dataset_id JOIN data_label dl ON ddl.label_id = dl.id WHERE ddg.dataset_group_id = #{datasetGroupId} AND ddl.deleted = 0 AND dl.deleted = 0")
    Integer countLabelsByDatasetGroupId(@Param("datasetGroupId") Long datasetGroupId);


    @Select("SELECT DISTINCT dataset_group_id FROM dataset_dataset_group WHERE dataset_id = #{datasetId}")
    List<Long> getGroupIdsByDatasetId(Long datasetId);

    @Delete({
            "<script>",
            "  DELETE FROM dataset_dataset_group",
            "  WHERE dataset_id IN",
            "  <foreach item='id' collection='datasetIds' open='(' separator=',' close=')'>",
            "    #{id}",
            "  </foreach>",
            "</script>"
    })
    void unbind(@Param("datasetIds") List<Long> datasetIds);

    /**
     * 根据数据集IDs统计标签种类数
     */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT LOWER(l.name)) " +
            "FROM data_label l " +
            "INNER JOIN data_dataset_label ddl ON l.id = ddl.label_id " +
            "WHERE ddl.dataset_id IN " +
            "<foreach item='id' collection='list' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach> " +
            "AND l.deleted = 0 " +
            "AND ddl.deleted = 0" +
            "</script>")
    Integer countLabelsByDatasetIds(@Param("list") List<Long> datasetIds);

    @Select("SELECT dataset_group_id FROM dataset_dataset_group WHERE dataset_id = #{datasetId}")
    List<Long> getDatasetGroupIdsByDatasetId(@Param("datasetId") Long datasetId);

    /**
     * 根据数据集组ID和标注类型获取数据集ID列表
     *
     * @param datasetGroupId 数据集组ID
     * @param annotateType   标注类型（102:目标检测, 103:语义分割）
     * @return 数据集ID列表
     */
    @Select("SELECT ddg.dataset_id FROM dataset_dataset_group ddg " +
            "INNER JOIN data_dataset dd ON ddg.dataset_id = dd.id " +
            "WHERE ddg.dataset_group_id = #{datasetGroupId} " +
            "AND dd.annotate_type = #{annotateType} " +
            "AND dd.deleted = 0")
    List<Long> getDatasetsByDatasetGroupIdAndAnnotateType(@Param("datasetGroupId") Long datasetGroupId, 
                                                           @Param("annotateType") Integer annotateType);

    /**
     * 根据数据集组ID和标注类型获取公开数据集ID列表
     *
     * @param datasetGroupId 数据集组ID
     * @param annotateType   标注类型（102:目标检测, 103:语义分割）
     * @return 公开数据集ID列表
     */
    @Select("SELECT ddg.dataset_id FROM dataset_dataset_group ddg " +
            "INNER JOIN data_dataset dd ON ddg.dataset_id = dd.id " +
            "WHERE ddg.dataset_group_id = #{datasetGroupId} " +
            "AND dd.annotate_type = #{annotateType} " +
            "AND dd.is_public = 1 " +
            "AND dd.deleted = 0")
    List<Long> getPublicDatasetsByDatasetGroupIdAndAnnotateType(@Param("datasetGroupId") Long datasetGroupId, 
                                                                 @Param("annotateType") Integer annotateType);


}
