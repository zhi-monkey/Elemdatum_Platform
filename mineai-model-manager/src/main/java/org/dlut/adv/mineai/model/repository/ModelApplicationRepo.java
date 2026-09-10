package org.dlut.adv.mineai.model.repository;

import org.apache.ibatis.annotations.Param;
import org.dlut.adv.mineai.core.entity.ModelApplication;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ModelApplicationRepo extends PagingAndSortingRepository<ModelApplication, Long>, JpaSpecificationExecutor<ModelApplication> {

    // 查询所有应用数据
    @Query("SELECT m FROM ModelApplication m JOIN FETCH m.applicationName")
    List<ModelApplication> findAll();

    /**
     * 删除模型应用绑定的场景, 这个写法可能有些问题，有更好的写法可以帮我改改
     *
     * @param id 模型应用id
     */
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM model_application_scene WHERE model_application_id = :id", nativeQuery = true)
    void removeBoundScene(Long id);

    // 判断场景是否被应用任务绑定
    @Query(value = "SELECT COUNT(*) FROM model_application_scene WHERE scene_id = :id", nativeQuery = true)
    Integer countBySceneId(@Param("id") Long id);


    @Transactional
    @Modifying
    @Query(value = "UPDATE model_application SET is_released = '已发布', update_time = now() WHERE id = :id", nativeQuery = true)
    void bindReleasedById(Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE model_application SET is_released = '未发布', update_time = now()  WHERE id = :id", nativeQuery = true)
    void unbindReleasedById(Long id);

    /**
     * 更新model_application表，把模型应用绑定的设备id置为null
     *
     * @param id 模型应用id
     */
    @Transactional
    @Modifying
    @Query(value = "UPDATE model_application SET device_id = NULL WHERE id = :id", nativeQuery = true)
    void removeBoundDevice(Long id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE model_application SET model_id = NULL WHERE id = :id", nativeQuery = true)
    void removeBoundModel(Long id);

    @Query(value = "SELECT DISTINCT a.application_name " +
            "FROM model_application m " +
            "JOIN application_name a ON m.application_name_id = a.id " +
            "WHERE m.is_released = '已发布' " +
            "AND m.device_id IS NOT NULL AND m.is_delete = 0 AND a.is_delete = 0", nativeQuery = true)
    List<String> getReleasedApplicationName();

    //    @Select("SELECT * FROM model_application WHERE application_name_id = #{applicationNameId} AND device_id = #{deviceId}")
    @Query("SELECT m FROM ModelApplication m WHERE m.applicationName.id = :applicationNameId AND m.device.id = :deviceId AND m.isReleased = '已发布'AND m.isDelete = 0")
    ModelApplication getUniqueModelApplication(Long applicationNameId, Long deviceId);

    @Query("SELECT m FROM ModelApplication m WHERE m.applicationName.id = :applicationNameId AND m.isReleased = '已发布' AND m.isDelete = 0 AND m.device IS NOT NULL")
    List<ModelApplication> findModelApplicationsByapplicationNameId(Long applicationNameId);

    @Query("SELECT m FROM ModelApplication m WHERE m.device.id = :deviceId AND m.isReleased = '已发布' AND m.isDelete = 0")
    List<ModelApplication> findModelApplicationsByDeviceId(Long deviceId);

    @Query("SELECT mg.splitSize FROM ModelApplication ma " +
            "JOIN Model m ON m.id = ma.model.id " +
            "JOIN ModelGeneration mg ON mg.id = m.generationId " +
            "WHERE ma.applicationName.id = :applicationNameId AND ma.device.id = :deviceId AND ma.isReleased = '已发布' AND ma.isDelete = 0 AND ma.device IS NOT NULL")
    String getSplitSizeByApplicationNameAndDevice(Long applicationNameId, Long deviceId);

    @Query("SELECT ma FROM ModelApplication ma WHERE ma.applicationName.id = :applicationNameId AND ma.device.id = :deviceId AND ma.isDelete = 0 AND ma.isReleased = '已发布'")
    ModelApplication findModelApplicationByApplicationNameIdAndDeviceId(Long applicationNameId, Long deviceId);

    @Transactional
    @Modifying
    @Query("UPDATE ModelApplication SET isDelete = 1 WHERE id = :id")
    void softDelete(Long id);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END " +
            "FROM ModelApplication m " +
            "WHERE m.applicationName.id = :applicationNameId AND m.isDelete = 0")
    boolean existsByApplicationNameId(@Param("applicationNameId") Long applicationNameId);

    @Query("SELECT COUNT(DISTINCT ma.applicationName.id) " +
            "FROM ModelApplication ma " +
            "WHERE ma.isDelete = 0 AND ma.isReleased = ?1 AND ma.applicationName IS NOT NULL")
    long countDistinctApplicationNameIdsByReleaseStatus(String releaseStatus);

    @Query("SELECT s.id, an.applicationName, c.chipType " +
            "FROM ModelApplication ma " +
            "JOIN ma.applicationName an " +
            "JOIN ma.applicableScene s " +
            "LEFT JOIN ma.device d " +
            "LEFT JOIN d.chip c " +
            "WHERE ma.isDelete = 0 AND s.isDelete = 0 AND ma.isReleased = '已发布'")
    List<Object[]> findSceneApplicationTaskAndChip();

    /**
     * 根据应用名称和设备固件查找 ModelApplication
     * @param applicationName 应用名称
     * @param deviceFirmware 设备固件（格式：设备名-固件版本）
     * @return ModelApplication
     */
    @Query("SELECT ma FROM ModelApplication ma " +
            "JOIN ma.applicationName an " +
            "JOIN ma.device d " +
            "WHERE an.applicationName = :applicationName " +
            "AND CONCAT(d.deviceName, '-', d.firmwareVersion) = :deviceFirmware " +
            "AND ma.isReleased = '已发布' " +
            "AND ma.isDelete = 0")
    ModelApplication findByApplicationNameAndDeviceAndFirmware(
            @Param("applicationName") String applicationName,
            @Param("deviceFirmware") String deviceFirmware);
}
