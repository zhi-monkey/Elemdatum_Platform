package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.ModelApplicationZipLabel;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author mingming
 */
@Repository
public interface ModelApplicationZipLabelRepo extends PagingAndSortingRepository<ModelApplicationZipLabel, Long>, JpaSpecificationExecutor<ModelApplicationZipLabel> {
    @Query("select l.labelId from ModelApplicationZipLabel l where l.modelApplicationId = ?1")
    List<Long> getBoundLabelIds(Long modelApplicationId);

    @Modifying
    @Transactional
    @Query(value = "insert into model_application_zip_label (model_application_id, label_id) values (:modelApplicationId, :labelId)", nativeQuery = true)
    void saveBoundLabelIds(Long modelApplicationId, Long labelId);

    @Modifying
    @Transactional
    @Query(value = "delete from model_application_zip_label where model_application_id = :modelApplicationId", nativeQuery = true)
    void removeBoundLabelByModelApplicationId(Long modelApplicationId);
}
