package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.Controller;
import org.dlut.adv.mineai.core.entity.ModelConfig;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelJobRepo extends PagingAndSortingRepository<ModelJob, Long>, JpaSpecificationExecutor<ModelJob> {

    List<ModelJob> findAll();

    /**
     * 根据条件查询所有ModelJob
     * 条件: modelGenerationId, jobType = TRAIN(1), status >= 2
     */
    @Query("SELECT mj FROM ModelJob mj " +
            "WHERE mj.modelGenerationId = :modelGenerationId " +
            "AND mj.jobType = 1 " +
            "AND mj.status >= 2")
    List<ModelJob> findTrainingJobsByGenerationAndStatus(@Param("modelGenerationId") Long modelGenerationId);

    // 未删除作业列表：同时要求 job 和 generation 未删除
    @Query(value = "SELECT mj.* " +
            "FROM model_job mj " +
            "JOIN model_generation mg ON mj.model_generation_id = mg.id " +
            "WHERE (mg.is_delete = 0 OR mg.is_delete IS NULL) " +
            "AND (mj.is_delete = 0 OR mj.is_delete IS NULL)",
            nativeQuery = true)
    List<ModelJob> findAllJobWithNoDeleted();

    // 未删除作业数量
    @Query(value = "SELECT COUNT(*) " +
            "FROM model_job mj " +
            "JOIN model_generation mg ON mj.model_generation_id = mg.id " +
            "WHERE (mg.is_delete = 0 OR mg.is_delete IS NULL) ",
            nativeQuery = true)
    long countJobWithNoDeleted();

    // 在你的ModelJobRepository中
    @Query("SELECT mj FROM ModelJob mj LEFT JOIN FETCH mj.params WHERE mj.id = :jobId")
    ModelJob findModelJobByIdWithParams(Long jobId);

    /**
     * 根据id查找作业
     *
     * @param id
     */
    ModelJob findModelJobById(long id);


    /**
     * 根据模型版本查找作业
     */

    ModelJob findModelJobByModelVersion(ModelVersion modelVersion);

    Page<ModelJob> findAll(Pageable pageable);

    ModelJob findModelJobByName(String name);

    @Query("SELECT DISTINCT mj FROM ModelJob mj LEFT JOIN FETCH mj.trainDatasetVersions WHERE mj.name = :name")
    ModelJob findModelJobByNameWithTrainDatasetVersions(@Param("name") String name);

    @Query("SELECT DISTINCT mj FROM ModelJob mj LEFT JOIN FETCH mj.selectedLabels WHERE mj.name = :name")
    ModelJob findModelJobByNameWithSelectedLabels(@Param("name") String name);


    int countModelJobsByModelVersionIdAndJobTypeAndStatus(long modelVersionId, int jobType, int status);

    int countModelJobsByModelVersionIdAndJobType(long modelVersionId, int jobType);

    int countModelJobsByModelVersionName(String modelVersionName);


    /**
     * 根据版本查作业
     */
    List<ModelJob> findModelJobsByModelVersionName(String modelVersionName);

    List<ModelJob> findModelJobsByModelVersionIdAndAndStatus(long modelVersionId, int status);

    List<ModelJob> findModelJobsByModelVersionIdAndJobTypeAndStatus(long modelVersionId, int jobType, int status);

    List<ModelJob> findModelJobsByControllerAndStatus(Controller controller, int status);

    /**
     * 获取工作
     *
     * @param isDelete
     * @param status
     * @return
     */

    @Query("select distinct mv from ModelJob mj join mj.modelVersion mv where mv.isDelete = ?1 and mj.status = ?2")
    List<ModelVersion> existsByModelVersionStatusAndJobStatus(int isDelete, int status);

    @Query("SELECT me.description FROM ModelJob mj " +
            "JOIN ModelGeneration mg ON mj.modelGenerationId = mg.id " +
            "JOIN mg.modelExplore me " +
            "WHERE mg.id = ?1")
    List<String> findModelExploreDescriptionByGenerationId(long modelGenerationId);


    // 获取某个 ModelGeneration 的所有 ModelJob
    List<ModelJob> findByModelGenerationId(Long modelGenerationId);

    Page<ModelJob> findByModelGenerationId(Long modelGenerationId, Pageable pageable);

    List<ModelJob> findModelJobsByStatus(int status);

    @Query("SELECT mv.modelConfigList FROM ModelJob mj " +
            "JOIN ModelGeneration mg ON mj.modelGenerationId = mg.id " +
            "JOIN mg.modelExplore me " +
            "JOIN me.convertModelVersion mv " +
            "WHERE mj.id = ?1")
    List<ModelConfig> findConvertModelConfigListByJobId(long jobId);

    @Query(value = "SELECT mjp.model_job_id AS modelJobId, " +
            "mjp.params_key AS paramsKey, " +
            "mjp.params AS params " +
            "FROM model_job mj " +
            "JOIN model_job_params mjp ON mjp.model_job_id = mj.id " +
            "WHERE mj.id = ?1", nativeQuery = true)
    List<Object[]> findModelJobParamsByJobId(Long jobId);

    /**
     * 模糊 + 倒序 + limit 找到 包含 keyword 的最新的转换记录
     *
     * @param keyword
     * @return
     */
    @Query(value = "SELECT id " +
            "FROM model_job as mj " +
            "WHERE name LIKE CONCAT('%', :keyword, '%') " +
            "ORDER BY name " +
            "DESC LIMIT 1", nativeQuery = true)
    Long findLatestJobWithLikeName(String keyword);

    /**
     * 找到prefix开头，name包含substring的job
     *
     * @param prefix
     * @param substring
     * @return
     */
    @Query("SELECT mj FROM ModelJob mj WHERE mj.name LIKE ?1 AND mj.name LIKE ?2")
    List<ModelJob> findByNameStartingWithAndNameContaining(String prefix, String substring);

    @Query(value = "SELECT mj.*, mg.user_id,mg.is_delete, u.username " +
            "FROM model_job mj " +  // 假设表名为 model_job
            "JOIN model_generation mg ON mj.model_generation_id = mg.id AND mg.is_delete = 0 " + // 只选择未删除的 model_generation
            "JOIN user u ON mg.user_id = u.id", nativeQuery = true)
    List<Object[]> findModelJobsWithUsersRaw();

    /**
     * 根据状态查询训练任务及其关联的Generation的userId
     * 返回Object[]数组：[ModelJob, Long userId]
     */
    @Query("SELECT j, g.userId FROM ModelJob j " +
           "JOIN ModelGeneration g ON j.modelGenerationId = g.id " +
           "WHERE j.status = :status AND j.jobType = :jobType")
    List<Object[]> findTrainingJobsWithUserIdByStatus(@Param("status") Integer status,
                                                       @Param("jobType") Integer jobType);

    long countModelJobsByStatusAndJobType(Integer status, Integer jobType);
}
