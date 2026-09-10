package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 算力推理设备实体
 */
@Data
@TableName("inference_device")
public class InferenceDevice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备名称 */
    private String deviceName;

    /** 推理设备IP */
    private String inferenceIp;

    /** 端口号 */
    private Integer inferencePort;

    /** 授权码 */
    @JsonIgnore
    private String authCode;

    /** 备注信息 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}

/**
 * 建表SQL归档：
 * CREATE TABLE `inference_device`
 * (
 *     `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
 *     `device_name`    VARCHAR(64)     NOT NULL COMMENT '设备名称',
 *     `inference_ip`   VARCHAR(45)     NOT NULL COMMENT '推理设备IP(支持IPv4/IPv6)',
 *     `inference_port` INT UNSIGNED    NOT NULL COMMENT '端口号',
 *     `auth_code`      VARCHAR(256)    NOT NULL COMMENT '授权码(建议加密存储)',
 *     `remark`         VARCHAR(500)             DEFAULT NULL COMMENT '备注信息',
 *     `created_by`     VARCHAR(64)     NOT NULL COMMENT '创建人',
 *     `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 *     `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 *
 *     PRIMARY KEY (`id`),
 *     UNIQUE KEY `uk_device_name` (`device_name`),
 *     UNIQUE KEY `uk_ip_port` (`inference_ip`, `inference_port`),
 *     CONSTRAINT `chk_port_range` CHECK (`inference_port` BETWEEN 1 AND 65535)
 * ) ENGINE = InnoDB
 *   DEFAULT CHARSET = utf8mb4 COMMENT ='算力推理设备表';
 */
