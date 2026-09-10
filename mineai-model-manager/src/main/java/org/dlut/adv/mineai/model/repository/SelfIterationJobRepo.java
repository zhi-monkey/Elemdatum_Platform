package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.SelfIterationJob;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface SelfIterationJobRepo extends PagingAndSortingRepository<SelfIterationJob, Long>,
        JpaSpecificationExecutor<SelfIterationJob> {

    List<SelfIterationJob> findBySelfIterationTask_IdOrderByRoundAsc(Long selfIterationTaskId);

    Optional<SelfIterationJob> findBySelfIterationTask_IdAndRound(Long selfIterationTaskId, int round);

    List<SelfIterationJob> findByStatus(int status);

    List<SelfIterationJob> findByStatusIn(List<Integer> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from SelfIterationJob j where j.id = :jobId")
    Optional<SelfIterationJob> findByIdForUpdate(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "INSERT INTO self_iteration_job_dataset_versions (self_iteration_job_id, dataset_version_id) " +
            "SELECT :jobId, :versionId FROM DUAL WHERE NOT EXISTS (" +
            "SELECT 1 FROM self_iteration_job_dataset_versions " +
            "WHERE self_iteration_job_id = :jobId AND dataset_version_id = :versionId)", nativeQuery = true)
    int insertDatasetVersionIdIfAbsent(@Param("jobId") Long jobId, @Param("versionId") Long versionId);

    @Query(value = "SELECT COUNT(DISTINCT dataset_version_id) FROM self_iteration_job_dataset_versions " +
            "WHERE self_iteration_job_id = :jobId", nativeQuery = true)
    int countDistinctDatasetVersionIdsByJobId(@Param("jobId") Long jobId);

    @Modifying
    @Query(value = "INSERT INTO self_iteration_job_annotated_datasets (self_iteration_job_id, dataset_id) " +
            "SELECT :jobId, :datasetId FROM DUAL WHERE NOT EXISTS (" +
            "SELECT 1 FROM self_iteration_job_annotated_datasets " +
            "WHERE self_iteration_job_id = :jobId AND dataset_id = :datasetId)", nativeQuery = true)
    int insertAnnotatedDatasetIdIfAbsent(@Param("jobId") Long jobId, @Param("datasetId") Long datasetId);

    @Query(value = "SELECT COUNT(*) FROM self_iteration_job_annotated_datasets " +
            "WHERE self_iteration_job_id = :jobId", nativeQuery = true)
    int countAnnotatedDatasetsByJobId(@Param("jobId") Long jobId);

    @Query(value = "SELECT dataset_id FROM self_iteration_job_annotated_datasets " +
            "WHERE self_iteration_job_id = :jobId", nativeQuery = true)
    List<Long> findAnnotatedDatasetIdsByJobId(@Param("jobId") Long jobId);
}
