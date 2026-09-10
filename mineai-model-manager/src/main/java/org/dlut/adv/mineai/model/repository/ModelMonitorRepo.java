package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author haoxiaoyang
 */
@Repository
public interface ModelMonitorRepo extends PagingAndSortingRepository<Monitor, Long>, JpaSpecificationExecutor<Monitor> {


    Monitor findMonitorById(long id);
}
