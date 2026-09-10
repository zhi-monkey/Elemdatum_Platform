package org.dlut.adv.mineai.model.controller;

import org.dlut.adv.mineai.core.annotation.SystemControllerLog;
import org.dlut.adv.mineai.core.constant.AuditLogConstants;
import org.dlut.adv.mineai.core.entity.AuditLogModel;
import org.dlut.adv.mineai.core.entity.Chip;
import org.dlut.adv.mineai.core.entity.Device;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.entity.RequestInfoContext;
import org.dlut.adv.mineai.core.entity.RequestInfoContextHolder;
import org.dlut.adv.mineai.core.entity.UserContext;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.core.service.AuditLogService;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dto.DeviceUpdateDTO;
import org.dlut.adv.mineai.model.service.ChipService;
import org.dlut.adv.mineai.model.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/device")
public class DeviceController {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private ChipService chipService;

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/findAll")
    public Msg<List<Device>> getAllDevices(Pageable pageable) {
        List<Device> deviceList = deviceService.findAll();
        return new Msg<>(MsgCode.SUCCEED, deviceList);
    }

    @PostMapping("/save")
    public Msg<Device> createDevice(@RequestBody DeviceUpdateDTO deviceUpdateDTO) {
        boolean isCreate = deviceUpdateDTO.getId() == null;
        Chip chip = chipService.findByChipType(deviceUpdateDTO.getChipType());
        Device device = new Device();
        device.setChip(chip);
        device.setDeviceName(deviceUpdateDTO.getDeviceName());
        device.setId(deviceUpdateDTO.getId());
        device.setFirmwareVersion(deviceUpdateDTO.getFirmwareVersion());
        Device savedDevice = deviceService.create(device);
        if (isCreate) {
            saveAddAuditLog("device_add",
                    "  deviceName: " + deviceUpdateDTO.getDeviceName()
                            + "  firmwareVersion: " + deviceUpdateDTO.getFirmwareVersion());
        } else {
            saveUpdateAuditLog("device_update",
                    "  deviceId: " + deviceUpdateDTO.getId()
                            + "  deviceName: " + deviceUpdateDTO.getDeviceName()
                            + "  firmwareVersion: " + deviceUpdateDTO.getFirmwareVersion());
        }
        return new Msg<>(MsgCode.SUCCEED, savedDevice);
    }

    @DeleteMapping("/delete/{id}")
    @SystemControllerLog(description = "device_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public Msg<Void> deleteDevice(@PathVariable Long id) {
        try {
            deviceService.deleteById(id);
        } catch (Exception e) {
            return new Msg<>(MsgCode.DEVICE_IS_BINDED);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }

    @GetMapping("findDeviceById")
    public Msg<Optional<Device>> findDeviceById(@RequestParam Long id) {
        Optional<Device> device = deviceService.findDeviceById(id);
        return new Msg<>(MsgCode.SUCCEED, device);
    }

    @GetMapping("/dynamicFindDevices")
    public Msg<Page<Device>> dynamicFindDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String firmwareVersion,
            @RequestParam(required = false) String chipType) {

        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<Device> devicesPage = deviceService.findDevices(deviceName, firmwareVersion, chipType, pageable);
        return new Msg<>(MsgCode.SUCCEED, devicesPage);
    }

    @GetMapping("getReleasedDeviceAndFirmWare")
    public Msg<List<String>> getReleasedDeviceAndFirmWare() {
        List<String> releasedDeviceAndFirmWare = deviceService.getReleasedApplicationName();
        return new Msg<>(MsgCode.SUCCEED, releasedDeviceAndFirmWare);
    }

    private void saveAddAuditLog(String description, String params) {
        saveAuditLog(description, params, AuditLogConstants.OperationType.ADD);
    }

    private void saveUpdateAuditLog(String description, String params) {
        saveAuditLog(description, params, AuditLogConstants.OperationType.UPDATE);
    }

    private void saveAuditLog(String description, String params, int operationType) {
        UserContext userContext = UserContextHolder.getUserContext();
        RequestInfoContext requestInfoContext = RequestInfoContextHolder.getRequestInfoContext();
        if (userContext == null || requestInfoContext == null) {
            return;
        }
        AuditLogModel auditLogModel = new AuditLogModel();
        auditLogModel.setRequestStatus(AuditLogConstants.RequestStatus.NORMAL);
        auditLogModel.setOperationType(operationType);
        auditLogModel.setUid(userContext.getId());
        auditLogModel.setUname(userContext.getUsername());
        auditLogModel.setCreateDate(new java.util.Date());
        auditLogModel.setIp(requestInfoContext.getIpAddress());
        auditLogModel.setRequestUri(requestInfoContext.getRequestUri());
        auditLogModel.setMethod(requestInfoContext.getRequestMethod());
        auditLogModel.setExecutionTime(0L);
        auditLogModel.setDescription(description);
        auditLogModel.setParams(params);
        auditLogService.saveAuditLog(auditLogModel);
    }
}
