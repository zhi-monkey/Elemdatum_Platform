package org.dlut.adv.mineai.model.service;

import org.dlut.adv.mineai.core.entity.Chip;import org.dlut.adv.mineai.core.entity.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface DeviceService {
    List<Device> findAll(); // 获取所有设备信息

//    Optional<Device> findById(Long id); // 根据 ID 查找设备信息

//    List<Chip> create(Device device, String chipType); // 保存或更新设备信息

    Device create(Device device);

    void deleteById(Long id); // 根据 ID 删除设备信息
    Page<Device> findDevices(String deviceName, String firmwareVersion, String chipType, Pageable pageable);

    Optional<Device> findDeviceById(Long id);

    List<String> getReleasedApplicationName();
}