package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.SelfIterationTask;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SelfIterationTaskRepo extends PagingAndSortingRepository<SelfIterationTask, Long>,
        JpaSpecificationExecutor<SelfIterationTask> {

    List<SelfIterationTask> findAll();

    List<SelfIterationTask> findByUserId(Long userId);

    List<SelfIterationTask> findByStatus(int status);
}
