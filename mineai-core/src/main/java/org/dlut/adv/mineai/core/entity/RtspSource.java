package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "rtsp_source")
public class RtspSource implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "rtsp_url", nullable = false, length = 1024)
    private String rtspUrl;

    @Column(name = "username", length = 128)
    private String username;

    @JsonIgnore
    @Column(name = "password", length = 1024)
    private String password;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "is_delete", nullable = false)
    private Integer isDelete = 0;
}

/**
 * CREATE TABLE `rtsp_source` (
 *   `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
 *   `name` VARCHAR(128) NOT NULL COMMENT '源名称',
 *   `rtsp_url` VARCHAR(1024) NOT NULL COMMENT 'RTSP地址',
 *   `username` VARCHAR(128) DEFAULT NULL COMMENT 'RTSP认证用户名',
 *   `password` VARCHAR(1024) DEFAULT NULL COMMENT 'RTSP认证密码（AES-GCM密文Base64）',
 *   `description` VARCHAR(512) DEFAULT NULL COMMENT '描述信息',
 *   `is_delete` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
 *   PRIMARY KEY (`id`),
 *   KEY `idx_is_delete` (`is_delete`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RTSP数据源表';
 */
