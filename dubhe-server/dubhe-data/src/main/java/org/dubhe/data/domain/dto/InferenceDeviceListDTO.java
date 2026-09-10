package org.dubhe.data.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 算力设备列表 DTO
 */
@Data
public class InferenceDeviceListDTO implements Serializable {

    private Long id;

    private String deviceName;

    private String inferenceIp;

    private Integer inferencePort;

    private String remark;

    private String createdBy;

    private LocalDateTime createdAt;

    /**
     * 设备状态（当前留空，后续由设备状态接口补充）
     */
    private String deviceStatus;

    /**
     * 加载算法应用（当前留空，后续由算法接口补充）
     */
    private String loadedAlgorithmApp;
}
