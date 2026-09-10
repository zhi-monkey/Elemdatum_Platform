package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.core.entity.MonitorModelConfig;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * @author haoxiaoyang
 */
@Repository
public interface MonitorModelConfigRepo extends PagingAndSortingRepository<MonitorModelConfig, Long>, JpaSpecificationExecutor<MonitorModelConfig> {


    @Transactional
    @Modifying
    int deleteMonitorModelConfigByMonitorAndModel(Monitor monitor, Model model);

    @Transactional
    @Modifying
    int deleteMonitorModelConfigByModel(Model model);



    MonitorModelConfig findMonitorModelConfigByModelIdAndMonitorId(Long modelId , Long monitorId);

    List<MonitorModelConfig>  findMonitorModelConfigsByModelId(Long modelId);



}


