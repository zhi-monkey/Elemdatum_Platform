package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.Monitor;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface MonitorRepo extends PagingAndSortingRepository<Monitor, Long>, JpaSpecificationExecutor<Monitor> {

    Monitor findMonitorByName(String monitorName);

    Monitor findMonitorByMonitorName(String monitorName);

    Monitor findMonitorById(Long monitorId);
}
