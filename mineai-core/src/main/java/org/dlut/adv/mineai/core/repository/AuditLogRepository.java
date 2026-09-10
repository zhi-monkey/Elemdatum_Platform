package org.dlut.adv.mineai.core.repository;

import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends PagingAndSortingRepository<AuditLogModel, Integer>,
        JpaSpecificationExecutor<AuditLogModel> {

}

