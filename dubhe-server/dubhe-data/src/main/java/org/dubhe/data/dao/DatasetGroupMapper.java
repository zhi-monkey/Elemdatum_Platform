package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.entity.DatasetGroup;

import java.util.List;

/**
 * @description 数据集分组Mapper
 * @author mingming
 * @date 2025/09/05
 */
@Mapper
@DataPermission(ignoresMethod = {"searchPrivateAndPublicDatasetGroupByName","getPrivateAndPublicDatasetGroupByPage","getAllDatasetGroupByPage", "selectPage", "selectCount", "deleteById", "selectOne", "publishDatasetGroups", "insert", "selectList", "getPersonalDatasetGroupByPage", "searchAllDatasetGroupByName", "searchPersonalDatasetGroupByName", "existCountByName", "selectById", "updateById", "getAllDatasetGroupPageWithFilter", "getPublicDatasetGroupPageWithFilter"})
public interface DatasetGroupMapper extends BaseMapper<DatasetGroup> {
    /**
     * 获取所有数据集分组
     *
     * @return 数据集分组列表
     */
    @Select("select * from dataset_group")
    List<DatasetGroup> getAllDatasetGroup();

    /**
     * 获取个人数据集分组
     *
     * @param userId 用户ID
     * @return 数据集分组列表
     */
    @Select("select * from dataset_group where create_user_id = #{userId}")
    List<DatasetGroup> getPersonalDatasetGroup(Long userId);


    /**
     * 分页获取所有数据集分组（管理员）
     *
     * @param page 分页参数
     * @return 分页结果
     */
    @Select("select * from dataset_group order by creation_time desc")
    IPage<DatasetGroup> getAllDatasetGroupByPage(Page<DatasetGroup> page);

    /**
     * 分页获取个人数据集分组
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @return 分页结果
     */
    @Select("select * from dataset_group where create_user_id = #{userId} order by creation_time desc")
    IPage<DatasetGroup> getPersonalDatasetGroupByPage(Page<DatasetGroup> page, @Param("userId") Long userId);


    /**
     * 分页获取个人数据集分组
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @return 分页结果
     */
    @Select("SELECT * FROM dataset_group " +
            "WHERE create_user_id = #{userId} OR is_public = TRUE " +
            "ORDER BY creation_time DESC")
    IPage<DatasetGroup> getPrivateAndPublicDatasetGroupByPage(Page<DatasetGroup> page,
                                                              @Param("userId") Long userId);




    /**
     * 按名称搜索所有数据集分组（管理员）
     *
     * @param page 分页参数
     * @param name 搜索关键词
     * @return 分页结果
     */
    @Select("<script>" +
            "select * from dataset_group " +
            "<where>" +
            "<if test='name != null and name != \"\"'>" +
            "and (name like concat('%', #{name}, '%') or description like concat('%', #{name}, '%'))" +
            "</if>" +
            "</where>" +
            "order by creation_time desc" +
            "</script>")
    IPage<DatasetGroup> searchAllDatasetGroupByName(Page<DatasetGroup> page, @Param("name") String name);

    /**
     * 按名称搜索个人数据集分组
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param name 搜索关键词
     * @return 分页结果
     */
    @Select("<script>" +
            "select * from dataset_group " +
            "<where>" +
            "create_user_id = #{userId}" +
            "<if test='name != null and name != \"\"'>" +
            "and (name like concat('%', #{name}, '%') or description like concat('%', #{name}, '%'))" +
            "</if>" +
            "</where>" +
            "order by creation_time desc" +
            "</script>")
    IPage<DatasetGroup> searchPersonalDatasetGroupByName(Page<DatasetGroup> page, @Param("userId") Long userId, @Param("name") String name);

    /**
     * 按名称搜索个人数据集分组（包括公开的）
     *
     * @param page 分页参数
     * @param userId 用户ID
     * @param name 搜索关键词
     * @return 分页结果
     */
    @Select("<script>" +
            "select * from dataset_group " +
            "<where>" +
            "(create_user_id = #{userId} OR is_public = 1)" +
            "<if test='name != null and name != \"\"'>" +
            "AND (name like concat('%', #{name}, '%') or description like concat('%', #{name}, '%'))" +
            "</if>" +
            "</where>" +
            "order by creation_time desc" +
            "</script>")
    IPage<DatasetGroup> searchPrivateAndPublicDatasetGroupByName(Page<DatasetGroup> page, @Param("userId") Long userId, @Param("name") String name);



