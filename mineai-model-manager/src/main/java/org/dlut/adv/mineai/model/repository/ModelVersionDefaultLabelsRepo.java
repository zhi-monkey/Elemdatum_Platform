package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.model.domain.entity.ModelVersionDefaultLabels;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author mingming
 * @date 2025/06/11
 */
public interface ModelVersionDefaultLabelsRepo extends PagingAndSortingRepository<ModelVersionDefaultLabels, Long>, JpaSpecificationExecutor< ModelVersionDefaultLabels> {

    @Query("SELECT m FROM ModelVersionDefaultLabels m WHERE m.modelVersionId = ?1")
    List<ModelVersionDefaultLabels> findRelationsByModelVersionId(Long id);

    @Query("SELECT m FROM ModelVersionDefaultLabels m WHERE m.modelVersionId IN ?1")
    List<ModelVersionDefaultLabels> findRelationsByModelVersionIds(List<Long> ids);
}
