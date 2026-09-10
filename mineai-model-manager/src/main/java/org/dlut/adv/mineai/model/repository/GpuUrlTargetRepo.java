package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.GpuUrlTarget;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GpuUrlTargetRepo extends PagingAndSortingRepository<GpuUrlTarget, Long>, JpaSpecificationExecutor<GpuUrlTarget> {

    @Query("SELECT g FROM GpuUrlTarget g WHERE g.isDelete = 0 " +
            "AND (COALESCE(:keyword, '') = '' OR g.name LIKE %:keyword% OR g.ip LIKE %:keyword% OR g.platformIp LIKE %:keyword%) " +
            "ORDER BY g.id DESC")
    List<GpuUrlTarget> searchAvailable(@Param("keyword") String keyword);

    List<GpuUrlTarget> findByIdInAndIsDelete(List<Long> ids, Integer isDelete);
}
