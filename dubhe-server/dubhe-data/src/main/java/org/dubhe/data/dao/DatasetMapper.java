package org.dubhe.data.dao;

import com.amazonaws.services.quicksight.model.DataSet;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dubhe.biz.base.annotation.DataPermission;
import org.dubhe.data.domain.dto.DatasetLabelInfoDTO;
import org.dubhe.data.domain.dto.DatasetNameTimeDTO;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.vo.DatasetInfoVO;
import org.dubhe.data.domain.vo.DatasetVersionQueryVO;
import org.dubhe.data.domain.vo.DatasetVersionVO;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @description 数据集管理 Mapper 接口
 * @date 2020-04-10
 */
@DataPermission(ignoresMethod = {"insert", "listPage", "selectById", "selectCountByPublic", "getDatasetNamesAndTimeList", "selectList", "dataVersionListVO", "updateModuleById", "getById", "updateStatus", "listPage", "selectLabelInfoByDatasetId", "findByName", "getAllPublishedDatasets", "findLabelsByDatasetId", "listPageWithUser","selectCount"})
public interface DatasetMapper extends BaseMapper<Dataset> {

    @Select("SELECT is_guided FROM data_dataset WHERE id = #{id}")
    Boolean isDatasetGuided(@Param("id") Long id);

    // 查找这条记录的数据集对应的所有标签
    @Select("SELECT d.label_id " +
            "FROM data_dataset dd " +
            "JOIN data_dataset_label d ON dd.id = d.dataset_id " +
            "WHERE dd.id = #{id}")
    List<Long> findLabelsByDatasetId(@Param("id") Long id);

//    /**
//     * 分页获取数据集
//     *
//     * @param page         分页插件
//     * @param queryWrapper 查询条件
//     * @return Page<Dataset>数据集列表
//     */
//    @Select("SELECT * FROM data_dataset ${ew.customSqlSegment}")
//    Page<Dataset> listPage(Page<Dataset> page, @Param("ew") Wrapper<Dataset> queryWrapper);

    // 先筛选数据集，再左连接用户信息，最后将结果字段映射成带有用户信息的完整数据
    @Select("<script>" +
            "SELECT * FROM (" +
            "   SELECT " +
            "   d.*, " +
            "   u.id AS 'createUser.id', " +
            "   u.username AS 'createUser.username', " +
            "   u.nick_name AS 'createUser.nickName' " +
            "   FROM data_dataset d " +
            "   LEFT JOIN user u ON d.create_user_id = u.id " +
            ") t ${ew.customSqlSegment}" +
            "</script>")
    Page<Dataset> listPage(Page<Dataset> page, @Param("ew") Wrapper<Dataset> queryWrapper);

    // 先筛选数据集，再左连接用户信息，最后将结果字段映射成带有用户信息的完整数据
    @Select("<script>" +
            "SELECT * FROM (" +
            "   SELECT " +
            "   d.*, " +
            "   u.id AS 'createUser.id', " +
            "   u.username AS 'createUser.username', " +
            "   u.nick_name AS 'createUser.nickName' " +
            "   FROM data_dataset d " +
            "   LEFT JOIN user u ON d.create_user_id = u.id " +
            ") t ${ew.customSqlSegment}" +
            "</script>")
    Page<Dataset> listPageWithUser(Page<Dataset> page, @Param("ew") Wrapper<Dataset> queryWrapper);

    @Select("SELECT * FROM data_dataset WHERE name = #{name} AND deleted = false")
    Dataset findByName(@Param("name") String name);


    /**
     * 修改数据集当前版本
     *
     * @param id          数据集ID
     * @param versionName 数据集版本名称
     */
    @Update("update data_dataset set current_version_name = #{versionName}  where id = #{id}")
    void updateVersionName(@Param("id") Long id, @Param("versionName") String versionName);

    /**
     * 更新数据集状态
     *
     * @param datasetId 数据集ID
     * @param status    数据集状态
     */
    @Update("update data_dataset set status = #{status} where id = #{datasetId}")
    void updateStatus(@Param("datasetId") Long datasetId, @Param("status") Integer status);


    /**
     * 获取指定类型数据集的数量
     *
     * @param type 数据集类型
     * @return int  公共数据集的数量
     */
    @Select("SELECT count(1) FROM data_dataset where type = #{type} and deleted = #{deleted}")
    int selectCountByPublic(@Param("type") Integer type, @Param("deleted") Integer deleted);


    /**
     * 根据标签组ID查询关联的数据集数量
     *
     * @param labelGroupId 标签组ID
     * @return int 数量
     */
    @Select("SELECT count(1) FROM data_dataset where label_group_id = #{labelGroupId}")
    int getCountByLabelGroupId(@Param("labelGroupId") Long labelGroupId);