    @Update("<script>" +
            "UPDATE dataset_group " +
            "SET is_public = 1 " +
            "WHERE id IN " +
            "  <foreach item='id' collection='datasetGroupIds' open='(' separator=',' close=')'>" +
            "    #{id}" +
            "  </foreach>" +
            "</script>")
    boolean publishDatasetGroups(@Param("datasetGroupIds") List<Long> datasetGroupIds);

    @Select("SELECT count(1) FROM dataset_group WHERE name = #{datasetGroupName}")
    Integer existCountByName(String datasetGroupName);

    /**
     * 分页获取数据集组（支持 isGuided 筛选）
     * @param page 分页参数
     * @param userId 用户ID（可为null，表示管理员）
     * @param name 搜索名称（可为null）
     * @param isGuided 是否引导式（可为null，表示不筛选）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT DISTINCT dg.* FROM dataset_group dg " +
            "<where>" +
            "<if test='userId != null'>" +
            "dg.create_user_id = #{userId} " +
            "</if>" +
            "<if test='name != null and name != \"\"'>" +
            "AND dg.name LIKE CONCAT('%', #{name}, '%') " +
            "</if>" +
            "<if test='isGuided != null and isGuided == true'>" +
            "AND EXISTS (" +
            "  SELECT 1 FROM dataset_dataset_group ddg " +
            "  INNER JOIN data_dataset d ON ddg.dataset_id = d.id AND d.deleted = 0 " +
            "  WHERE ddg.dataset_group_id = dg.id AND d.is_guided = 1" +
            ") " +
            "</if>" +
            "<if test='isGuided != null and isGuided == false'>" +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM dataset_dataset_group ddg " +
            "  INNER JOIN data_dataset d ON ddg.dataset_id = d.id AND d.deleted = 0 " +
            "  WHERE ddg.dataset_group_id = dg.id AND d.is_guided = 1" +
            ") " +
            "</if>" +
            "</where>" +
            "ORDER BY dg.creation_time DESC" +
            "</script>")
    IPage<DatasetGroup> getAllDatasetGroupPageWithFilter(Page<DatasetGroup> page,
                                                          @Param("userId") Long userId,
                                                          @Param("name") String name,
                                                          @Param("isGuided") Boolean isGuided);

    /**
     * 分页获取公开数据集组（支持 isGuided 筛选）
     * @param page 分页参数
     * @param name 搜索名称（可为null）
     * @param isGuided 是否引导式（可为null，表示不筛选）
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT DISTINCT dg.* FROM dataset_group dg " +
            "<where>" +
            "dg.is_public = 1 " +
            "<if test='name != null and name != \"\"'>" +
            "AND dg.name LIKE CONCAT('%', #{name}, '%') " +
            "</if>" +
            "<if test='isGuided != null and isGuided == true'>" +
            "AND EXISTS (" +
            "  SELECT 1 FROM dataset_dataset_group ddg " +
            "  INNER JOIN data_dataset d ON ddg.dataset_id = d.id AND d.deleted = 0 AND d.is_guided = 1 " +
            "  INNER JOIN data_dataset_version dv ON dv.dataset_id = d.id AND dv.is_public = 1 " +
            "  WHERE ddg.dataset_group_id = dg.id" +
            ") " +
            "</if>" +
            "<if test='isGuided != null and isGuided == false'>" +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM dataset_dataset_group ddg " +
            "  INNER JOIN data_dataset d ON ddg.dataset_id = d.id AND d.deleted = 0 AND d.is_guided = 1 " +
            "  INNER JOIN data_dataset_version dv ON dv.dataset_id = d.id AND dv.is_public = 1 " +
            "  WHERE ddg.dataset_group_id = dg.id" +
            ") " +
            "</if>" +
            "</where>" +
            "ORDER BY dg.creation_time DESC" +
            "</script>")
    IPage<DatasetGroup> getPublicDatasetGroupPageWithFilter(Page<DatasetGroup> page,
                                                             @Param("name") String name,
                                                             @Param("isGuided") Boolean isGuided);
}
