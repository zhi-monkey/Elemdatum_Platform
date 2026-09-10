package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.ModelExplore;
import org.dlut.adv.mineai.core.entity.ModelGeneration;
import org.dlut.adv.mineai.model.jpaProjection.ModelGenerationProjection;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ModelGenerationRepo extends PagingAndSortingRepository<ModelGeneration, Long>, JpaSpecificationExecutor<ModelGeneration> {

    ModelGeneration findModelGenerationByName(String name);

    @Query("SELECT m FROM ModelGeneration m WHERE m.id = ?1")
    ModelGeneration getModelGenerationById(Long id);

    //List<ModelGeneration> findModelGenerationsByModelExploreAndStatus(ModelExplore model, int publishing);

    List<ModelGeneration> findModelGenerationsByModelExplore(ModelExplore model);

    /**
     * 查询可用于引导式自动标注的生产任务（基础条件筛选）
     *
     * 条件说明（数据库层过滤）：
     * 1. 未删除: mg.isDelete = false
     * 2. 是引导式: mg.isGuided = true
     * 3. 可复用: mg.isReuse = true
     * 4. 支持自动标注: tmv.autoLabel = true
     * 5. 标注类型匹配: mg.annotationType = :annotationType
     *
     * 注意：状态条件（4）需要在应用层判断
     *
     * @param annotationType 标注类型
     * @return 符合基础条件的生产任务列表
     */
    @Query("SELECT mg FROM ModelGeneration mg " +
            "JOIN FETCH mg.modelExplore me " +
            "JOIN FETCH me.trainModelVersion tmv " +
            "WHERE mg.isDelete = false " +
            "AND mg.isGuided = true " +
            "AND mg.isReuse = true " +
            "AND tmv.isAutoLabel = true " +
            "AND mg.annotationType = :annotationType")
    List<ModelGeneration> findGuidedGenerationsForAutoLabel(
            @Param("annotationType") String annotationType
    );

    /**
     * 查询所有的生产任务
     *
     * @return
     */
    @Query("select mg from ModelGeneration as mg")
    List<ModelGeneration> findAll();

    @Query("SELECT m FROM ModelGeneration m WHERE m.name LIKE ?1 AND m.isDelete = ?2")
    List<ModelGeneration> findModelGenerationListByNameLike(String name, boolean isdelete);

    @Query("SELECT m.name FROM ModelGeneration m WHERE m.name LIKE CONCAT(:prefix, '%')")
    List<String> findAllNamesStartingWith(String prefix);

    // 查询所有 isDelete 为 0 的 ModelGeneration 并返回其关联的 Model 的 ID
    @Query("SELECT mg.model.id FROM ModelGeneration mg WHERE mg.isDelete = false ")
    List<Long> findAllBindModelIds();

    @Query("SELECT COUNT(1) FROM ModelGeneration mg WHERE mg.modelExplore.id = :modelExploreId AND mg.isDelete = false ")
    Integer countByModelExploreId(Long modelExploreId);

    @Query(value = "SELECT " +
            "mg.id AS id, " +
            "mg.name AS name, " +
            "dd.id AS datasetId, " +
            "dd.name AS datasetName, " +
            "mg.is_guided AS isGuided, " + // 固定为引导式
            "CAST(null AS SIGNED) AS datasetVersionId " + // 无版本ID
            "FROM model_generation mg " +
            "INNER JOIN data_dataset dd ON mg.train_dataset = dd.id " +
            "WHERE " +
            "   mg.is_guided = true " +
            "   AND dd.id IN (:datasetIds) " +
            "   AND mg.is_delete = 0 " +
            "   AND dd.deleted = 0", nativeQuery = true)
    List<ModelGenerationProjection> findGuidedDatasetBinding(@Param("datasetIds") List<Long> datasetIds);

    @Query(value = "SELECT " +
            "mg.id AS id, " +
            "mg.name AS name, " +
            "dd.id AS datasetId, " +
            "dd.name AS datasetName, " +
            "mg.is_guided AS isGuided, " +
            "mg.train_dataset AS datasetVersionId " +
            "FROM model_generation mg " +
            "INNER JOIN data_dataset_version ddv ON mg.train_dataset = ddv.id " +
            "INNER JOIN data_dataset dd ON ddv.dataset_id = dd.id " +
            "WHERE dd.id IN (:datasetIds) " +
            "AND mg.is_delete = 0 " +
            "AND ddv.deleted = 0 " +
            "AND mg.is_guided = false " +
            "AND dd.deleted = 0", nativeQuery = true)
    List<ModelGenerationProjection> findStandardBindingByDatasetIds(@Param("datasetIds")List<Long> datasetIds);

    @Query("SELECT DISTINCT mg FROM ModelGeneration mg JOIN mg.trainDatasetVersions v WHERE v IN :versionIds")
    List<ModelGeneration> findByTrainDatasetVersionsContains(@Param("versionIds") List<Long> versionIds);


    /**
     * 高效地查询支持自动标注的模型生产任务。
     * 该查询将所有过滤条件（包括权限、状态和关联对象的属性）下推到数据库执行。
     *
     * @param userId  当前用户的ID
     * @param isAdmin 当前用户是否为管理员
     * @return 符合条件的模型生产任务列表
     */
    @Query("SELECT mg FROM ModelGeneration mg " +
            "JOIN mg.modelExplore me " +
            "JOIN me.trainModelVersion tmv " +
            "WHERE mg.isDelete = false " +
            "AND mg.isGuided = false " +
            "AND tmv.isAutoLabel = true " +
            "AND (:isAdmin = true OR mg.userId = :userId)")
    List<ModelGeneration> findModelGenerationSupportAutoLabel(
            @Param("userId") Long userId,
            @Param("isAdmin") boolean isAdmin
    );



    /**
     * 查询绑定了指定数据集版本的所有未删除任务
     * @param versionId 数据集版本ID
     * @return 绑定该版本的任务列表
     */
    @Query("SELECT mg FROM ModelGeneration mg " +
            "WHERE mg.isDelete = false " +
            "AND :versionId MEMBER OF mg.trainDatasetVersions")
    List<ModelGeneration> findByTrainDatasetVersionsContainsAndIsDeleteFalse(
            @Param("versionId") Long versionId);

    /**
     * 查询绑定指定应用且未删除的生产任务
     *
     * @param modelApplicationId 应用ID
     * @return 绑定到该应用的生产任务列表
     */
    List<ModelGeneration> findByModelApplicationIdAndIsDeleteFalse(Long modelApplicationId);

    // 未删除作业数量（按 model_generation 统计）
    @Query(value = "SELECT COUNT(*) " +
            "FROM model_generation mg " +   // 这里结尾一定要有空格
            "WHERE (mg.is_delete = 0 OR mg.is_delete IS NULL)",
            nativeQuery = true)
    long countGenerationWithNoDeleted();
}