    /**
     * 根据标签组ID和deleted字段查询数据集数量
     *
     * @param labelGroupId 标签组ID
     * @param deleted 是否已删除
     * @return 引用标签组的数据集数量
     */
    @Select("SELECT count(1) FROM data_dataset WHERE label_group_id = #{labelGroupId} AND deleted = #{deleted}")
    int getCountByLabelGroupIdAndDeleted(@Param("labelGroupId") Long labelGroupId, @Param("deleted") boolean deleted);
    /**
     * 数据集数据删除
     *
     * @param id         数据集id
     * @param deleteFlag 删除标识
     * @return int 数量
     */
    @Update("update data_dataset set deleted = #{deleteFlag} where id = #{id}")
    int updateStatusById(@Param("id") Long id, @Param("deleteFlag") boolean deleteFlag);

    /**
     * 根据数据集ID删除数据信息
     *
     * @param datasetId 数据集ID
     */
    @Delete("delete from data_dataset where  id = #{datasetId}")
    void deleteInfoById(@Param("datasetId") Long datasetId);

    List<DatasetVersionQueryVO> dataVersionListVO(@Param("annotateType") Integer annotateType,
                                                  @Param("module") Integer module, @Param("ids") List<Long> ids,
                                                  @Param("currentUserId") Long currentUserId);

    //根据数据集id查询对应的标签信息
    @Select("SELECT " +
            "ddl.label_id AS labelId, " +
            "dl.name AS labelName, " +
            "COUNT(dfa.id) AS annotationCount " +
            "FROM data_dataset_label ddl " +
            "LEFT JOIN data_file_annotation dfa " +
            "ON ddl.label_id = dfa.label_id AND ddl.dataset_id = dfa.dataset_id " +
            "INNER JOIN data_label dl " +
            "ON ddl.label_id = dl.id " +
            "WHERE ddl.dataset_id = #{datasetId} " +
            "AND ddl.deleted = 0 " +
            "AND dl.deleted = b'0' " +
            "AND (dfa.deleted = b'0' OR dfa.deleted IS NULL) " +
            "GROUP BY ddl.label_id, dl.name")
    List<DatasetLabelInfoDTO> selectLabelInfoByDatasetId(@Param("datasetId") Long datasetId);


    /**
     * 根据数据类型查询数据
     *
     * @param dataType
     * @return
     */
    @Delete("delete from data_dataset where  data_type = #{dataType}")
    List<Dataset> selectByDataType(@Param("dataType") Integer dataType);

    // 更新数据集多人标注状态
    @Update("update data_dataset set module = #{module} where id = #{id}")
    int updateModuleById(@Param("id") Long id, @Param("module") Integer module);

    List<DatasetInfoVO> selectDatasetsWithVersionFileCount();

    @Update("update data_dataset set is_public = 1 where id = #{datasetId}")
    int publishDataset(Long datasetId);

    @Select("SELECT * FROM data_dataset WHERE current_version_name IS NOT NULL AND deleted = 0 AND is_public = 1")
    List<Dataset> getAllPublishedDatasets();

    @Update("update data_dataset set is_publishing = #{isPublishing} where id = #{datasetId}")
    void updateIsPublishingById(Long datasetId, boolean isPublishing);

    @Select("SELECT is_guided FROM data_dataset WHERE id = #{datasetId} and deleted = 0")
    Boolean isGuided(Long datasetId);

    /**
     * 查询数据集及其版本信息
     * 只返回未删除的数据集和版本
     * @return List<DatasetVersionVO> 包含 dataset_id, dataset_name, version_name 的数据集版本列表
     */
    @Select("SELECT " +
            "dd.id AS datasetId, " +
            "dd.name AS name, " +
            "dd.create_user_id AS createUserId, " +
            "dd.is_public AS isPublic, " +
            "ddv.id AS id, " +
            "ddv.version_name AS versionName " +
            "FROM (SELECT id, name, create_user_id, is_public FROM data_dataset WHERE deleted = 0 AND current_version_name IS NOT NULL) dd " +
            "LEFT JOIN data_dataset_version ddv ON dd.id = ddv.dataset_id AND ddv.deleted = 0 " +
            "ORDER BY dd.id ASC, ddv.version_name ASC")
    List<DatasetVersionVO> selectDatasetWithVersions();

    /**
     * 批量恢复数据集
     * @param ids 数据集ID列表
     */
    void batchRecoverDataset(
            @NotNull(message = "id不能为空")
            @Param("ids") Long[] ids
    );

    /**
     * 获取已删除的所有数据集
     */
    @Select("SELECT * FROM data_dataset WHERE deleted = true")
    List<Dataset> getAllDeletedDataset();

    List<DatasetNameTimeDTO> getDatasetNamesAndTimeList(@Param("datasetIds") List<Long> datasetIds);

    /**
     * 检查数据集列表中是否存在引导式数据集
     *
     * @param datasetIds 数据集ID列表
     * @return 是否存在引导式数据集
     */
    @Select("<script>" +
            "SELECT 1 FROM data_dataset " +
            "WHERE id IN " +
            "<foreach collection='list' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND is_guided = 1 AND deleted = 0 LIMIT 1" +
            "</script>")
    Boolean existsGuidedDataset(@Param("list") List<Long> datasetIds);
}
