package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import feign.Param;

import java.util.List;

@Repository
public interface DeviceRepository extends PagingAndSortingRepository<Device, Long>, JpaSpecificationExecutor<Device> {
    @Query("SELECT d FROM Device d")
    List<Device> findAll(); // 查询所有设备信息

    @Query("SELECT d FROM Device d WHERE " +
            "(COALESCE(:deviceName, '') = '' OR d.deviceName LIKE %:deviceName%) AND " +
            "(COALESCE(:firmwareVersion, '') = '' OR d.firmwareVersion LIKE %:firmwareVersion%) AND " +
            "(COALESCE(:chipType, '') = '' OR d.chip.chipType LIKE %:chipType%)")
    Page<Device> findByDeviceNameAndFirmwareVersionAndChip(
            @Param("deviceName") String deviceName,
            @Param("firmwareVersion") String firmwareVersion,
            @Param("chipId") String chipType,
            Pageable pageable);

    @Query(value = "SELECT DISTINCT d.* " +
            "FROM model_application m " +
            "JOIN device d ON m.device_id = d.id " +
            "WHERE m.is_released = '已发布'" +
            "AND m.device_id IS NOT NULL", nativeQuery = true)
    List<Device> getReleasedApplicationName();

//    @Select("SELECT * FROM device WHERE device_name = #{deviceName}")
    @Query("SELECT d FROM Device d WHERE d.deviceName = :deviceName")
    Device findDeviceByDeviceName(String deviceName);

    @Query("SELECT d FROM Device d WHERE d.deviceName = :deviceName AND d.firmwareVersion = :firmwareVersion")
    Device findDeviceByDeviceNameAndFirmwareVersion(String deviceName, String firmwareVersion);
}