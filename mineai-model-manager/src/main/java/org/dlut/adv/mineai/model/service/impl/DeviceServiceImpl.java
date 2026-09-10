package org.dlut.adv.mineai.model.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.dlut.adv.mineai.core.entity.Device;
import org.dlut.adv.mineai.model.repository.ChipRepository;
import org.dlut.adv.mineai.model.repository.DeviceRepository;
import org.dlut.adv.mineai.model.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class DeviceServiceImpl implements DeviceService {

    @Autowired
    private DeviceRepository deviceRepository; // 注入 repository

    @Autowired
    private ChipRepository chipRepository;

    @Override
    public List<Device> findAll() {
        return deviceRepository.findAll(); // 查询所有设备信息
    }

//    @Override
//    public Optional<Device> findById(Long id) {
//        return deviceRepository.findById(id); // 根据 ID 查询设备
//    }
    @Override
    public Page<Device> findDevices(String deviceName, String firmwareVersion, String chipType, Pageable pageable) {
        return deviceRepository.findByDeviceNameAndFirmwareVersionAndChip(deviceName, firmwareVersion, chipType, pageable);
    }

    @Override
    public Device create(Device device) {
        return deviceRepository.save(device);
        //添加或编辑
    }

    @Override
    public void deleteById(Long id) {
        deviceRepository.deleteById(id);
    }

    @Override
    public Optional<Device> findDeviceById(Long id) {
        return deviceRepository.findById(id);
    }

    @Override
    public List<String> getReleasedApplicationName() {
        List<Device> vos = deviceRepository.getReleasedApplicationName();
        List<String> list = new ArrayList<>();
        for (Device vo : vos) {
            list.add(vo.getDeviceName() + "-" + vo.getFirmwareVersion());
        }
        return list;
    }
}