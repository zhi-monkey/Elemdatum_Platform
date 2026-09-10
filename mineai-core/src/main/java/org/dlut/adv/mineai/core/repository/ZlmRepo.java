package org.dlut.adv.mineai.core.repository;

import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.core.entity.SubsystemMonitor;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 注意不要和功能模块命名冲突
 * 注入失败请使用 @EnableJpaRepositories 添加扫描路径
 *
 * @author dean
 */
@Repository
public interface ZlmRepo extends PagingAndSortingRepository<Monitor, Long>, JpaSpecificationExecutor<Monitor> {

    /**
     * 在数据库中查找直接推流并且在运行的Monitor 走转发的忽略
     *
     * @param isPushStream 是否推流
     * @param statusUsing  是否在使用
     * @return List<Monitor>
     */
    List<Monitor> findMonitorByIsPushStreamAndStatusUsing(int isPushStream, int statusUsing);

    /**
     * 查找所有子系统的列表
     * @return List<SubsystemMonitor>
     */
    @Query(value = "select s from SubsystemMonitor s")
    List<SubsystemMonitor> getSubsystemMonitors();

    /**
     * 在数据库中查找绑定了数据集并且在运行的Monitor 无论走转发还是推流
     *
     * @param statusUsing 是否在使用
     * @param isRecord 是否在录制
     * @return List<Monitor>
     */
    List<Monitor> findMonitorByStatusUsingAndDatasetIdIsNotNullAndIsRecord(int statusUsing, int isRecord);

    Monitor findMonitorById(long id);
}