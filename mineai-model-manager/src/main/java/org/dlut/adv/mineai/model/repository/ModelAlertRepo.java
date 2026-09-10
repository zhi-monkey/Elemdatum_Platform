package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.ModelAlert;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface ModelAlertRepo extends PagingAndSortingRepository<ModelAlert, Long>, JpaSpecificationExecutor<ModelAlert> {

    /**
     * 查询所有分页
     *
     * @param pageable
     * @return
     */
    Page<ModelAlert> findAll(Pageable pageable);

//    @Query("select m from ModelAlert m where m.monitor.name = :monitorName ")
//    Page<ModelAlert> findModelAlertsByMonitorName(Pageable pageable, String monitorName);
//
//    @Query("select m from ModelAlert m where m.model.modelName = :modelName ")
//    Page<ModelAlert> findModelAlertsByModelName(Pageable pageable, String modelName);

    @Query("select m from ModelAlert m where m.monitor.name = :monitorName and m.model.modelName = :modelName ")
    Page<ModelAlert> findModelAlertsByMonitorNameAndModelName(Pageable pageable, String monitorName, String modelName);

    @Query("select m from ModelAlert m where m.monitorName in :monitorNames and m.modelName in :modelNames and m.isDelete = 0 and m.characteristic = 0")
    List<ModelAlert> findSubsystemModelAlertsByMonitorNamesAndModelNames(@Param("monitorNames") List<String> monitorNames, @Param("modelNames") List<String> modelNames);

    @Query("select m from ModelAlert m where m.monitor.monitorName in :monitorNames and m.model.modelName in :modelNames and m.isDelete = 0 and m.characteristic = 0")
    List<ModelAlert> findModelAlertsByMonitorNamesAndModelNames(@Param("monitorNames") List<String> monitorNames, @Param("modelNames") List<String> modelNames);

    //@Query("select m from ModelAlert m where m.monitor.monitorName = :monitorName and m.isDelete = 0 and m.characteristic = 0")
    List<ModelAlert> findModelAlertsByMonitorName(String monitorName);
//    /**
//     * 根据monitorName分页查询报警信息
//     */
//    @Query("select m from ModelAlert m where (m.monitorName = :monitorName or m.monitor.monitorName = :monitorName) and m.isDelete = ModelAlert.UNDELETED")
//    Page<ModelAlert> findModelAlertsByMonitorName(Pageable pageable, @Param("monitorName") String monitorName);
//
//    /**
//     * 根据modelName分页查询报警信息
//     */
//    @Query("select m from ModelAlert m where (m.modelName = :modelName or m.model.modelName = :modelName) and m.isDelete = ModelAlert.UNDELETED")
//    Page<ModelAlert> findModelAlertsByModelName(Pageable pageable, @Param("modelName") String modelName);
//
//    /**
//     * 根据时间分页查询报警信息
//     */
//    Page<ModelAlert> findModelAlertsByCreateTimeBetweenAndIsDelete(String startTime, String endTime, int isDelete, Pageable pageable);
//
//    /**
//     * 根据状态分页查询报警信息
//     */
//    Page<ModelAlert> findModelAlertsByStatusAndIsDelete(int status, int isDelete, Pageable pageable);

    /**
     * 增加保存警报信息
     *
     * @param modelAlert
     * @return
     */
    ModelAlert save(ModelAlert modelAlert);

    /**
     * 根据monitor查询所有相关的报警信息
     */
    @Query(value = "select ma from ModelAlert ma where ma.monitor.id = :monitorId")
    List<ModelAlert> findModelAlertsByMonitor(long monitorId);

    /**
     * 获得所有未删除的警报信息
     */
    @Query(nativeQuery = true, value = "select * from model_alert where is_delete = ?1")
    List<ModelAlert> findModelAlertsByIsDelete(int isDelete);

    /**
     * 获得所有未删除的警报信息的中央平台monitorId
     */
    @Query(nativeQuery = true, value = "select distinct monitor_id from model_alert where is_delete = ?1 and monitor_id IS NOT NULL and characteristic = ?2")
    List<Long> findDistinctMonitorIdWithModelAlert(int isDelete, int characteristic);

    /**
     * 获得所有未删除的警报信息的子系统monitorname
     */
    @Query(nativeQuery = true, value = "select distinct monitor_name from model_alert where is_delete = ?1 and monitor_name IS NOT NULL and characteristic = ?2")
    List<String> findDistinctMonitorNameWithModelAlert(int isDelete, int characteristic);

    /**
     * 获得所有未删除的警报信息的中央平台modelid
     */
    @Query(nativeQuery = true, value = "select distinct model_id from model_alert where is_delete = ?1 and model_id IS NOT NULL and characteristic = ?2")
    List<Long> findDistinctModelIdWithModelAlert(int isDelete, int characteristic);

    /**
     * 获得所有未删除的警报信息的子系统modelname
     */
    @Query(nativeQuery = true, value = "select distinct model_name from model_alert where is_delete = ?1 and model_name IS NOT NULL and characteristic = ?2")
    List<String> findDistinctModelNameWithModelAlert(int isDelete, int characteristic);

    /**
     * 获得所有/未删除的非正常/正常警报信息
     */
    @Query(value = "select ma from ModelAlert ma where ma.isDelete = :isDelete and ma.characteristic = :characteristic")
    List<ModelAlert> findAllModelAlerts(int isDelete, int characteristic);

    /**
     * @param date
     * @return
     */

    @Query(value = "select count(ma) from ModelAlert ma where  " +
            "DATE (:date)=DATE (ma.createTime) and ma.isDelete = 0 and ma.characteristic = 0")
    int countBarAlertByDate(Date date);

    /**
     * 查询最近num条数据
     *
     * @param isDelete 是否删除
     * @param num      查询条数
     * @return List<Monitor> 监控设备列表
     */
    @Query(nativeQuery = true, value = "select * from model_alert where is_delete= ?1 order by id desc limit ?2")
    List<ModelAlert> findModelAlertListByDataNumAndDesc(int isDelete, int num);

    @Query(value = "select count(m) as num from ModelAlert m where m.image = true and m.characteristic = 0")
    int findModelAlertNumsByImage();

    @Query(value = "select count(m) as num from ModelAlert m where m.audio = true and m.characteristic = 0")
    int findModelAlertNumsByAudio();

    @Query(value = "select count(m) as num from ModelAlert m where m.video = true and m.characteristic = 0")
    int findModelAlertNumsByVideo();

    @Query(value = "select count(m) as num from ModelAlert m where m.characteristic = 0")
    int findModelAlertNums();

    ModelAlert findModelAlertById(Long id);

    /*
          查询子系统报警信息
         */
    @Query(value = "select m from ModelAlert m where m.subsystem <> :subsystem and m.isDelete = :isDelete")
    List<ModelAlert> findModelAlertsBySubsystemAndIsDelete(String subsystem, int isDelete);

    /**
     * 查询近dateNum天的数据
     * @param isDelete 是否删除
     * @param dateNum  天数
     * @return 报警信息列表
     */
    @Query(nativeQuery = true, value = "select * from model_alert where is_delete= ?1 and create_time >= " +
            "DATE_SUB(NOW(), INTERVAL ?2 DAY) ORDER BY create_time DESC")
    List<ModelAlert> findModelAlertListByDateNumAndDesc(int isDelete, int dateNum);

    /**
     * 根据算法名称 和 模糊子系统名 查找最新数据
     * 返回当前摄像头对应分组算法的最新值
     *
     * @param algName
     * @param subPreName
     * @return
     */
    @Query(value = "select * " +
            "from model_alert as ma1 " +
            "where (ma1.monitor_name, ma1.create_time) in ( " +
            "   select ma.monitor_name as 'moniter_name', max(ma.create_time) as 'time' " +
            "   from model_alert as ma  " +
            "   where ma.id in ( " +
            "       select model_alert_id " +
            "       from model_alert_data as mad" +
            "       where mad.data = ?1 " +
            "   ) and ma.subsystem like %?2% " +
            "   GROUP BY ma.monitor_name " +
            ") and ma1.is_delete = 0;", nativeQuery = true)
    List<ModelAlert> findModelAlertByAlgorithmNameAndSubSystemPrefixName(String algName, String subPreName);

    /**
     * 针对 小时煤流数据量 返回近 7 个小时的数据
     *
     * @return
     */
    @Query(value = "select * " +
            "from model_alert as ma " +
            "where ma.id in ( " +
            "   select mad.model_alert_id as data_id " +
            "   from model_alert_data as mad " +
            "   where mad.data = ?1 " +
            ") and ma.is_delete = 0 " +
            "and ma.create_time >= CURTIME() - INTERVAL 7 HOUR " +
            "and ma.create_time <= CURTIME();", nativeQuery = true)
    List<ModelAlert> getHourlyCoalFlowData(String commadName);
}
