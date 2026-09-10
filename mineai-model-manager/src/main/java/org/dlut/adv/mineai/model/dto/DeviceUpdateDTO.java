package org.dlut.adv.mineai.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class DeviceUpdateDTO {
    private Long id; // ID

    private String deviceName; // 设备名称

    private String firmwareVersion; // 固件版本

    private String chipType;
}
